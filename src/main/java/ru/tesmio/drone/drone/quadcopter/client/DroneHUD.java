package ru.tesmio.drone.drone.quadcopter.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import ru.tesmio.drone.Dronecraft;
import ru.tesmio.drone.drone.quadcopter.DroneEntity;
import ru.tesmio.drone.registry.InitItems;

//TODO:
public class DroneHUD {
    private static final ResourceLocation SIGNAL_LEVEL = new ResourceLocation(Dronecraft.MODID, "textures/gui/signal_bars.png");
    private static final ResourceLocation SIGNAL_ICON = new ResourceLocation(Dronecraft.MODID, "textures/gui/signal_icon.png");
    private static final ResourceLocation COMPASS = new ResourceLocation(Dronecraft.MODID, "textures/gui/compass.png");
    private static final int COMPASS_SIZE = 64;
    static Minecraft mc = Minecraft.getInstance();
    static Font font = mc.font;
    static int screenWidth,screenHeight;
    public static void renderDroneHud(GuiGraphics guiGraphics) {

        if (!(mc.getCameraEntity() instanceof DroneEntity drone)) return;

        if (!mc.player.getUUID().equals(drone.getControllerUUID())) return;

        boolean needFlyController = drone.validateUpdates(InitItems.FLY_CONTROLLER.get(), 4);
        boolean needGPSController = drone.validateUpdates(InitItems.GPS_CONTROLLER.get(), 2);
        PoseStack poseStack = guiGraphics.pose();
        Player player = drone.level().getPlayerByUUID(drone.getControllerUUID());
        screenWidth = mc.getWindow().getGuiScaledWidth();
        screenHeight = mc.getWindow().getGuiScaledHeight();
        renderSignal(guiGraphics, screenWidth, drone);
        poseStack.pushPose();
        poseStack.scale(1.1f, 1.1f, 1.0f);
        if(needFlyController) {
            guiGraphics.drawString(mc.font, drone.getFlightMode().getName(), 10, 10, 0xFFFFFF);
            guiGraphics.drawString(mc.font, drone.getStabMode().getName(), 130, 10, 0xFFFFFF);
            guiGraphics.drawString(mc.font, drone.getZoomMode().getName(), 220, 10, 0xFFFFFF);
            guiGraphics.drawString(mc.font, drone.getVisionMode().getName(), 280, 10, 0xFFFFFF);


            drawRightAlignedText(guiGraphics, String.format("%.1f° :Y", (drone.getDroneYaw() % 360 + 360) % 360), screenWidth - 60, 30, 0xFFFFFF);
            drawRightAlignedText(guiGraphics, String.format("%.1f° :P", Mth.abs(drone.getDronePitch())), screenWidth - 60, 43, 0xFFFFFF);
            drawRightAlignedText(guiGraphics, String.format("%.1f° :R", Mth.abs(drone.getDroneRoll())), screenWidth - 60, 56, 0xFFFFFF);

            Vec3 vel = drone.getDeltaMovement();
            float horizSpeed = (float) Math.sqrt(vel.x * vel.x + vel.z * vel.z) * 20f;
            float vertSpeed = (float) vel.y * 20f;
            double altitude = Mth.abs((float) drone.getY()) - drone.level().getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, drone.blockPosition()).getY();
            double distance = drone.distanceTo(player);

            guiGraphics.drawString(font, Component.literal(String.format("H.S: %.1f m/s", horizSpeed)), (screenWidth / 2) - 140, screenHeight - 40, 0xFFFFFF);
            guiGraphics.drawString(font, Component.literal(String.format("V.S: %.1f m/s", Mth.abs(vertSpeed))), (screenWidth / 2) - 70, screenHeight - 40, 0xFFFFFF);
            guiGraphics.drawString(font, Component.literal(String.format("H: %.1f m", altitude)), (screenWidth / 2) + 5, screenHeight - 40, 0xFFFFFF);
            guiGraphics.drawString(font, Component.literal(String.format("D: %.1f m", distance)), (screenWidth / 2) + 60, screenHeight - 40, 0xFFFFFF);
        }
        poseStack.popPose();
        if(needGPSController) {
            BlockPos pos = drone.blockPosition();
            guiGraphics.drawString(mc.font, Component.literal(   "GPS: " + pos.getX() +" | "+  pos.getY() +" | "+  pos.getZ() ), screenWidth - 470, 27, 0xFFFFFF);
            renderDynamicCompassSelfRot(guiGraphics, drone.getDroneYaw(), (screenWidth / 2) + 171, screenHeight - 40 - 49, screenWidth);
            drawCompassBackground(guiGraphics, screenWidth - 59, 5);
        }
    }

    private static void renderSignal(GuiGraphics guiGraphics, int screenWidth, DroneEntity drone) {
        float exponent = 2.5f;
        float signalStrength = Mth.clamp(calculateSignalStrength(drone) / 100f, 0f, 1f);
        signalStrength = 1f - (float) Math.pow(1f - signalStrength, exponent);
        double distanceSq = drone.distanceToSqr(mc.player);
        boolean forceFullSignal = distanceSq <= 10 * 10;
        int rawFilledWidth = (int)(40 * signalStrength);
        int filledWidth = (rawFilledWidth / 8) * 8;

        if (forceFullSignal || signalStrength >= 0.999f) {
            filledWidth = 40;
        }

        if (filledWidth > 0) guiGraphics.blit(SIGNAL_LEVEL, screenWidth - 59, 5, 0, 0, filledWidth, 20, 40, 20);

        guiGraphics.blit(SIGNAL_ICON, screenWidth - 82, 5, 0, 0, 20, 20, 20, 20);
    }



    // Вспомогательный метод для рисования стрелки компаса
    private static void drawCompassBackground(GuiGraphics guiGraphics,int triX, int triY) {

        PoseStack poseStack = guiGraphics.pose();
        RenderSystem.enableBlend();  // ВКЛЮЧАЕМ СМЕШИВАНИЕ
        RenderSystem.defaultBlendFunc();  // Стандартный режим смешивания (src alpha, 1-src alpha)
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 0.7f);
        poseStack.pushPose();
        poseStack.scale(0.51f, 0.51f, 0f);
        poseStack.translate(354f, 330f, 0f);
        guiGraphics.blit(COMPASS, triX, triY, 0, 0, 128, 128, 128, 128);
        poseStack.popPose();
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.disableBlend();

    }
    private static void renderDynamicCompassSelfRot(GuiGraphics guiGraphics, float yaw, int x, int y, int screenWidth) {
        PoseStack poseStack = guiGraphics.pose();
        poseStack.pushPose();
        poseStack.translate((x + COMPASS_SIZE/2f)-15, (y + COMPASS_SIZE/2f)+10, 0);
        String[] directions = {"N", "NE", "E", "SE", "S", "SW", "W", "NW"};
        int[] colors = {0x00AAFF, 0x000000, 0x000000, 0x000000,
                0xFF0000, 0x000000, 0x000000, 0x000000};
        float radius = 25f; // Радиус круга
        float scale = 0.9f; // Масштаб текста

        // Плавное вращение (можно добавить интерполяцию для анимации)
        float smoothYaw = yaw; // Здесь можно добавить плавную интерполяцию

        // Рисуем направления
        for (int i = 0; i < directions.length; i++) {
            float angle = i * 45f - smoothYaw;
            float rad = (float)Math.toRadians(angle);

            // Плавное изменение размера в зависимости от положения
            float distanceFactor = 1.0f - Math.abs(angle % 90 - 45) / 45f;
            float currentScale = scale * (0.7f + 0.3f * distanceFactor);

            // Позиция с учетом перспективы
            float dx = (float)Math.sin(rad) * radius;
            float dy = (float)Math.cos(rad) * radius;

            // Эффект "выдвижения" ближайших элементов
            float zOffset = 1.0f + 0.5f * distanceFactor;

            poseStack.pushPose();
            poseStack.translate(dx, -dy, 0);

            String dir = directions[i];
            int textWidth = mc.font.width(dir);
            guiGraphics.drawString(mc.font, Component.literal(dir),
                    -textWidth/2, -4, colors[i], true);
            poseStack.popPose();



        }


        poseStack.popPose();
    }
    private static void drawCircle(PoseStack poseStack, int centerX, int centerY, int radius, int argbColor) {
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.getBuilder();

        float a = ((argbColor >> 24) & 0xFF) / 255.0f;
        float r = ((argbColor >> 16) & 0xFF) / 255.0f;
        float g = ((argbColor >> 8) & 0xFF) / 255.0f;
        float b = (argbColor & 0xFF) / 255.0f;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();


        buffer.begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
        Matrix4f matrix = poseStack.last().pose();

        // Центральная точка
        buffer.vertex(matrix, centerX, centerY, 0).color(r, g, b, a).endVertex();

        int segments = 64;
        for (int i = 0; i <= segments; i++) {
            double angle = 2 * Math.PI * i / segments;
            float x = centerX + (float) (Math.cos(angle) * radius);
            float y = centerY + (float) (Math.sin(angle) * radius);
            buffer.vertex(matrix, x, y, 0).color(r, g, b, a).endVertex();
        }

        tesselator.end();


        RenderSystem.disableBlend();
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

        if (yaw >= 337.5 || yaw < 22.5) return "§bS";
        else if (yaw >= 22.5 && yaw < 67.5) return "§0SW";
        else if (yaw >= 67.5 && yaw < 112.5) return "§0W";
        else if (yaw >= 112.5 && yaw < 157.5) return "§0NW";
        else if (yaw >= 157.5 && yaw < 202.5) return "§cN";
        else if (yaw >= 202.5 && yaw < 247.5) return "§0NE";
        else if (yaw >= 247.5 && yaw < 292.5) return "§0E";
        else return "§0SE";
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

