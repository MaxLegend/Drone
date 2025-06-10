package ru.tesmio.drone.drone;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PacketDistributor;
import ru.tesmio.drone.Core;
import ru.tesmio.drone.packets.PacketSystem;
import ru.tesmio.drone.packets.client.*;
import ru.tesmio.drone.packets.server.DroneViewPacket;

import java.util.Optional;
import java.util.UUID;


//TODO: отвязка от пульта должна быть при покидании зоны, но не нужно постоянно держать getController.
// предмет должен помнить текущий НБТ и при взаимодействии предметом надо отправлять пакет, который
// будет проверять, можно ли соединится с этим дроном. И если можно, то он соединяется и опять
// записывает в controllerUUID дрона информацию о том, кто управляет
public class DroneEntity extends Mob {
    private UUID CONTROLLER_UUID;
    private static final EntityDataAccessor<Float> DATA_VIEW_DISTANCE = SynchedEntityData.defineId(DroneEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_SIM_DISTANCE = SynchedEntityData.defineId(DroneEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<String> DATA_FLIGHT_MODE = SynchedEntityData.defineId(DroneEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> DATA_STAB_MODE = SynchedEntityData.defineId(DroneEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> DATA_ZOOM_MODE = SynchedEntityData.defineId(DroneEntity.class, EntityDataSerializers.STRING);

    private static final EntityDataAccessor<Optional<UUID>> DATA_CONTROLLER = SynchedEntityData.defineId(DroneEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private Vec3 velocity = Vec3.ZERO;
    private float droneRoll, droneYaw, dronePitch;
    public float prevRoll, prevYaw, prevPitch;
    public float prevTiltX = 0f;
    public float prevTiltZ = 0f;
    private Vec3 lastInput = Vec3.ZERO;
    public double currentSpeed = 0.0; // Текущая скорость (можно сохранить и между тикками)
    public final double acceleration = 0.1; // Ускорение (изменяй по вкусу)


    public DroneEntity(EntityType<? extends Mob> type, Level world) {
        super(type, world);
      //  this.noPhysics = false;
        this.setPersistenceRequired();
        this.setNoGravity(false);
        this.setHealth(20.0f);
    }
    @Override
    public InteractionResult interactAt(Player player, Vec3 vec, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        ItemStack droneItem = new ItemStack(Core.ITEM_DRONE.get());
        ItemEntity itemEntity = new ItemEntity(
                this.level(), // уровень (мир)
                this.getX(), this.getY(), this.getZ(), // позиция (можно чуть сместить по Y, если нужно)
                droneItem // ItemStack для спавна
        );
        if (player.isShiftKeyDown() && !this.level().isClientSide && hand == InteractionHand.MAIN_HAND) {
            if(stack.isEmpty()) {
                discard();
                this.level().addFreshEntity(itemEntity);
            }
        }

        if (!stack.isEmpty() && stack.getItem() instanceof RemoteItem) {

            if (!this.level().isClientSide && !player.isPassenger()) {
                CompoundTag tag = stack.getOrCreateTag();
                if(tag.contains("DroneUUID"))return InteractionResult.sidedSuccess(false);

                tag.putUUID("DroneUUID", this.getUUID());
                this.CONTROLLER_UUID = player.getUUID();
                if (player instanceof ServerPlayer sp) {

                    int simChunks = sp.getServer().getPlayerList().getSimulationDistance();
                    int viewChunks = sp.getServer().getPlayerList().getViewDistance();
                    PacketSystem.CHANNEL.send(PacketDistributor.PLAYER.with(() -> sp),
                            new DroneControllerPacket(this.getUUID(), this.CONTROLLER_UUID));
                    PacketSystem.CHANNEL.send(PacketDistributor.PLAYER.with(() -> sp),
                            new DistanceControlPacket(simChunks,viewChunks, this.getId()));
                }
                return InteractionResult.sidedSuccess(player.level().isClientSide);
            }
        }
        return super.interactAt(player, vec, hand);
    }
    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_SIM_DISTANCE, 0f);
        this.entityData.define(DATA_VIEW_DISTANCE, 0f);
        this.entityData.define(DATA_FLIGHT_MODE, FlightMode.NORMAL.name());
        this.entityData.define(DATA_STAB_MODE, StabMode.STAB.name());
        this.entityData.define(DATA_ZOOM_MODE, ZoomMode.DEF.name());
        this.entityData.define(DATA_CONTROLLER, Optional.empty());
    }

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

    public UUID getControllerUUID() {
        return CONTROLLER_UUID;
    }
    public void setControllerUUID(UUID uuid) {
        this.CONTROLLER_UUID = uuid;
        this.entityData.set(DATA_CONTROLLER, Optional.ofNullable(uuid));
    }
    public void cycleZoomMode() {
        setZoomMode(getZoomMode().next());

    }
    public void setZoomMode(ZoomMode mode) {
        entityData.set(DATA_ZOOM_MODE, mode.name());
    }
    public ZoomMode getZoomMode() {
        return ZoomMode.valueOf(entityData.get(DATA_ZOOM_MODE));
    }
    public void cycleStabMode() {
        setStabMode(getStabMode().next());

    }
    public void cycleFlightMode() {
        setFlightMode(getFlightMode().next());
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
    @Override
    public boolean isAlwaysTicking() {return true;}
    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {return false;}

    @Override
    public boolean hurt(DamageSource source, float amount) {
        // Порог скорости, ниже которого урон не наносится
        double speedThreshold = 0.1;

        // Если скорость ниже порога — не наносим урон
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
    public boolean causeFallDamage(float fallDistance, float damageMultiplier, DamageSource source) {
        float minFallDistance =5.0f;
        if (fallDistance <= minFallDistance) {
            return false;
        }
        float damage = (fallDistance - minFallDistance) * 0.5f;
        this.hurt(source, damage);
        return false;
    }
    public boolean isLinked() {
        return CONTROLLER_UUID != null;
    }

    final Vec3 GRAVITY = new Vec3(0, -0.001, 0);
    final float AIR_FRICTION     = 0.98f;  // маленькое сопротивление воздуха
    final float STAB_FRICTION  = 0.90f;  // чуть гасит движение вперёд

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
                ServerPlayer sp = (ServerPlayer) controller;
                checkAcceptableArea(serverLevel, controller, newVel);
//                PacketSystem.CHANNEL.send(PacketDistributor.PLAYER.with(() -> sp),
//                        new DroneViewPacket(this.getUUID(), this.getDroneYaw(), this.getDronePitch(), this.getDroneRoll()));
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

                  this.setControllerUUID(getControllerUUID());
        }
    }
    public void applyClientView(float yaw, float pitch, float roll) {
        setDroneDirection(yaw, pitch);
        if(getStabMode() == StabMode.FPV) setDroneRoll(roll);

    }
    public void applyServerView(float yaw, float pitch, float roll) {

        setDroneDirection(yaw, pitch);
        if(getStabMode() == StabMode.FPV) setDroneRoll(roll);
    }
    public void applyServerMovement(Vec3 input) {
        this.lastInput = input;
        this.velocity = this.velocity.add(input);
    }

    public void applyClientMovement(Vec3 input) {
        this.lastInput = input;
        this.velocity = this.velocity.add(input);
    }
    public double getZoom() {
         double zoom = switch (getZoomMode()) {
             case DEF -> 1;
            case X2 -> 0.5;
            case X4 -> 0.25;
            case X8 -> 0.1;
             case SPEC_ZOOM -> 0.02;
        };
        return zoom;
    }
    public float getSpeed() {
        if(getStabMode() == StabMode.FPV) {
            double maxSpeed = switch (getFlightMode()) {
                case SILENT -> 0.1;
                case SLOW -> 0.12;
                case NORMAL -> 0.29;
                case SPORT -> 0.8;
                case FORCED_SPORT -> 0.9;
            };
            return (float)maxSpeed;
        }
        if(getStabMode() == StabMode.MANUAL) {
            double maxSpeed = switch (getFlightMode()) {
                case SILENT -> 0.11;
                case SLOW -> 0.13;
                case NORMAL -> 0.33;
                case SPORT -> 0.57;
                case FORCED_SPORT -> 0.98;
            };
            return (float)maxSpeed;
        }
        double maxSpeed = switch (getFlightMode()) {
            case SILENT -> 0.1;
            case SLOW -> 0.11;
            case NORMAL -> 0.23;
            case SPORT -> 0.47;
            case FORCED_SPORT -> 0.8;
        };
        return (float)maxSpeed;
    }
    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        tag.putFloat("Yaw", getDroneYaw());
        tag.putFloat("Pitch", getDronePitch());
        tag.putFloat("Roll", getDroneRoll());
        tag.putString("FlightMode", getFlightMode().name());
        tag.putString("StabMode", getStabMode().name());
        if (getControllerUUID() != null) {
            tag.putUUID("Controller", getControllerUUID());
        }
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
        if (tag.hasUUID("Controller")) {
            setControllerUUID(tag.getUUID("Controller"));
        }
    }
    public enum ZoomMode {
        DEF("zmode.def"),
        X2("zmode.x2"),
        X4("zmode.x4"),
        X8("zmode.x8"),
        SPEC_ZOOM("zmode.spec_zoom");

        String name;
        ZoomMode(String name) {
            this.name = name;
        }
        public Component getName() {
            return Component.translatable(name);
        }
        public ZoomMode next() {
            return values()[(this.ordinal() + 1) % values().length];
        }

    }
    public enum StabMode {
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
        public Component getDisplayMode() {
            return switch (this) {
                case STAB -> Component.translatable("drone.stab_mode.stab");
                case FPV -> Component.translatable("drone.stab_mode.fpv");
                case MANUAL -> Component.translatable("drone.stab_mode.manual");
            };
        }
    }
    public enum FlightMode {
        SILENT("fmode.silent"),
        SLOW("fmode.slow"),
        NORMAL("fmode.normal"),
        SPORT("fmode.sport"),
        FORCED_SPORT("fmode.forced_sport");

        String name;
        FlightMode(String name) {
            this.name = name;
        }

        public Component getName() {
            return Component.translatable(name);
        }

        public FlightMode next() {
            return values()[(this.ordinal() + 1) % values().length];
        }

        public Component getDisplayText() {
            return switch (this) {
                case SILENT -> Component.translatable("drone.flight_mode.silent");
                case SLOW -> Component.translatable("drone.flight_mode.slow");
                case NORMAL -> Component.translatable("drone.flight_mode.normal");
                case SPORT -> Component.translatable("drone.flight_mode.sport");
                case FORCED_SPORT -> Component.translatable("drone.flight_mode.forced_sport");
            };
        }
    }
    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                  .add(Attributes.MOVEMENT_SPEED, 4.0)
                  .add(Attributes.MAX_HEALTH, 20.0);
    }
}
