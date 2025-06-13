package ru.tesmio.drone.drone.quadcopter;

import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
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
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;
import ru.tesmio.drone.Dronecraft;
import ru.tesmio.drone.drone.BaseDroneEntity;
import ru.tesmio.drone.drone.quadcopter.container.DroneEntityMenu;
import ru.tesmio.drone.packets.PacketSystem;
import ru.tesmio.drone.packets.client.*;
import ru.tesmio.drone.registry.InitItems;


//TODO: отвязка от пульта должна быть при покидании зоны, но не нужно постоянно держать getController.
// предмет должен помнить текущий НБТ и при взаимодействии предметом надо отправлять пакет, который
// будет проверять, можно ли соединится с этим дроном. И если можно, то он соединяется и опять
// записывает в controllerUUID дрона информацию о том, кто управляет
public class DroneEntity extends BaseDroneEntity implements ContainerEntity, MenuProvider {
    public DroneEntity INSTANCE;
    private NonNullList<ItemStack> items = NonNullList.withSize(10, ItemStack.EMPTY);
    @Nullable
    private ResourceLocation lootTable;
    private long lootTableSeed;
    private static final EntityDataAccessor<String> DATA_FLIGHT_MODE = SynchedEntityData.defineId(DroneEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> DATA_STAB_MODE = SynchedEntityData.defineId(DroneEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> DATA_ZOOM_MODE = SynchedEntityData.defineId(DroneEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> DATA_VISION_MODE = SynchedEntityData.defineId(DroneEntity.class, EntityDataSerializers.STRING);
 //   private static final EntityDataAccessor<Optional<UUID>> DATA_CONTROLLER = SynchedEntityData.defineId(DroneEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private Vec3 velocity = Vec3.ZERO;
    private float droneRoll, droneYaw, dronePitch;
    public float prevRoll, prevYaw, prevPitch;
    public float prevTiltX = 0f;
    public float prevTiltZ = 0f;
    private Vec3 lastInput = Vec3.ZERO;
    public double currentSpeed = 0.0; // Текущая скорость (можно сохранить и между тикками)
    public final double acceleration = 0.01; // Ускорение (изменяй по вкусу)




    public DroneEntity(EntityType<? extends Mob> type, Level world) {
        super(type, world);
        this.setPersistenceRequired();
        this.setNoGravity(false);
        this.setHealth(20.0f);
    }

    @Override
    public InteractionResult interactAt(Player player, Vec3 vec, InteractionHand hand) {
        // Ранний выход для клиентской стороны или неосновной руки
        if (!level().isClientSide && hand == InteractionHand.MAIN_HAND && player.isShiftKeyDown()) {

            NetworkHooks.openScreen( (ServerPlayer)player, new MenuProvider() {
                @Override
                public Component getDisplayName() {
                    return Component.literal("Drone Control"); // Можно любое имя GUI
                }

                @Override
                public AbstractContainerMenu createMenu(int windowId, Inventory inv, Player player) {
                    return new DroneEntityMenu(windowId, inv, DroneEntity.this);
                }
            }, buffer -> {
                buffer.writeInt(this.getId());
            });
            return InteractionResult.CONSUME;
        }
        if (hand != InteractionHand.MAIN_HAND || level().isClientSide) {
            return super.interactAt(player, vec, hand);
        }

        ItemStack stack = player.getItemInHand(hand);

        // Обработка сброса дрона при нажатом Shift
        if (player.isShiftKeyDown() && stack.isEmpty()) {
            discardDrone();
            return InteractionResult.SUCCESS;
        }

        // Обработка подключения пульта управления
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
        this.CONTROLLER_UUID = player.getUUID();

        if (player instanceof ServerPlayer serverPlayer) {
            sendConnectionPackets(serverPlayer);
        }

        return true;
    }

    private void sendConnectionPackets(ServerPlayer player) {
        int simDistance = player.server.getPlayerList().getSimulationDistance();
        int viewDistance = player.server.getPlayerList().getViewDistance();

        PacketSystem.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new DroneControllerPacket(getUUID(), CONTROLLER_UUID));

        PacketSystem.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new DistanceControlPacket(simDistance, viewDistance, getId()));
    }

    @Override
    public void defineSynchedDroneData(){
        this.entityData.define(DATA_FLIGHT_MODE, FlightMode.NORMAL.name());
        this.entityData.define(DATA_STAB_MODE, StabMode.STAB.name());
        this.entityData.define(DATA_ZOOM_MODE, ZoomMode.DEF.name());
        this.entityData.define(DATA_VISION_MODE, VisionMode.NORMAL.name());
    }
    public float getYRot() {
        return this.droneYaw;
    }
    public float getXRot() {
        return this.dronePitch;
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
    public void tick() {
        super.tick();
        if (this.isInWater()) {
            syncDie();
            return;
        }

        prevRoll = droneRoll;
        prevYaw = droneYaw;
        prevPitch = dronePitch;
        if(!isLinked()) this.velocity = new Vec3(0, -0.1, 0);
        Vec3 newVel = calculateFrictionFactor();
        if (newVel.lengthSqr() > getSpeed() * getSpeed()) {
            newVel = newVel.normalize().scale(getSpeed());
        }
        if (!level().isClientSide && isLinked()) {

            ServerLevel serverLevel = (ServerLevel) level();
            Player controller = serverLevel.getPlayerByUUID(getControllerUUID());

            if (controller != null) {
                checkAcceptableArea(serverLevel, controller, newVel);
            }
        }
        if(velocity != null) {
            this.velocity = newVel;
            setDeltaMovement(velocity);
            move(MoverType.SELF, velocity);
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
        if(getStabMode() == StabMode.FPV) setDroneRoll(roll);
    }
    public void applyMovement(Vec3 input) {
        this.lastInput = input;
        this.velocity = this.velocity.add(input);
    }


    /** NBT */
    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        tag.putFloat("Yaw", getDroneYaw());
        tag.putFloat("Pitch", getDronePitch());
        tag.putFloat("Roll", getDroneRoll());
        tag.putString("FlightMode", getFlightMode().name());
        tag.putString("StabMode", getStabMode().name());
        tag.putString("VisionMode", getVisionMode().name());
        if (getControllerUUID() != null) {
            tag.putUUID("Controller", getControllerUUID());
        }
        this.addChestVehicleSaveData(tag);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
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
        this.readChestVehicleSaveData(tag);
    }


    /** MODES */

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


    public StabMode getStabMode() {
        return StabMode.valueOf(entityData.get(DATA_STAB_MODE));
    }
    public void setStabMode(StabMode mode) {
        entityData.set(DATA_STAB_MODE, mode.name());
    }
    public FlightMode getFlightMode() {
        return FlightMode.valueOf(entityData.get(DATA_FLIGHT_MODE));
    }
    public void setFlightMode(FlightMode mode) {
        entityData.set(DATA_FLIGHT_MODE, mode.name());
    }
    public void setVisionMode(VisionMode mode) {
        this.entityData.set(DATA_VISION_MODE, mode.name());
    }
    public VisionMode getVisionMode() {
        return VisionMode.valueOf(this.entityData.get(DATA_VISION_MODE));
    }
    public void cycleVisionMode() {
        cycleMode(this::getVisionMode, this::setVisionMode);
    }
    public void setZoomMode(ZoomMode mode) {
        entityData.set(DATA_ZOOM_MODE, mode.name());
    }
    public ZoomMode getZoomMode() {
        return ZoomMode.valueOf(entityData.get(DATA_ZOOM_MODE));
    }
    public void cycleStabMode() {
        cycleMode(this::getStabMode, this::setStabMode);
    }
    public void cycleZoomMode() {
        cycleMode(this::getZoomMode, this::setZoomMode);
    }
    public void cycleFlightMode() {
        cycleMode(this::getFlightMode, this::setFlightMode);
    }



    @Override
    public void setLootTable(@Nullable ResourceLocation lootTable) {
        this.lootTable = lootTable;
    }

    @Override
    public void setLootTableSeed(long seed) {
        this.lootTableSeed = seed;
    }

    @Override
    public NonNullList<ItemStack> getItemStacks() {
        return this.items;
    }
    @Override
    public void clearItemStacks() {
        this.items = NonNullList.withSize(10, ItemStack.EMPTY);
    }

    @Override
    public int getContainerSize() {
        return this.items.size();
    }

    @Override
    public ItemStack getItem(int i) {
        return null;
    }

    @Override
    public ItemStack removeItem(int index, int count) {
        return this.removeChestVehicleItem(index, count);
    }

    @Override
    public ItemStack removeItemNoUpdate(int index) {
        return this.removeChestVehicleItemNoUpdate(index);
    }

    @Override
    public void setItem(int index, ItemStack stack) {
        this.setChestVehicleItem(index, stack);
    }

    @Override
    public void setChanged() {

    }

    @Override
    public boolean stillValid(Player player) {
        return this.isChestVehicleStillValid(player);
    }

    @Override
    public void clearContent() {
        this.clearChestVehicleContent();
    }
    @Override
    public Component getDisplayName() {
        return Component.literal("Drone Upgrades");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int windowId, Inventory playerInventory, Player player) {
        return new DroneEntityMenu(windowId, playerInventory, this);
    }
}
