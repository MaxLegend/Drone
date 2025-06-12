package ru.tesmio.drone.drone.quadcopter;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import ru.tesmio.drone.packets.PacketSystem;
import ru.tesmio.drone.packets.server.DroneReconnectPacket;


import java.util.UUID;

public class RemoteItem extends Item {

    public RemoteItem(Properties properties) {
        super(properties);
    }
    @Override
    public InteractionResult useOn(UseOnContext context) {
        return InteractionResult.PASS;
    }

    // ПКМ по сущности (в том числе по дрону)
    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (!(target instanceof DroneEntity drone)) return InteractionResult.PASS;

        if (!player.level().isClientSide) {
            CompoundTag tag = stack.getOrCreateTag();
            tag.putUUID("DroneUUID", drone.getUUID());
            stack.setTag(tag);
            drone.setControllerUUID(player.getUUID());

            player.displayClientMessage(Component.literal("§aДрон успешно привязан."), true);
        }

        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        // Очистка связи при SHIFT + ПКМ в воздухе
        if (player.isShiftKeyDown()) {
            CompoundTag tag = stack.getOrCreateTag();
            if (tag.contains("DroneUUID")) {
                tag.remove("DroneUUID");
                stack.setTag(tag);
                player.displayClientMessage(Component.literal("§cСвязь с дроном сброшена."), true);
                return InteractionResultHolder.success(stack);
            } else {
                return InteractionResultHolder.fail(stack);
            }
        }

        if (!level.isClientSide) {
            // На сервере отправляем пакет восстановления связи
            UUID targetUUID = getLinkedDroneUUID(stack);
            if (targetUUID != null) {
                PacketSystem.CHANNEL.sendToServer(new DroneReconnectPacket(targetUUID));
            }
            return InteractionResultHolder.success(stack);
        }

        // На клиенте ищем дрона и переключаем камеру
        UUID targetUUID = getLinkedDroneUUID(stack);
        DroneEntity drone = findDroneByUUID(player, targetUUID);

        if (drone != null) {
            Minecraft.getInstance().setCameraEntity(drone);
            return InteractionResultHolder.success(stack);
        } else {
            player.displayClientMessage(Component.literal("§cДрон не найден."), true);
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