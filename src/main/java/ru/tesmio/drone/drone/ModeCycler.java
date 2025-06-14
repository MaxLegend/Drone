package ru.tesmio.drone.drone;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class ModeCycler {
    /**
     * Условие доступности режима.
     */
    public record ModeCondition<T extends Enum<T>>(Item item, int slotIndex, T mode) {}

    private final List<ItemStack> inventory;
    private final boolean modifierKeyDown;

    /**
     * @param inventory Список предметов из инвентаря (например, entity.getInventory().getItems())
     * @param modifierKeyDown Нажата ли клавиша модификатора (например, Shift)
     */
    public ModeCycler(List<ItemStack> inventory, boolean modifierKeyDown) {
        this.inventory = inventory;
        this.modifierKeyDown = modifierKeyDown;
    }

    /**
     * Проверка, находится ли нужный предмет в конкретном слоте.
     */
    private boolean hasItemInSlot(Item item, int slotIndex) {
        if (slotIndex >= 0 && slotIndex < inventory.size()) {
            return inventory.get(slotIndex).getItem() == item;
        }
        return false;
    }

    /**
     * Получение списка доступных режимов с учетом условий.
     *
     * @param enumClass   Класс Enum (например, FlightMode.class)
     * @param conditions  Список условий доступности для режимов
     * @return Список доступных режимов
     */
    public <T extends Enum<T>> List<T> getAvailableModes(Class<T> enumClass, List<ModeCondition<T>> conditions) {
        List<T> available = new ArrayList<>();
        Set<T> requiredModes = new HashSet<>();

        // Собираем все режимы, для которых есть условия
        for (ModeCondition<T> cond : conditions) {
            requiredModes.add(cond.mode());
            if (hasItemInSlot(cond.item(), cond.slotIndex())) {
                available.add(cond.mode());
            }
        }

        // Добавляем все остальные режимы (без условий)
        for (T mode : enumClass.getEnumConstants()) {
            if (!requiredModes.contains(mode)) {
                available.add(mode);
            }
        }

        return available;
    }

    /**
     * Переключение режима среди доступных значений.
     *
     * @param getter         Функция получения текущего режима
     * @param setter         Функция установки нового режима
     * @param availableModes Список доступных режимов (от getAvailableModes)
     */
    public <T extends Enum<T>> void cycleMode(
            Supplier<T> getter,
            Consumer<T> setter,
            List<T> availableModes
    ) {
        T current = getter.get();
        int index = availableModes.indexOf(current);

        if (index == -1) {
            setter.accept(availableModes.get(0));
            return;
        }

        int nextIndex = modifierKeyDown
                ? (index - 1 + availableModes.size()) % availableModes.size()
                : (index + 1) % availableModes.size();

        setter.accept(availableModes.get(nextIndex));
    }
}
