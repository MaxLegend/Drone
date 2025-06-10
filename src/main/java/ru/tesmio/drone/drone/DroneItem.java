package ru.tesmio.drone.drone;

import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import ru.tesmio.drone.Core;

public class DroneItem extends Item {
    public DroneItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        Player player = context.getPlayer();
        Direction side = context.getClickedFace();

        double spawnX = context.getClickedPos().getX() +1+side.getStepX()-0.5;
        double spawnY = context.getClickedPos().getY() + side.getStepY();
        double spawnZ = context.getClickedPos().getZ() + 1+ side.getStepZ()-0.5;

        DroneEntity drone = Core.DRONE.get().create(level);
        if (drone != null) {
            drone.moveTo(spawnX, spawnY, spawnZ, player != null ? player.getYRot() : 0, 0);
            level.addFreshEntity(drone);
        }

        return InteractionResult.SUCCESS;
    }

}