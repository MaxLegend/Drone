package ru.tesmio.drone.drone.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import ru.tesmio.drone.Core;
import ru.tesmio.drone.drone.DroneEntity;

public class DroneHUD {
  //  private static final ResourceLocation FRAME = new ResourceLocation(Core.MODID, "textures/gui/hud_frame.png");
    private static final ResourceLocation SIGNAL_BARS = new ResourceLocation(Core.MODID, "textures/gui/signal_bars.png");
    static Minecraft mc = Minecraft.getInstance();
    public static void renderDroneHud(GuiGraphics guiGraphics) {
        if (!(mc.getCameraEntity() instanceof DroneEntity drone)) return;
        if (drone.getControllerUUID() == null) return;
        PoseStack poseStack = guiGraphics.pose();
        Player player = drone.level().getPlayerByUUID(drone.getControllerUUID());
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();

        String direction = getCardinalDirection(drone.getYRot());
        poseStack.pushPose();
        poseStack.scale(1.1f, 1.1f, 1.0f);
        // === 2. Режимы полёта (слева сверху) ===
        int textColor = 0xFFFFFF;
        guiGraphics.drawString(mc.font, Component.literal("Mode: " + drone.getFlightMode().name()), 10, 10, textColor);
        guiGraphics.drawString(mc.font, Component.literal("Stab: " + drone.getStabMode().name()), 130, 10, textColor);

        // === 3. Индикатор сигнала (справа верх) ===
        int barX = screenWidth - 20;
        int barY = 20;
        int barWidth = 10;
        int barHeight = 50;
        float signalStrength = calculateSignalStrength(drone);// 0–100
        int filledHeight = (int)(barHeight * signalStrength / 100f);
        guiGraphics.blit(SIGNAL_BARS, barX, barY + (barHeight - filledHeight), 0, (barHeight - filledHeight), barWidth, filledHeight, barWidth, barHeight);

        // === 4. GPS и ориентация (справа верх) ===
        BlockPos pos = drone.blockPosition();
        int infoX = screenWidth - 150;
        int rightMargin = 60;
        int infoY = 20;
        guiGraphics.drawString(mc.font, Component.literal(   "GPS: " + pos.getX() +" | "+  pos.getY() +" | "+  pos.getZ() ), infoX, infoY - 10, textColor);
        drawRightAlignedText(guiGraphics, String.format("%.1f° :Y", (drone.getDroneYaw() % 360 + 360) % 360),
                screenWidth - rightMargin, infoY + 10, textColor);
        drawRightAlignedText(guiGraphics, String.format("%.1f° :P", Mth.abs(drone.getDronePitch())),
                screenWidth - rightMargin, infoY + 23, textColor);
        drawRightAlignedText(guiGraphics, String.format("%.1f° :R", drone.getDroneRoll()),
                screenWidth - rightMargin, infoY + 36, textColor);
    //    guiGraphics.drawString(mc.font, Component.literal(String.format("P: %.1f", drone.getDronePitch())), infoX, infoY + 20, textColor);
   //     guiGraphics.drawString(mc.font, Component.literal(String.format("R: %.1f", drone.getDroneRoll())), infoX, infoY + 30, textColor);    // === 5. Горизонтальная и вертикальная скорость ===
        int centerX = screenWidth / 2;
        int y = screenHeight - 40;

        Vec3 vel = drone.getDeltaMovement();
        float horizSpeed = (float) Math.sqrt(vel.x * vel.x + vel.z * vel.z) * 20f;
        float vertSpeed = (float) vel.y * 20f;
        double altitude = drone.getY() - drone.level().getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, drone.blockPosition()).getY();
        double distance = drone.distanceTo(player);


        Font font = mc.font;

        guiGraphics.drawString(font, Component.literal(String.format("H.S: %.1f m/s", horizSpeed)), centerX -220, y, textColor);
        guiGraphics.drawString(font, Component.literal(String.format("V.S: %.1f m/s", vertSpeed)), centerX -140, y, textColor);
        guiGraphics.drawString(font, Component.literal(String.format("H: %.1f m", altitude)), centerX +5, y, textColor);
        guiGraphics.drawString(font, Component.literal(String.format("D: %.1f m", distance)), centerX + 60, y, textColor);
        guiGraphics.drawString(font, Component.literal("D.V: " + direction), centerX + 120, y, textColor);

        poseStack.popPose();
    }

    private static void drawRightAlignedText(GuiGraphics guiGraphics, String text, int rightEdgeX, int y, int color) {
        int textWidth = mc.font.width(text);
        guiGraphics.drawString(mc.font, Component.literal(text), rightEdgeX - textWidth, y, color);
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
    private static String getCardinalDirection(float yaw) {
        // Нормализуем угол от 0 до 360
        yaw = (yaw % 360 + 360) % 360;

        // Определяем направление
        if (yaw >= 337.5 || yaw < 22.5) return "S";
        else if (yaw >= 22.5 && yaw < 67.5) return "SW";
        else if (yaw >= 67.5 && yaw < 112.5) return "W";
        else if (yaw >= 112.5 && yaw < 157.5) return "NW";
        else if (yaw >= 157.5 && yaw < 202.5) return "N";
        else if (yaw >= 202.5 && yaw < 247.5) return "NE";
        else if (yaw >= 247.5 && yaw < 292.5) return "E";
        else return "SE";
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
    private static Vec2 rotate(float x, float y, float cx, float cy, float sin, float cos) {
        float dx = x - cx;
        float dy = y - cy;
        return new Vec2(
                cx + dx * cos - dy * sin,
                cy + dx * sin + dy * cos
        );
    }
}

