package ru.tesmio.drone.drone;

import net.minecraft.client.KeyMapping;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.PacketDistributor;
import ru.tesmio.drone.drone.quadcopter.DroneEntity;
import ru.tesmio.drone.packets.PacketSystem;
import ru.tesmio.drone.packets.client.DroneDeathPacket;
import ru.tesmio.drone.registry.InitItems;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static ru.tesmio.drone.drone.quadcopter.control.DroneController.CTRL_KEY;

public class BaseDroneEntity extends Mob {
    private static final EntityDataAccessor<Float> DATA_VIEW_DISTANCE = SynchedEntityData.defineId(DroneEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_SIM_DISTANCE = SynchedEntityData.defineId(DroneEntity.class, EntityDataSerializers.FLOAT);
    protected static final KeyMapping modifierKey = CTRL_KEY;
    public final float AIR_FRICTION = 0.98f;
    public final float STAB_FRICTION  = 0.90f;
    public UUID CONTROLLER_UUID;
    public BaseDroneEntity(EntityType<? extends Mob> type, Level world) {
        super(type, world);
        this.setPersistenceRequired();
        this.setNoGravity(false);
        this.setHealth(20.0f);
    }
    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        defineSynchedDroneData();
        this.entityData.define(DATA_SIM_DISTANCE, 0f);
        this.entityData.define(DATA_VIEW_DISTANCE, 0f);
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
    public void defineSynchedDroneData() {}
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

    @Override
    public boolean isAlwaysTicking() {return true;}
    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {return false;}



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

    public <T extends Enum<T> & ICyclableEnum<T>> void cycleMode(
            Supplier<T> getter,
            Consumer<T> setter) {

        T current = getter.get();
        T next = modifierKey.isDown() ? current.prev() : current.next();
        setter.accept(next);
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
