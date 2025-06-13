package ru.tesmio.drone.drone.quadcopter.container;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.ContainerEntity;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;
import net.minecraftforge.items.wrapper.InvWrapper;
import ru.tesmio.drone.drone.quadcopter.DroneEntity;
import ru.tesmio.drone.registry.InitMenus;

public class DroneEntityMenu extends AbstractContainerMenu {
    private final DroneEntity drone;
    private final Player player;
    private final IItemHandler playerInventory;

    public DroneEntityMenu(int id, Inventory playerInventory, DroneEntity drone) {
        super(InitMenus.DRONE_ENTITY_MENU.get(), id);
        this.drone = drone;
        this.player = playerInventory.player;
        this.playerInventory = new InvWrapper(playerInventory);
        // Слоты дрона
        for (int i = 0; i < drone.getContainerSize(); i++) {
            this.addSlot(new Slot((Container) drone, i, 44 + i * 18, 20));
        }

    }
    // Добавление слотов игрока
    private void addPlayerSlots(Inventory playerInventory) {
        final int PLAYER_INVENTORY_Y_OFFSET = 140; // Смещение для инвентаря игрока
        final int HOTBAR_Y_OFFSET = 198; // Смещение для хотбара
        // Основные слоты (27 слотов)
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                int slotIndex = col + row * 9 + 9;
                int xPos = 8 + col * 18;
                int yPos = PLAYER_INVENTORY_Y_OFFSET + row * 18;
                this.addSlot(new SlotItemHandler(this.playerInventory, slotIndex, xPos, yPos));
            }
        }

        // Хотбар (9 слотов)
        for (int col = 0; col < 9; ++col) {
            int xPos = 8 + col * 18;
            int yPos = HOTBAR_Y_OFFSET;
            this.addSlot(new SlotItemHandler(this.playerInventory, col, xPos, yPos));
        }
    }

    // Перенос предметов по Shift+Клик
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            itemstack = stack.copy();

            // Если клик по сундуку
            if (index < 27) {
                if (!this.moveItemStackTo(stack, 27, 63, true)) {
                    return ItemStack.EMPTY;
                }
            }
            // Если клик по инвентарю игрока
            else if (!this.moveItemStackTo(stack, 0, 27, false)) {
                return ItemStack.EMPTY;
            }

            if (stack.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }

        return itemstack;
    }

    @Override
    public boolean stillValid(Player player) {
        return drone.isChestVehicleStillValid(player);
    }
    private static ContainerEntity getDroneEntity(Inventory playerInv, FriendlyByteBuf buf) {
        int entityId = buf.readVarInt();
        Entity entity = playerInv.player.level().getEntity(entityId);
        if (!(entity instanceof ContainerEntity containerEntity)) {
            throw new IllegalArgumentException("Invalid drone entity ID: " + entityId);
        }
        return containerEntity;
    }

}
