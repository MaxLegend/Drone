package ru.tesmio.drone.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
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
import ru.tesmio.drone.Core;

import java.util.UUID;

/** TODO: добавить: HUD, возможность регулировать фокусное расстояние, при уничтожении дрона - возврат обратно в игрока
 * TODO: добавить радиус действия, если дрон вылетел за радиус - возвращать камеру в игрока. Добавить дрону уровень заряда
 * TODO: Добавить уровень заряда пульту, отображение к какому дрону привязан. Добавить красивый рендер пульта в руках
 * на основе типа как игрок карту держит. Вернуть дрону ХП, если с определенной скоростью (в любом направлении) врезался в блок -
 * убить энтити. На HUD должно отображаться: текущие координаты дрона, фокусное расстояние, текущая скорость, текущее ускорение
 * добавить также рамку - эффект что это дисплей
*/
public class DroneEntity extends Mob {
    private UUID controllingPlayerUUID;
    private Vec3 velocity = Vec3.ZERO;
    private float movementSpeedMultiplier = 1.0f;
    private static final EntityDataAccessor<Float> DATA_HEALTH_ID = SynchedEntityData.defineId(DroneEntity.class, EntityDataSerializers.FLOAT);

    public DroneEntity(EntityType<? extends Mob> type, Level level) {
        super(type, level);
    }
    @Override
    protected void dropAllDeathLoot(DamageSource damageSource) {
        spawnAtLocation(new ItemStack(Core.ITEM_DRONE.get())); // Дроп предмета дрона
    }
    public void adjustSpeed(float delta) {
        movementSpeedMultiplier = Mth.clamp(movementSpeedMultiplier + delta, 0.1f, 5.0f);
    }

    public float getSpeed() {
        return movementSpeedMultiplier;
    }

    public void setSpeed(float speed) {
        this.movementSpeedMultiplier = speed;
    }
    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                  .add(Attributes.MOVEMENT_SPEED, 1.0)
                  .add(Attributes.MAX_HEALTH, 1.0);
    }

    public void setControllingPlayer(Player player) {
        this.controllingPlayerUUID = player.getUUID();
    }

    public UUID getControllingPlayerUUID() {
        return controllingPlayerUUID;
    }
    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!level().isClientSide) {
            this.kill(); // мгновенное уничтожение
        }
        return true;
    }
    @Override
    public void tick() {
        super.tick();
      //  System.out.println("movementSpeedMultiplier " + movementSpeedMultiplier);
        // Инерция
        velocity = velocity.scale(0.60); // трение
        if (velocity.lengthSqr() > 0.4 * 0.4) {
            velocity = velocity.normalize().scale(0.6 * movementSpeedMultiplier);
        }
        setDeltaMovement(velocity);
        move(MoverType.SELF, velocity); // явное движение
        setYHeadRot(getYRot());
        setYBodyRot(getYRot());
    }
    public void applyServerMovement(Vec3 input, float yaw, float pitch) {
        this.velocity = this.velocity.add(input);
        // Синхронизируем поворот
        this.setYRot(yaw);
        this.setXRot(pitch);
        this.yRotO = yaw;
        this.xRotO = pitch;

    }
    public void applyClientMovement(Vec3 input) {
        // Увеличение скорости от ввода
        this.velocity = this.velocity.add(input);
    }
//    @Override
//    public float getViewXRot(float partialTicks) {
//        return this.getXRot(); // Отключаем интерполяцию
//    }
//
//    @Override
//    public float getViewYRot(float partialTicks) {
//        return this.getYRot(); // Отключаем интерполяцию
//    }
    @Override
    protected void defineSynchedData() {
        super.defineSynchedData(); // обязательно!
        this.entityData.define(DATA_HEALTH_ID, 20.0f);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        if (controllingPlayerUUID != null) {
            tag.putUUID("Controller", controllingPlayerUUID);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        if (tag.hasUUID("Controller")) {
            controllingPlayerUUID = tag.getUUID("Controller");
        }
    }

    @Override
    public InteractionResult interactAt(Player player, Vec3 vec, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!stack.isEmpty() && stack.getItem() instanceof RemoteItem) {
            CompoundTag tag = stack.getOrCreateTag();
            tag.putUUID("DroneUUID", this.getUUID());
            return InteractionResult.sidedSuccess(player.level().isClientSide);
        }
        return super.interactAt(player, vec, hand);
    }
}