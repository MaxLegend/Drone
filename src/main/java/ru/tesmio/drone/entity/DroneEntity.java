package ru.tesmio.drone.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

public class DroneEntity extends Mob {
    private UUID controllingPlayerUUID;
    private Vec3 velocity = Vec3.ZERO;
    private static final EntityDataAccessor<Float> DATA_HEALTH_ID = SynchedEntityData.defineId(DroneEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_YAW = SynchedEntityData.defineId(DroneEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_PITCH = SynchedEntityData.defineId(DroneEntity.class, EntityDataSerializers.FLOAT);

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
    }
    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }
    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        if (tag.hasUUID("Controller")) {
            controllingPlayerUUID = tag.getUUID("Controller");
        }
    }
    public void applyServerMovement(Vec3 input, float yaw,float currentPitch) {
        this.velocity = this.velocity.add(input);
        //    this.setDroneYaw(yaw);
        //    this.setDronePitch(currentPitch);
    }
    public void applyClientMovement(Vec3 input, float yaw,float currentPitch) {
        this.velocity = this.velocity.add(input);
        //     this.setDroneYaw(yaw);
        //      this.setDronePitch(currentPitch);
    }

    @Override
    public InteractionResult interactAt(Player player, Vec3 vec, InteractionHand hand) {

        ItemStack stack = player.getItemInHand(hand);
        if (!stack.isEmpty() && stack.getItem() instanceof RemoteItem) {
            if (!this.level().isClientSide && !player.isPassenger()) {
                CompoundTag tag = stack.getOrCreateTag();
                tag.putUUID("DroneUUID", this.getUUID());

                return InteractionResult.sidedSuccess(player.level().isClientSide);
            }
        }
        return super.interactAt(player, vec, hand);
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return new ClientboundAddEntityPacket(this);
    }
    @Override
    public void tick() {
        super.tick();
        velocity = velocity.scale(0.60);
        if (velocity.lengthSqr() > 0.4 * 0.4) {
            velocity = velocity.normalize().scale(0.6);
        }
        setDeltaMovement(velocity);
        move(MoverType.SELF, velocity);
        setYBodyRot(getYRot());

    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                  .add(Attributes.MOVEMENT_SPEED, 4.0)
                  .add(Attributes.MAX_HEALTH, 1.0);
    }
    public UUID getControllingPlayerUUID() {
        return controllingPlayerUUID;
    }
    @Override
    protected void defineSynchedData() {
        super.defineSynchedData(); // обязательно!
        this.entityData.define(DATA_HEALTH_ID, 20.0f);
        this.entityData.define(DATA_YAW, 0f);
        this.entityData.define(DATA_PITCH, 0f);
    }
    public float getDroneYaw() {
        return entityData.get(DATA_YAW);
    }
    public void setDroneYaw(float yaw) {
        entityData.set(DATA_YAW, yaw);
    }
    public float getDronePitch() {
        return entityData.get(DATA_PITCH);
    }
    public void setDronePitch(float pitch) {
        entityData.set(DATA_PITCH, pitch);
    }

}
