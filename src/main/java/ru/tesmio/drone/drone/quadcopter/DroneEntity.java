package ru.tesmio.drone.drone.quadcopter;

import net.minecraft.client.Minecraft;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.ContainerEntity;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.network.PacketDistributor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import ru.tesmio.drone.Dronecraft;


import ru.tesmio.drone.drone.quadcopter.container.DroneEntityMenu;
import ru.tesmio.drone.drone.quadcopter.container.UpgradeContainer;
import ru.tesmio.drone.packets.PacketSystem;
import ru.tesmio.drone.packets.client.*;
import ru.tesmio.drone.packets.server.DroneModesC2SP;
import ru.tesmio.drone.registry.InitItems;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static ru.tesmio.drone.drone.quadcopter.control.DroneController.CTRL_KEY;


//TODO: отвязка от пульта должна быть при покидании зоны, но не нужно постоянно держать getController.
// предмет должен помнить текущий НБТ и при взаимодействии предметом надо отправлять пакет, который
// будет проверять, можно ли соединится с этим дроном. И если можно, то он соединяется и опять
// записывает в controllerUUID дрона информацию о том, кто управляет.
//
// TODO: Остановился на процессе решения вопроса с переключением режимов - не переключаются вложенные режимы - почему то.
public class DroneEntity extends Mob implements ContainerEntity {
    public static final EntityDataAccessor<Float> DATA_VIEW_DISTANCE = SynchedEntityData.defineId(DroneEntity.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Float> DATA_SIM_DISTANCE = SynchedEntityData.defineId(DroneEntity.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<String> DATA_FLIGHT_MODE = SynchedEntityData.defineId(DroneEntity.class, EntityDataSerializers.STRING);
    public static final EntityDataAccessor<String> DATA_STAB_MODE = SynchedEntityData.defineId(DroneEntity.class, EntityDataSerializers.STRING);
    public static final EntityDataAccessor<String> DATA_ZOOM_MODE = SynchedEntityData.defineId(DroneEntity.class, EntityDataSerializers.STRING);
    public static final EntityDataAccessor<String> DATA_VISION_MODE = SynchedEntityData.defineId(DroneEntity.class, EntityDataSerializers.STRING);


    public UUID CONTROLLER_UUID;
    public static final Logger LOGGER = LogManager.getLogger(Dronecraft.MODID);
    private final UpgradeContainer inventory = new UpgradeContainer();
    private final NonNullList<ItemStack> items;
    private Vec3 velocity = Vec3.ZERO;
    private float droneRoll, droneYaw, dronePitch;
    public float prevRoll, prevYaw, prevPitch;
    public float prevTiltX = 0f;
    public float prevTiltZ = 0f;
    public double currentSpeed = 0.0;
    public final double acceleration = 0.01;
    private float targetTiltX;
    private float targetTiltZ;

    public float bodyXRot, bodyZRot;
    public float prevBodyXRot, prevBodyZRot;
    public float rotorAngle = 0f;
    public float angularVelocity = 720f;
    public final float AIR_FRICTION = 0.98f;
    public final float STAB_FRICTION  = 0.90f;



    public DroneEntity(EntityType<? extends Mob> type, Level world) {
        super(type, world);

        this.setNoGravity(false);
        this.setHealth(20.0f);
        this.items = NonNullList.withSize(10, ItemStack.EMPTY);
    }
    public void syncDie() {
        if (!level().isClientSide && getControllerUUID() != null) {
            Player player = level().getPlayerByUUID(getControllerUUID());
            if (player instanceof ServerPlayer sp) {
                PacketSystem.CHANNEL.send(PacketDistributor.PLAYER.with(() -> sp),
                        new DroneDeathPacket(false, getControllerUUID()));
            }
        }
    }
    @Override
    public boolean isAlwaysTicking() {return true;}
    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {return false;}
    @Override
    public void die(DamageSource cause) {
        if (!this.isRemoved()) {
            if (this.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.SMOKE,
                        this.getX(), this.getY(), this.getZ(),
                        10, 0.2, 0.2, 0.2, 0.1);
            }
            syncDie();
                        this.discard();
        }
    }


    @Override
    public InteractionResult interactAt(Player player, Vec3 vec, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level().isClientSide && player.isShiftKeyDown() && stack.isEmpty()) {
            if (player instanceof ServerPlayer serverPlayer) {
                NetworkHooks.openScreen(serverPlayer, this, buf -> buf.writeVarInt(this.getId()));
            }
            return InteractionResult.CONSUME;
        }

        if (hand != InteractionHand.MAIN_HAND || level().isClientSide) {
            return super.interactAt(player, vec, hand);
        }



        if (player.isShiftKeyDown() && stack.isEmpty()) {
            discardDrone();
            return InteractionResult.SUCCESS;
        }

        if (stack.getItem() instanceof RemoteItem) {
            return handleRemoteConnection(player, stack)
                    ? InteractionResult.SUCCESS
                    : InteractionResult.FAIL;
        }

        return super.interactAt(player, vec, hand);
    }

