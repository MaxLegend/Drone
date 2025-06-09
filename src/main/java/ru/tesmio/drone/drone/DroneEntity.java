package ru.tesmio.drone.drone;

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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.PacketDistributor;
import org.stringtemplate.v4.ST;
import ru.tesmio.drone.droneold.RemoteItem;
import ru.tesmio.drone.packets.PacketSystem;
import ru.tesmio.drone.packets.client.*;
import java.util.UUID;

//после перезахода сбрасывается uuid, после вылета за зону сбрасывается uuid и обратно не возвращается. починить
public class DroneEntity extends Mob {
    private UUID CONTROLLER_UUID;
    private static final EntityDataAccessor<Float> DATA_VIEW_DISTANCE = SynchedEntityData.defineId(DroneEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_SIM_DISTANCE = SynchedEntityData.defineId(DroneEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<String> DATA_FLIGHT_MODE = SynchedEntityData.defineId(DroneEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> DATA_STAB_MODE = SynchedEntityData.defineId(DroneEntity.class, EntityDataSerializers.STRING);

    private Vec3 velocity = Vec3.ZERO;
    private float droneRoll, rollVelocity;
    public float prevRoll;
    public float prevTiltX = 0f;
    public float prevTiltZ = 0f;


    public DroneEntity(EntityType<? extends Mob> type, Level world) {
        super(type, world);
        this.noPhysics = false;
        this.setNoGravity(true);
        this.setHealth(20.0f);
    }
    @Override
    public InteractionResult interactAt(Player player, Vec3 vec, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!stack.isEmpty() && stack.getItem() instanceof RemoteItem) {

            if (!this.level().isClientSide && !player.isPassenger()) {
                CompoundTag tag = stack.getOrCreateTag();
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
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {return new ClientboundAddEntityPacket(this);}
    public float getDroneYaw() { return getYRot(); }
    public void setDroneDirection(float yaw,float pitch) {
        this.setYRot(yaw);
        this.setXRot(pitch);
    }
    public float getDroneRoll() {
        return droneRoll;
    }

    public void setDroneRoll(float roll) {

        this.droneRoll = roll;
    }

    public float getDronePitch() { return getXRot(); }


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
    }
    public void cycleStabMode() {
        setStabMode(getStabMode().next());
//        if (!level().isClientSide && getControllerUUID() != null) {
//            Player player = level().getPlayerByUUID(getControllerUUID());
//            if (player instanceof ServerPlayer sp) {
//                PacketSystem.CHANNEL.send(PacketDistributor.PLAYER.with(() -> sp),
//                        new DroneStabModePacket(getStabMode()));
//            }
//        }
    }
    public void cycleFlightMode() {
        setFlightMode(getFlightMode().next());
//        if (!level().isClientSide && getControllerUUID() != null) {
//            Player player = level().getPlayerByUUID(getControllerUUID());
//            if (player instanceof ServerPlayer sp) {
//                PacketSystem.CHANNEL.send(PacketDistributor.PLAYER.with(() -> sp),
//                        new DroneFlightModePacket(getFlightMode()));
//            }
//        }
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
    public void die(DamageSource cause) {
        super.die(cause);

        if (!level().isClientSide && getControllerUUID() != null) {
            Player player = level().getPlayerByUUID(getControllerUUID());
            if (player instanceof ServerPlayer sp) {

                PacketSystem.CHANNEL.send(PacketDistributor.PLAYER.with(() -> sp),
                        new DroneDeathPacket(false, getControllerUUID()));
            }
        }
    }
    @Override
    public boolean causeFallDamage(float fallDistance, float damageMultiplier, DamageSource source) {
        float safeDistance = 3.0f;
        if (fallDistance <= safeDistance) {
            return false;
        }
        return super.causeFallDamage(fallDistance, damageMultiplier, source);
    }
    @Override
    public void tick() {
        super.tick();

        prevRoll = droneRoll;
        Vec3 newVel = this.velocity.scale(0.9);
        if (newVel.lengthSqr() > getSpeed() * getSpeed()) {
            newVel = newVel.normalize().scale(getSpeed());
        }
        if (!level().isClientSide && getControllerUUID() != null) {
            ServerLevel serverLevel = (ServerLevel) level();
            Player controller = serverLevel.getPlayerByUUID(getControllerUUID());

            if (controller != null) {
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
                    double vOut = newVel.dot(outward);
                    if (vOut > 0) {
                        newVel = newVel.subtract(outward.scale(vOut));
                    }

                    this.velocity = Vec3.ZERO;
                    setDeltaMovement(Vec3.ZERO);
                    PacketSystem.CHANNEL.send(PacketDistributor.PLAYER.with(() -> sp),
                            new ActionBarMessagePacket("drone_message.out_of_range"));
                    PacketSystem.CHANNEL.send(PacketDistributor.PLAYER.with(() -> sp),
                            new DroneDeathPacket(false, getControllerUUID()));

                    this.setControllerUUID(null);
                }
            }
        }
        this.velocity = newVel;
        setDeltaMovement(velocity);
        move(MoverType.SELF, velocity);
        setYBodyRot(getDroneYaw());
    }

    public void applyServerMovement(Vec3 input, float yaw, float pitch, float roll) {
        this.velocity = this.velocity.add(input);
        setDroneDirection(yaw, pitch);
        if(getStabMode() == StabMode.FPV) setDroneRoll(roll);
    }

    public void applyClientMovement(Vec3 input, float yaw, float pitch, float roll) {
        this.velocity = this.velocity.add(input);
        setDroneDirection(yaw, pitch);
        if(getStabMode() == StabMode.FPV) setDroneRoll(roll);
    }
    public float getSpeed() {
        if(getStabMode() == StabMode.FPV) {
            double maxSpeed = switch (getFlightMode()) {
                case SILENT -> 0.07;
                case SLOW -> 0.12;
                case NORMAL -> 0.4;
                case SPORT -> 0.8;
                case FORCED_SPORT -> 1.8;
            };
            return (float)maxSpeed;
        }
        double maxSpeed = switch (getFlightMode()) {
            case SILENT -> 0.04;
            case SLOW -> 0.08;
            case NORMAL -> 0.28;
            case SPORT -> 0.57;
            case FORCED_SPORT -> 1.4;
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
    public enum StabMode {
        STAB,
        FPV;

        StabMode() {}
        public StabMode next() {
            return values()[(this.ordinal() + 1) % values().length];
        }
        public Component getDisplayMode() {
            return switch (this) {
                case STAB -> Component.translatable("drone.stab_mode.stab");
                case FPV -> Component.translatable("drone.stab_mode.fpv");
            };
        }
    }
    public enum FlightMode {
        SILENT,
        SLOW,
        NORMAL,
        SPORT,
        FORCED_SPORT;

        FlightMode() {}

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
                  .add(Attributes.MAX_HEALTH, 1.0);
    }
}
