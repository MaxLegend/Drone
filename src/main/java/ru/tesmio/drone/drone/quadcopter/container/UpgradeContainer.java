package ru.tesmio.drone.drone.quadcopter.container;

import net.minecraft.core.NonNullList;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import ru.tesmio.drone.registry.InitItems;

public class UpgradeContainer extends SimpleContainer {

    public UpgradeContainer() {
        super(10);
    }


    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (slot == 0 ) return stack.getItem() == InitItems.TI_CONTROLLER.get();
        if (slot == 1 ) return stack.getItem() == InitItems.IR_CONTROLLER.get();
        if (slot == 2 ) return stack.getItem() == InitItems.GPS_CONTROLLER.get();
        if (slot == 3 ) return stack.getItem() == InitItems.ZOOM2.get();
        if (slot == 4 ) return stack.getItem() == InitItems.FLY_CONTROLLER.get();
        if (slot == 5 ) return stack.getItem() == InitItems.STAB_CHIP.get();
        if (slot == 6 ) return stack.getItem() == InitItems.MANUAL_CHIP.get();
        if (slot == 7 ) return stack.getItem() == InitItems.SPEED_CHIP.get();
        if (slot == 8 ) return stack.getItem() == InitItems.ZOOM1.get();
        if (slot == 9 ) return stack.getItem() == InitItems.SPEED_CHIP2.get();
        return true;
    }
}
