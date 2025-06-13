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
import ru.tesmio.drone.registry.InitMenus;

public class DroneEntityMenu extends AbstractContainerMenu {
    private final ContainerEntity drone;

    public DroneEntityMenu(int id, Inventory playerInventory, ContainerEntity drone) {
        super(InitMenus.DRONE_ENTITY_MENU.get(), id);
        this.drone = drone;

        // Слоты дрона
        for (int i = 0; i < drone.getContainerSize(); i++) {
            this.addSlot(new Slot((Container) drone, i, 44 + i * 18, 20));
        }

        // Инвентарь игрока
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 51 + row * 18));
            }
        }

        // Хотбар
        for (int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 109));
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int i) {
        return ItemStack.EMPTY;
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
