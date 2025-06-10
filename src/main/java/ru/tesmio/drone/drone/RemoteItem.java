package ru.tesmio.drone.drone;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;


import java.util.UUID;

public class RemoteItem extends Item {

    public RemoteItem(Properties properties) {
        super(properties);
    }
    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();

        return InteractionResult.FAIL;
    }
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {

        if (!level.isClientSide) {
            return InteractionResultHolder.fail(player.getItemInHand(hand));
        }

        ItemStack stack = player.getItemInHand(hand);
        UUID targetUUID = getLinkedDroneUUID(stack);

        DroneEntity drone = findDroneByUUID(player, targetUUID);
        CompoundTag tag = stack.getOrCreateTag();
        if( player.isShiftKeyDown() && tag.contains("DroneUUID")) {
            if (drone != null) {
                stack.setTag(null);
                drone.setControllerUUID(null);
            }

            return InteractionResultHolder.success(stack);
        }
        if (drone != null  ) {
            Minecraft.getInstance().setCameraEntity(drone);
            return InteractionResultHolder.success(stack);
        } else {
            return InteractionResultHolder.fail(stack);
        }

    }

    private UUID getLinkedDroneUUID(ItemStack stack) {
        CompoundTag tag = stack.getOrCreateTag();
        return tag.contains("DroneUUID") ? tag.getUUID("DroneUUID") : null;
    }

    private DroneEntity findDroneByUUID(Player player, UUID uuid) {
        if (uuid == null) return null;

        return player.level().getEntitiesOfClass(DroneEntity.class, player.getBoundingBox().inflate(64)).stream()
                     .filter(d -> uuid.equals(d.getUUID()))
                     .findFirst().orElse(null);
    }
}