    private void discardDrone() {
        ItemStack droneItem = new ItemStack(InitItems.ITEM_DRONE.get());
        level().addFreshEntity(new ItemEntity(level(), getX(), getY(), getZ(), droneItem));
        discard();
    }

    private boolean handleRemoteConnection(Player player, ItemStack remote) {

        if (player.isPassenger()) {
            return false;
        }

        CompoundTag tag = remote.getOrCreateTag();
        if (tag.contains("DroneUUID")) {
            return false;
        }
        tag.putUUID("DroneUUID", getUUID());
        this.setControllerUUID(player.getUUID());
        if (player instanceof ServerPlayer serverPlayer) {
            sendConnectionPackets(serverPlayer);
        }

        return true;
    }

    private void sendConnectionPackets(ServerPlayer player) {
        int simDistance = player.server.getPlayerList().getSimulationDistance();
        int viewDistance = player.server.getPlayerList().getViewDistance();

        PacketSystem.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new DistanceControlPacket(simDistance, viewDistance, getId()));
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_FLIGHT_MODE, FlightMode.NORMAL.name());
        this.entityData.define(DATA_STAB_MODE, StabMode.FPV.name());
        this.entityData.define(DATA_ZOOM_MODE, ZoomMode.DEF.name());
        this.entityData.define(DATA_VISION_MODE, VisionMode.NORMAL.name());
        this.entityData.define(DATA_SIM_DISTANCE, 0f);
        this.entityData.define(DATA_VIEW_DISTANCE, 0f);
    }

    @Override
    public float getViewXRot(float partialTick) {
        float interpolatedYaw = Mth.lerp(partialTick, prevPitch, this.dronePitch);
        return interpolatedYaw;
    }

    @Override
    public float getViewYRot(float partialTick) {
        float interpolatedYaw = Mth.lerp(partialTick, prevYaw, this.droneYaw);
        return interpolatedYaw;
    }

    @Override
    public float getYRot() {
        return this.droneYaw;
    }

    /**DAMAGE*/
    @Override
    public boolean hurt(DamageSource source, float amount) {
        double speedThreshold = 0.1;
        if (this.velocity.length() < speedThreshold) {
            return false;
        }

        if (this.isInvulnerableTo(source)) {
            return false;
        }

        if (super.hurt(source, amount)) {
            if (this.getHealth() <= 0.0F) {
                this.die(source);
            }
            return true;
        }
        return false;
    }
    public Vec3 calculateFrictionFactor() {
        Vec3 vel = this.velocity;
        if (getStabMode() == StabMode.MANUAL) {

            vel = vel.scale(AIR_FRICTION);
        } else {
            vel = vel.scale(STAB_FRICTION);
        }
        return vel;
    }
    @Override
    public boolean isPersistenceRequired() {
        return true; // Сущность никогда не будет удалена автоматически
    }
    @Override
    public void tick() {
        super.tick();

        if (this.isInWater()) {
            syncDie();
            return;
        }

        prevRoll = droneRoll;
        prevYaw = droneYaw;
        prevPitch = dronePitch;
        prevTiltX = targetTiltX;
        prevTiltZ = targetTiltZ;
        this.rotorAngle = (this.rotorAngle + this.angularVelocity / 20f) % 360f;
//        if(level().isClientSide) {
//            System.out.println("CLIENT targetTiltX " + targetTiltX);
//            System.out.println("CLIENT targetTiltZ " + targetTiltX);
//        }
//        if(!level().isClientSide) {
//            System.out.println("SERVER targetTiltX " + targetTiltX);
//            System.out.println("SERVER targetTiltZ " + targetTiltX);
//        }
        if (!isLinked()) {
            this.velocity = new Vec3(0, -0.1, 0);
        }

        if (!level().isClientSide && isLinked()) {
            Vec3 newVel = calculateFrictionFactor();

            if (newVel.lengthSqr() > getSpeed() * getSpeed()) {
                newVel = newVel.normalize().scale(getSpeed());
            }

            ServerLevel serverLevel = (ServerLevel) level();
            Player controller = serverLevel.getPlayerByUUID(getControllerUUID());

            if (controller != null) {
                checkAcceptableArea(serverLevel, controller, newVel);
            }

            if (velocity != null) {

                this.velocity = newVel;
                setDeltaMovement(velocity);
                move(MoverType.SELF, velocity);
            }
        }
    }



    public void checkAcceptableArea(ServerLevel serverLevel, Player controller,  Vec3 velocity) {
        int simChunks = serverLevel.getServer().getPlayerList().getSimulationDistance();
        int viewChunks = serverLevel.getServer().getPlayerList().getViewDistance();
        int minChunks = Math.min(simChunks, viewChunks);
        double maxDist = (minChunks - 2) * 16.0;
        double maxDistSq = maxDist * maxDist;
        double warningDistSq = (maxDist * 0.8) * (maxDist * 0.8);

        Vec3 dronePos = this.position();
        Vec3 playerPos = controller.position();
        Vec3 toDrone = dronePos.subtract(playerPos);
        double distSq = toDrone.lengthSqr();

        ServerPlayer sp = (ServerPlayer) controller;

        if (distSq > warningDistSq && distSq < maxDistSq) {
            PacketSystem.CHANNEL.send(PacketDistributor.PLAYER.with(() -> sp),
                    new ActionBarMessagePacket("drone_message.warn_range"));
        }

        if (distSq > maxDistSq) {
            Vec3 outward = toDrone.normalize();
            double vOut = velocity.dot(outward);
            if (vOut > 0) {
                velocity = velocity.subtract(outward.scale(vOut));
            }

            this.velocity = Vec3.ZERO;
            setDeltaMovement(Vec3.ZERO);
            PacketSystem.CHANNEL.send(PacketDistributor.PLAYER.with(() -> sp),
                    new ActionBarMessagePacket("drone_message.out_of_range"));
            PacketSystem.CHANNEL.send(PacketDistributor.PLAYER.with(() -> sp),
                    new DroneDeathPacket(false, getControllerUUID()));

            unlinkController();
        }
    }

    /** CLIENT-SERVER */
    public void applyView(float yaw, float pitch, float roll) {
        setDroneDirection(yaw, pitch);
        if (getStabMode() == StabMode.FPV) setDroneRoll(roll);
    }
    public void applyMovement(Vec3 input) {
        this.velocity = this.velocity.add(input);
    }
    public void applyAnimations() {
        float partialTick = Minecraft.getInstance().getPartialTick();
        float ageInTicks = this.tickCount + partialTick;

        this.angularVelocity = isLinked() ? 720f : 360;

        float hoverAmplitude = 0.02f;
        float hoverSpeed = 0.1f;
        float hoverX = Mth.sin(ageInTicks * hoverSpeed) * hoverAmplitude;
        float hoverZ = Mth.cos(ageInTicks * hoverSpeed) * hoverAmplitude;
        float lerpFactor = 0.02f;

        this.bodyXRot = Mth.lerp(lerpFactor, this.bodyXRot , hoverX);
        this.bodyZRot = Mth.lerp(lerpFactor, this.bodyZRot, hoverZ);
    }
    public float getTiltFromFlightMode() {
        switch (getFlightMode()) {
            case SILENT -> {return 0.3f;}
            case SLOW -> {return 0.45f;}
            case NORMAL -> {return 0.6f;}
            case SPORT -> {return 0.8f;}
            case FORCED_SPORT -> {return 1.1f;}
        }
        return 0.5f;
    }
    public void updateTilt(float forwardInput, float sidewaysInput) {
        float maxTilt = getTiltFromFlightMode();


        float length = Mth.sqrt(forwardInput * forwardInput + sidewaysInput * sidewaysInput);
        if (length > 0) {
            forwardInput /= length;
            sidewaysInput /= length;
        }

        float combinedTilt = maxTilt * length;
        sidewaysInput = -sidewaysInput;

        targetTiltX = forwardInput * combinedTilt;
        targetTiltZ = sidewaysInput * combinedTilt;

        // Можно делать сглаживание здесь или на клиенте
        float lerpFactor = 0.2f;
        float newTiltX = Mth.lerp(lerpFactor, this.getTiltX(), targetTiltX);
        float newTiltZ = Mth.lerp(lerpFactor, this.getTiltZ(), targetTiltZ);

        this.setTiltX(newTiltX);
        this.setTiltZ(-newTiltZ);

    }

    /** NBT */
    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        writeNBT(tag);
        tag.putFloat("Yaw", getDroneYaw());
        tag.putFloat("Pitch", getDronePitch());
        tag.putFloat("Roll", getDroneRoll());
        tag.putString("FlightMode", getFlightMode().name());
        tag.putString("StabMode", getStabMode().name());
        tag.putString("VisionMode", getVisionMode().name());
        if (getControllerUUID() != null) {
            tag.putUUID("ControllerUUID", getControllerUUID());
        }

    }
    public void writeNBT(CompoundTag tag) {
        ListTag itemList = new ListTag();
        for (int i = 0; i < this.inventory.getContainerSize(); i++) {
            ItemStack stack = this.inventory.getItem(i);
            CompoundTag itemTag = new CompoundTag();
            if (!stack.isEmpty()) {
                stack.save(itemTag);
            }
            itemTag.putByte("Slot", (byte) i);
            itemList.add(itemTag);
        }
        tag.put("Items", itemList);
    }
    public void readNBT(CompoundTag tag) {
        ListTag itemList = tag.getList("Items", Tag.TAG_COMPOUND);
        this.items.clear();
        for (int i = 0; i < itemList.size(); i++) {
            CompoundTag itemTag = itemList.getCompound(i);
            int slot = itemTag.getByte("Slot") & 255;
            if (slot >= 0 && slot < this.inventory.getContainerSize()) {
                this.inventory.setItem(slot, ItemStack.of(itemTag));
            }
        }
    }
    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        readNBT(tag);
        setDroneDirection(tag.getFloat("Yaw"), tag.getFloat("Pitch"));
        setDroneRoll(tag.getFloat("Roll"));
        if (tag.contains("FlightMode")) {
            setFlightMode(FlightMode.valueOf(tag.getString("FlightMode")));
        }
        if (tag.contains("StabMode")) {
            setStabMode(StabMode.valueOf(tag.getString("StabMode")));
        }
        if (tag.contains("VisionMode")) {
            setVisionMode(VisionMode.valueOf(tag.getString("VisionMode")));
        }
        if (tag.hasUUID("ControllerUUID")) {
            this.setControllerUUID(tag.getUUID("ControllerUUID"));
        } else {
            this.setControllerUUID(null);
        }

    }




    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                  .add(Attributes.MOVEMENT_SPEED, 4.0)
                  .add(Attributes.MAX_HEALTH, 20.0);
    }


    /** STANDART SETTERS AND GETTER*/
    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {return new ClientboundAddEntityPacket(this);}
    public void setDroneDirection(float yaw,float pitch) {
        this.setDroneYaw(yaw);
        this.setDronePitch(pitch);
    }
    public float getDroneRoll() {
        return droneRoll;
    }
    public void setDronePitch(float pitch) {
        this.dronePitch = pitch;
    }
    public void setDroneYaw(float yaw) {
        this.droneYaw = yaw;
    }
    public void setDroneRoll(float roll) {
        this.droneRoll = roll;
    }
    public float getDroneYaw() {
        return droneYaw;
    }
    public float getDronePitch() {
        return dronePitch;
    }

    public void setFlightMode(FlightMode mode) {
        entityData.set(DATA_FLIGHT_MODE, mode.name());
    }
    public void setStabMode(StabMode mode) {
        entityData.set(DATA_STAB_MODE, mode.name());
    }
    public void setVisionMode(VisionMode mode) {
        this.entityData.set(DATA_VISION_MODE, mode.name());
    }
    public void setZoomMode(ZoomMode mode) {
        entityData.set(DATA_ZOOM_MODE, mode.name());
    }

    public FlightMode getFlightMode() {
        return FlightMode.valueOf(entityData.get(DATA_FLIGHT_MODE));
    }
    public StabMode getStabMode() {
        return StabMode.valueOf(entityData.get(DATA_STAB_MODE));
    }
    public VisionMode getVisionMode() {
        return VisionMode.valueOf(this.entityData.get(DATA_VISION_MODE));
    }
    public double getZoom() {
        return getZoomMode().zoomMpl;
    }
    public float getSpeed() {
        return switch (getStabMode()) {
            case FPV -> (float)getFlightMode().fpvSpeed;
            case MANUAL -> (float)getFlightMode().manualSpeed;
            default -> (float)getFlightMode().stabSpeed;
        };
    }

    public ZoomMode getZoomMode() {
        return ZoomMode.valueOf(entityData.get(DATA_ZOOM_MODE));
    }

    public boolean validateUpdates(Item item, int slotIndex) {
        if (slotIndex < 0 || slotIndex >= getContainerSize()) return false;
        ItemStack stack = inventory.getItem(slotIndex); // Или inventory.getStackInSlot(slotIndex);

        return !stack.isEmpty() && stack.getItem() == item;
    }
    public void cycleVisionMode() {
        VisionMode current = getCurrentVisionMode();
        List<VisionMode> availableModes = getAvailableVisionModes();
        int index = availableModes.indexOf(current);

        if (index == -1) {
            setVisionMode(availableModes.get(0));
            return;
        }

        int nextIndex = CTRL_KEY.isDown() ? (index - 1 + availableModes.size()) % availableModes.size() : (index + 1) % availableModes.size();
        VisionMode next = availableModes.get(nextIndex);
        setVisionMode(next);
        PacketSystem.CHANNEL.sendToServer(new DroneModesC2SP(getUUID(), getCurrentFlightMode(), getCurrentStabMode(), getCurrentZoomMode(), next));

    }
    public List<VisionMode> getAvailableVisionModes() {
        List<VisionMode> available = new ArrayList<>();
        available.add(VisionMode.NORMAL);
        if (validateUpdates(InitItems.IR_CONTROLLER.get(), 1)) {
            available.add(VisionMode.MONOCHROME);
            available.add(VisionMode.THERMOCHROME);
            if (validateUpdates(InitItems.TI_CONTROLLER.get(), 0)) {
                available.add(VisionMode.GREENCHROME);
                available.add(VisionMode.THERMAL);
            }
        }

        return available;
    }
    public VisionMode getCurrentVisionMode() {
        String stored = entityData.get(DATA_VISION_MODE);
        try {
            return VisionMode.valueOf(stored);
        } catch (IllegalArgumentException | NullPointerException e) {
            setVisionMode(VisionMode.NORMAL);
            return VisionMode.NORMAL;
        }
    }
    public void cycleStabMode() {
        StabMode current = getCurrentStabMode();
        List<StabMode> availableModes = getAvailableStabModes();
        int index = availableModes.indexOf(current);
        if (index == -1) {
            setStabMode(availableModes.get(0));
            return;
        }
        int nextIndex = CTRL_KEY.isDown() ? (index - 1 + availableModes.size()) % availableModes.size() : (index + 1) % availableModes.size();
        StabMode next = availableModes.get(nextIndex);
        setStabMode(next);
        PacketSystem.CHANNEL.sendToServer(new DroneModesC2SP(getUUID(), getCurrentFlightMode(), next, getCurrentZoomMode(), getCurrentVisionMode()));

    }
    public List<StabMode> getAvailableStabModes() {
        List<StabMode> available = new ArrayList<>();
        available.add(StabMode.FPV);

        if (validateUpdates(InitItems.STAB_CHIP.get(), 5)) {
            available.add(StabMode.STAB);
        }
        if (validateUpdates(InitItems.MANUAL_CHIP.get(), 6)) {
            available.add(StabMode.MANUAL);
        }
        return available;
    }
    public StabMode getCurrentStabMode() {
        String stored = entityData.get(DATA_STAB_MODE);
        try {
            return StabMode.valueOf(stored);
        } catch (IllegalArgumentException | NullPointerException e) {
            setStabMode(StabMode.FPV);
            return StabMode.FPV;
        }
    }

    public void cycleZoomMode() {
        ZoomMode current = getCurrentZoomMode();
        List<ZoomMode> availableModes = getAvailableZoomModes();
        int index = availableModes.indexOf(current);
        if (index == -1) {
            setZoomMode(availableModes.get(0));
            return;
        }
        int nextIndex = CTRL_KEY.isDown() ? (index - 1 + availableModes.size()) % availableModes.size() : (index + 1) % availableModes.size();
        ZoomMode next = availableModes.get(nextIndex);
        setZoomMode(next);
        PacketSystem.CHANNEL.sendToServer(new DroneModesC2SP(getUUID(), getCurrentFlightMode(), getCurrentStabMode(), next, getCurrentVisionMode()));

    }
    public List<ZoomMode> getAvailableZoomModes() {
        List<ZoomMode> available = new ArrayList<>();
        available.add(ZoomMode.DEF);
        if (validateUpdates(InitItems.ZOOM1.get(), 8)) {
            available.add(ZoomMode.X2);
            available.add(ZoomMode.X4);
            if (validateUpdates(InitItems.ZOOM2.get(), 3)) {
                available.add(ZoomMode.X8);
                available.add(ZoomMode.SPEC_ZOOM);
            }
        }

        return available;
    }
    public ZoomMode getCurrentZoomMode() {
        String stored = entityData.get(DATA_ZOOM_MODE);
        try {
            return ZoomMode.valueOf(stored);
        } catch (IllegalArgumentException | NullPointerException e) {
            setZoomMode(ZoomMode.DEF);
            return ZoomMode.DEF;
        }
    }
    public FlightMode getCurrentFlightMode() {
        String stored = entityData.get(DATA_FLIGHT_MODE);
        try {
            return FlightMode.valueOf(stored);
        } catch (IllegalArgumentException | NullPointerException e) {
            setFlightMode(FlightMode.NORMAL);
            return FlightMode.NORMAL;
        }
    }

    public void cycleFlightMode() {
        FlightMode current = getCurrentFlightMode();
        List<FlightMode> availableModes = getAvailableFlightModes();
        int index = availableModes.indexOf(current);
        if (index == -1) {
            setFlightMode(availableModes.get(0));
            return;
        }
        int nextIndex = CTRL_KEY.isDown() ? (index - 1 + availableModes.size()) % availableModes.size() : (index + 1) % availableModes.size();
        FlightMode next = availableModes.get(nextIndex);
        setFlightMode(next);
        PacketSystem.CHANNEL.sendToServer(new DroneModesC2SP(getUUID(), next, getCurrentStabMode(), getCurrentZoomMode(), getCurrentVisionMode()));

    }
    public List<FlightMode> getAvailableFlightModes() {
        List<FlightMode> available = new ArrayList<>();
        available.add(FlightMode.SILENT);
        available.add(FlightMode.SLOW);
        available.add(FlightMode.NORMAL);

        if (validateUpdates(InitItems.SPEED_CHIP.get(), 7)) {
            available.add(FlightMode.SPORT);
            if (validateUpdates(InitItems.SPEED_CHIP2.get(), 9)) {
                available.add(FlightMode.FORCED_SPORT);
            }
        }

        return available;
    }
    public UUID getControllerUUID() {
        return CONTROLLER_UUID;
    }
    public void setControllerUUID(UUID uuid) {
        this.CONTROLLER_UUID = uuid;
    }
    public void unlinkController() {
        this.CONTROLLER_UUID = null;
    }
    public boolean isLinked() {
        return CONTROLLER_UUID != null;
    }

    public void syncViewAndSimDistance(float syncView, float syncSimDist) {
        entityData.set(DATA_VIEW_DISTANCE, syncView);
        entityData.set(DATA_SIM_DISTANCE, syncSimDist);
    }
    public float getSyncView() {
        return entityData.get(DATA_VIEW_DISTANCE);
    }
    public float getSyncSimDist() {
        return entityData.get(DATA_SIM_DISTANCE);
    }
    public float getTiltX() {
        return this.targetTiltX;
    }

    public float getTiltZ() {
        return this.targetTiltZ;
    }

    public void setTiltX(float tiltX) {
        this.targetTiltX = tiltX;
    }

    public void setTiltZ(float tiltZ) {
        this.targetTiltZ = tiltZ;
    }
    /** CONTAINER */
    public Container getContainer() {
        return this.inventory;
    }
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new DroneEntityMenu(containerId, playerInventory, this);
    }
    @Override
    public Component getDisplayName() {
        return Component.translatable("container.drone_entity");
    }
    @Override
    public void clearContent() {
        this.items.clear();
    }

    @Override
    public int getContainerSize() {
        return 10;
    }

    @Override
    public void setLootTable(@Nullable ResourceLocation resourceLocation) {

    }

    @Override
    public void setLootTableSeed(long l) {

    }

    @Override
    public NonNullList<ItemStack> getItemStacks() {
        return items;
    }

    @Override
    public void clearItemStacks() {

    }

    @Override
    public boolean isEmpty() {
        for(ItemStack itemstack : items) {
            if (!itemstack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getItem(int index) {
        return this.items.get(index);
    }

    @Override
    public ItemStack removeItem(int index, int count) {
        return ContainerHelper.removeItem(this.items, index, count);
    }

    @Override
    public ItemStack removeItemNoUpdate(int index) {
        return ContainerHelper.takeItem(this.items, index);
    }

    @Override
    public void setItem(int index, ItemStack stack) {
        items.set(index, stack);
        if (!stack.isEmpty() && stack.getCount() > this.getMaxStackSize()) {
            stack.setCount(this.getMaxStackSize());
        }
    }

    @Override
    public void setChanged() {
        this.setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        if (this.isRemoved()) {
            return false;
        } else {
            return !(player.distanceToSqr(this) > 64.0D);
        }
    }

    @Override
    public void startOpen(Player player) {
    }

    @Override
    public void stopOpen(Player player) {
    }
    public interface ICyclableEnum<T extends Enum<T>> {
        T next();
        T prev();
    }
    public enum ZoomMode implements ICyclableEnum<ZoomMode> {
        DEF("zmode.def", 1),
        X2("zmode.x2",0.5),
        X4("zmode.x4",0.25),
        X8("zmode.x8",0.1),
        SPEC_ZOOM("zmode.spec_zoom",0.02);

        final String name;
        public final double zoomMpl;
        ZoomMode(String name, double zoomMpl) {
            this.name = name;
            this.zoomMpl = zoomMpl;
        }
        public Component getName() {
            return Component.translatable(name);
        }
        public ZoomMode next() {
            return values()[(this.ordinal() + 1) % values().length];
        }
        public ZoomMode prev() {
            int newOrdinal = (this.ordinal() - 1 + values().length) % values().length;
            return values()[newOrdinal];
        }
    }
    public enum VisionMode implements ICyclableEnum<VisionMode> {
        NORMAL("vmode.normal"),
        MONOCHROME("vmode.monochrome"),
        THERMOCHROME("vmode.thermochrome"),
        GREENCHROME("vmode.greenchrome"),
        THERMAL("vmode.thermal");

        String name;
        VisionMode(String name) {
            this.name = name;
        }
        public Component getName() {
            return Component.translatable(name);
        }
        public VisionMode next() {
            return values()[(this.ordinal() + 1) % values().length];
        }
        public VisionMode prev() {
            int newOrdinal = (this.ordinal() - 1 + values().length) % values().length;
            return values()[newOrdinal];
        }
    }
    public enum StabMode implements ICyclableEnum<StabMode> {
        STAB("smode.stab"),
        FPV("smode.fpv"),
        MANUAL("smode.manual");

        String name;
        StabMode(String name) {
            this.name = name;
        }
        public Component getName() {
            return Component.translatable(name);
        }
        public StabMode next() {
            return values()[(this.ordinal() + 1) % values().length];
        }
        public StabMode prev() {
            int newOrdinal = (this.ordinal() - 1 + values().length) % values().length;
            return values()[newOrdinal];
        }
    }
    public enum FlightMode implements ICyclableEnum<FlightMode> {
        SILENT("fmode.silent", 0.1, 0.11, 0.1),
        SLOW("fmode.slow",0.12, 0.13, 0.11),
        NORMAL("fmode.normal",0.29, 0.33, 0.23),
        SPORT("fmode.sport",0.8, 0.57, 0.47),
        FORCED_SPORT("fmode.forced_sport",0.9, 0.98, 0.8);

        public final String name;
        public final double fpvSpeed;
        public final double manualSpeed;
        public final double stabSpeed;
        FlightMode(String name, double fpvSpeed, double manualSpeed, double stabSpeed) {
            this.name = name;
            this.fpvSpeed = fpvSpeed;
            this.manualSpeed = manualSpeed;
            this.stabSpeed = stabSpeed;
        }

        public Component getName() {
            return Component.translatable(name);
        }

        public FlightMode next() {
            return values()[(this.ordinal() + 1) % values().length];
        }
        public FlightMode prev() {
            int newOrdinal = (this.ordinal() - 1 + values().length) % values().length;
            return values()[newOrdinal];
        }

    }
}
