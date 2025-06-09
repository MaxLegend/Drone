package ru.tesmio.drone.droneold;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import ru.tesmio.drone.drone.DroneEntity;

public class DroneHUD {
    private static final Minecraft mc = Minecraft.getInstance();

    public static void render(GuiGraphics guiGraphics, float partialTicks) {
        if (!(mc.getCameraEntity() instanceof DroneEntity drone)) return;

        int width = mc.getWindow().getGuiScaledWidth();
        int height = mc.getWindow().getGuiScaledHeight();
        PoseStack poseStack = guiGraphics.pose();

        // Получаем необходимые данные
        Vec3 velocity = drone.getDeltaMovement();
        float speed = (float) velocity.length() * 20; // Преобразуем в блоки/сек
        String direction = getCardinalDirection(drone.getYRot());
        BlockPos pos = drone.blockPosition();
        String coordinates = String.format("X: %d Y: %d Z: %d", pos.getX(), pos.getY(), pos.getZ());

        // Рассчитываем уровень сигнала (расстояние до игрока)
        float signalStrength = calculateSignalStrength(drone);

        // Рассчитываем высоту до земли
        float heightAboveGround = calculateHeightAboveGround(drone);
        String uuidclient = drone.getControllerUUID() == null ? "🟥 DISCONNECTED" : "🟩 CONNECTED";
        // Начинаем отрисовку
        poseStack.pushPose();

        // Фон для информации
        guiGraphics.fill(width - 150, 10, width - 10, 100, 0x80000000);
//        guiGraphics.drawString(mc.font, uuidclient,
//                width - 140, 5, 0xFFFFFF);

        // Скорость
        guiGraphics.drawString(mc.font, Component.literal("Скорость: " + String.format("%.1f", speed) + " м/с"),
                width - 140, 20, 0xFFFFFF);

        // Направление
        guiGraphics.drawString(mc.font, Component.literal("Направление: " + direction),
                width - 140, 35, 0xFFFFFF);

        // Координаты
        guiGraphics.drawString(mc.font, Component.literal(coordinates),
                width - 140, 50, 0xFFFFFF);

        // Уровень сигнала (расстояние до игрока)
        guiGraphics.drawString(mc.font, Component.literal("Сигнал: " + String.format("%.0f", signalStrength) + "%"),
                width - 140, 65, getSignalColor(signalStrength));

        // Высота над землей
        guiGraphics.drawString(mc.font, Component.literal("Высота: " + String.format("%.1f", heightAboveGround) + " м"),
                width - 140, 80, 0xFFFFFF);

        poseStack.popPose();
    }

    private static String getCardinalDirection(float yaw) {
        // Нормализуем угол от 0 до 360
        yaw = (yaw % 360 + 360) % 360;

        // Определяем направление
        if (yaw >= 337.5 || yaw < 22.5) return "Юг";
        else if (yaw >= 22.5 && yaw < 67.5) return "Юго-запад";
        else if (yaw >= 67.5 && yaw < 112.5) return "Запад";
        else if (yaw >= 112.5 && yaw < 157.5) return "Северо-запад";
        else if (yaw >= 157.5 && yaw < 202.5) return "Север";
        else if (yaw >= 202.5 && yaw < 247.5) return "Северо-восток";
        else if (yaw >= 247.5 && yaw < 292.5) return "Восток";
        else return "Юго-восток";
    }

    private static float calculateSignalStrength(DroneEntity drone) {
       // System.out.println("player " + drone.getControllingPlayerUUID());
        if (drone.getControllerUUID() == null) return 0;

        Player player = drone.level().getPlayerByUUID(drone.getControllerUUID());

        if (player == null) return 0;
        // System.out.println("player " + drone.getControllingPlayerUUID());
        float maxDist = Math.min(drone.getSyncView(), drone.getSyncSimDist());
       // double maxDist = (minChunks - 2) * 16.0;
        double maxDistSq = maxDist * maxDist;
        double distance = player.distanceTo(drone);
        double maxDistance = maxDistSq; // Максимальное расстояние управления
        //  System.out.println("player " +drone.getSyncView());
        double signal = 100 * (1 - distance / maxDistSq);

        return (float) Mth.clamp(signal, 0, 100);
    }

    private static float calculateHeightAboveGround(DroneEntity drone) {
        // Начинаем проверку от позиции дрона вниз
        for (int y = (int) drone.getY(); y > 0; y--) {
            BlockPos pos = new BlockPos((int) drone.getX(), y, (int) drone.getZ());
            if (!drone.level().isEmptyBlock(pos)) {
                return (float) (drone.getY() - y - 1); // -1 чтобы учесть что блок начинается с целого Y
            }
        }
        return (float) drone.getY(); // Если не нашли блоков, возвращаем абсолютную высоту
    }

    private static int getSignalColor(float strength) {
        if (strength > 75) return 0x00FF00; // Зеленый
        else if (strength > 50) return 0xFFFF00; // Желтый
        else if (strength > 25) return 0xFFA500; // Оранжевый
        else return 0xFF0000; // Красный
    }
}
