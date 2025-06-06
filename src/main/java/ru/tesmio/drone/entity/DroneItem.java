package ru.tesmio.drone.entity;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import ru.tesmio.drone.Core;

public class DroneItem extends Item {
    public DroneItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide) {
            DroneEntity drone = Core.DRONE.get().create(level);
            if (drone != null) {
                drone.moveTo(player.getX(), player.getY() + 1, player.getZ(), player.getYRot(), 0);
                level.addFreshEntity(drone);
            }
        }
        return InteractionResultHolder.success(player.getItemInHand(hand));
    }
}