package ru.tesmio.drone.drone;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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
import net.minecraftforge.network.PacketDistributor;
import ru.tesmio.drone.Core;
import ru.tesmio.drone.packets.PacketSystem;
import ru.tesmio.drone.packets.client.ActionBarMessagePacket;
import ru.tesmio.drone.packets.client.DroneControllerPacket;
import ru.tesmio.drone.packets.client.DroneDeathPacket;
import ru.tesmio.drone.packets.client.DroneFlightModePacket;

import java.util.UUID;

public class DroneEntity extends Mob {
    private UUID controllingPlayerUUID;
    private Vec3 velocity = Vec3.ZERO;
    private static final EntityDataAccessor<Float> DATA_HEALTH_ID = SynchedEntityData.defineId(DroneEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_YAW = SynchedEntityData.defineId(DroneEntity.class, EntityDataSerializers.FLOAT);
       private static final EntityDataAccessor<Float> DATA_PITCH = SynchedEntityData.defineId(DroneEntity.class, EntityDataSerializers.FLOAT);
    private EnumFlightMode flightMode = EnumFlightMode.NORMAL;

    public DroneEntity(EntityType<? extends Mob> type, Level world) {
        super(type, world);
        this.noPhysics = false;
        this.setNoGravity(true);
    }


    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        if (controllingPlayerUUID != null) {
            tag.putUUID("Controller", controllingPlayerUUID);
        }
        tag.putString("FlightMode", flightMode.name());
    }




    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        if (tag.hasUUID("Controller")) {
            controllingPlayerUUID = tag.getUUID("Controller");
        }
        if (tag.contains("FlightMode")) {
            flightMode = EnumFlightMode.valueOf(tag.getString("FlightMode"));
        }
    }

    public void applyServerMovement(Vec3 input, float yaw,float currentPitch) {

          this.velocity = this.velocity.add(input);
    }
    public void applyClientMovement(Vec3 input, float yaw,float currentPitch) {
        this.velocity = this.velocity.add(input);
    }



    @Override
    public InteractionResult interactAt(Player player, Vec3 vec, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!stack.isEmpty() && stack.getItem() instanceof RemoteItem) {
            if (!this.level().isClientSide && !player.isPassenger()) {
                CompoundTag tag = stack.getOrCreateTag();
                tag.putUUID("DroneUUID", this.getUUID());
                this.controllingPlayerUUID = player.getUUID();
                if (player instanceof ServerPlayer sp) {
                    PacketSystem.CHANNEL.send(PacketDistributor.PLAYER.with(() -> sp),
                            new DroneControllerPacket(this.getUUID(), this.controllingPlayerUUID));
                }
                return InteractionResult.sidedSuccess(player.level().isClientSide);
            }
        }
        return super.interactAt(player, vec, hand);
    }



    @Override
    public void tick() {
        super.tick();
        Vec3 newVel = this.velocity.scale(0.75);
        double maxSpeed = switch (flightMode) {
            case SLOW -> 0.2;
            case NORMAL -> 0.6;
            case SPORT -> 1.2;
        };
        //  System.out.println("SIDE " + level().isClientSide + " MODE " + getFlightMode());
        //   System.out.println("maxSpeed " + maxSpeed + " " + newVel.lengthSqr() + flightMode);
        if (newVel.lengthSqr() > maxSpeed * maxSpeed) {
            newVel = newVel.normalize().scale(maxSpeed);
        }
        if (!level().isClientSide && controllingPlayerUUID != null) {
            ServerLevel serverLevel = (ServerLevel) level();
            Player controller = serverLevel.getPlayerByUUID(controllingPlayerUUID);

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
                            new DroneDeathPacket(false, controllingPlayerUUID));

                    this.controllingPlayerUUID = null;
                }
            }
            this.velocity = newVel;
            setDeltaMovement(velocity);
            move(MoverType.SELF, velocity);
            setYBodyRot(getYRot());
        }

    }

    @Override
    public void die(DamageSource cause) {
        super.die(cause);
        if (!level().isClientSide && controllingPlayerUUID != null) {
            Player player = level().getPlayerByUUID(controllingPlayerUUID);
            if (player instanceof ServerPlayer sp) {
                PacketSystem.CHANNEL.send(PacketDistributor.PLAYER.with(() -> sp),
                        new DroneDeathPacket(false, controllingPlayerUUID));
            }
        }
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                  .add(Attributes.MOVEMENT_SPEED, 4.0)
                  .add(Attributes.MAX_HEALTH, 1.0);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData(); // обязательно!
        this.entityData.define(DATA_HEALTH_ID, 20.0f);
        this.entityData.define(DATA_YAW, 0f);
        this.entityData.define(DATA_PITCH, 0f);
    }
    @Override
    public boolean isAlwaysTicking() {return true;}
    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {return new ClientboundAddEntityPacket(this);}
    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {return false;}
    public float getDroneYaw() {return entityData.get(DATA_YAW);}
    public void setDroneYaw(float yaw) {entityData.set(DATA_YAW, yaw);}
    public float getDronePitch() {return entityData.get(DATA_PITCH);}
    public void setDronePitch(float pitch) {entityData.set(DATA_PITCH, pitch);}
    public EnumFlightMode getFlightMode() {return flightMode;}
    public void setFlightMode(EnumFlightMode mode) {this.flightMode = mode;}
    public void cycleFlightMode() {
        setFlightMode(flightMode.next());

        if (!level().isClientSide && controllingPlayerUUID != null) {
            Player player = level().getPlayerByUUID(controllingPlayerUUID);
            if (player instanceof ServerPlayer sp) {
                PacketSystem.CHANNEL.send(PacketDistributor.PLAYER.with(() -> sp),
                        new DroneFlightModePacket(this.flightMode));
            }
        }
    }
    public void setControllingPlayerUUIDClient(UUID uuid) {this.controllingPlayerUUID = uuid;}
    public UUID getControllingPlayerUUID() {return controllingPlayerUUID;}
}
