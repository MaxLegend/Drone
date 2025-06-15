package ru.tesmio.drone.drone.quadcopter.container;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.ContainerEntity;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import ru.tesmio.drone.drone.quadcopter.DroneEntity;
import ru.tesmio.drone.registry.InitMenus;

public class DroneEntityMenu extends AbstractContainerMenu {
    private final Container container;
    public DroneEntityMenu(int containerId, Inventory playerInventory, DroneEntity droneEntity) {
        super(InitMenus.DRONE_ENTITY_MENU.get(), containerId);
        this.container = droneEntity.getContainer();

        this.addSlot(new UpgradeSlot(container, 0, 28, 14));  // TI Controller
        this.addSlot(new UpgradeSlot(container, 1, 71, 4));   // ir controller
        this.addSlot(new UpgradeSlot(container, 2, 118, 20)); // gps controller
//
        this.addSlot(new UpgradeSlot(container, 3, 16, 46));  // zoom1
        this.addSlot(new UpgradeSlot(container, 4, 74, 42));  //fly controller
        this.addSlot(new UpgradeSlot(container, 5, 117, 66)); // stab chip
//
        this.addSlot(new UpgradeSlot(container, 6, 141, 66));  // manual chip
        this.addSlot(new UpgradeSlot(container, 7, 139, 12));  // speed 1
//
        this.addSlot(new UpgradeSlot(container, 8, 39, 74));  // zoom2
        this.addSlot(new UpgradeSlot(container, 9, 154, 10));  // Нижний центр

        // Инвентарь игрока (сдвинут вниз)
        int playerInvY = 97;
        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 9; ++j) {
                this.addSlot(new Slot(playerInventory, j + i * 9 + 9, 8 + j * 18, playerInvY + i * 18));
            }
        }

        // Хотбар игрока
        for (int i = 0; i < 9; ++i) {
            this.addSlot(new Slot(playerInventory, i, 8 + i * 18, playerInvY + 58));
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack itemstack1 = slot.getItem();
            itemstack = itemstack1.copy();
            if (index < 10) {
                if (!this.moveItemStackTo(itemstack1, 10, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(itemstack1, 0, 10, false)) {
                return ItemStack.EMPTY;
            }

            if (itemstack1.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }
        return itemstack;
    }

    @Override
    public boolean stillValid(Player player) {
        return container.stillValid(player);
    }
}
