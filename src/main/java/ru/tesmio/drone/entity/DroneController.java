package ru.tesmio.drone.entity;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;
import ru.tesmio.drone.Core;
import ru.tesmio.drone.packets.DroneMovePacket;

public class DroneController {
    public static final KeyMapping EXIT_CONTROL_KEY = new KeyMapping(
            "key.drone.exit_drone", // ID (должен быть уникальным)
            KeyConflictContext.IN_GAME, // Контекст (например, только в игре)
            InputConstants.Type.KEYSYM, // Тип (клавиатура/мышь)
            GLFW.GLFW_KEY_R, // Клавиша по умолчанию (R)
            "key.category.drone" // Категория в настройках управления
    );
    private static float currentYaw = 0;
    private static float currentPitch = 0;
    private static boolean mouseGrabbed = false;
    private static Minecraft mc = Minecraft.getInstance();

    public static void stopPlayer(Player player) {
        if (player != null) {
            player.setDeltaMovement(Vec3.ZERO);
            player.zza = 0;
            player.xxa = 0;
            player.yya = 0;
            if (mc.getCameraEntity() instanceof DroneEntity) {
                player.setShiftKeyDown(false);
                player.setSprinting(false);
            }
        }
    }
    public static void exitControl() {
        if (EXIT_CONTROL_KEY.consumeClick()) {
            mc.setCameraEntity(mc.player);
            if (mc.player != null) {
                mc.player.setInvisible(false);
                mc.player.setInvulnerable(false);
                mc.player.setNoGravity(false);
                mc.player.stopRiding();
            }
            if (mouseGrabbed) {
                mc.mouseHandler.releaseMouse();
                mouseGrabbed = false;
            }
        }
    }
    public static void mouseGrabber(boolean isGuiOpen) {
        if (mc.getCameraEntity() instanceof DroneEntity drone) {
            if (!mouseGrabbed && !isGuiOpen) {
                currentYaw = drone.getYRot();
                currentPitch = drone.getXRot();
                mc.mouseHandler.grabMouse();
                mouseGrabbed = true;
            }
        } else {
            if (mouseGrabbed) {
                mc.mouseHandler.releaseMouse();
                mouseGrabbed = false;
            }
        }
        if (isGuiOpen && mouseGrabbed) {
            mc.mouseHandler.releaseMouse();
            mouseGrabbed = false;
        }
        mc.getCameraEntity();
    }
    public static void moveDrone(DroneEntity drone) {
        Vec3 movement = Vec3.ZERO;
        // Перехват управления на дрон
        float speed = 0.2f;
        double rad = Math.toRadians(currentYaw);
        Vec3 forward = new Vec3(-Math.sin(rad), 0, Math.cos(rad));
        Vec3 right = forward.cross(new Vec3(0, 1, 0)).normalize();
        Vec3 up = new Vec3(0, 1, 0);
        if (mc.options.keyUp.isDown()) movement = movement.add(forward.scale(speed));
        if (mc.options.keyDown.isDown()) movement = movement.subtract(forward.scale(speed));
        if (mc.options.keyLeft.isDown()) movement = movement.subtract(right.scale(speed));
        if (mc.options.keyRight.isDown()) movement = movement.add(right.scale(speed));
        if (mc.options.keyJump.isDown()) movement = movement.add(up.scale(speed));
        if (mc.options.keyShift.isDown()) movement = movement.subtract(up.scale(speed));

        drone.applyClientMovement(movement, currentYaw, currentPitch);

        Core.CHANNEL.sendToServer(new DroneMovePacket(
                drone.getUUID(),
                movement,
                currentYaw, currentPitch
        ));
    }

    public static void turnDrone(boolean isGuiOpen, DroneEntity drone) {
        if (!isGuiOpen) {
            float yawVelocity = (float) mc.mouseHandler.getXVelocity();
            float pitchVelocity = (float) mc.mouseHandler.getYVelocity();

            if (mc.options.invertYMouse().get()) {
                pitchVelocity = -pitchVelocity;
            }

            double sensitivity = mc.options.sensitivity().get();
            double scale = Math.pow(sensitivity * 0.6 + 0.2, 3) * 2.0;

            // Инкрементальное изменение углов
            currentYaw = (float) (yawVelocity * scale);
            currentPitch = (float) (pitchVelocity * scale);

            // Ограничение pitch, но не yaw
            currentPitch = Mth.clamp(currentPitch, -45.0f, 90.0f);

            // yaw теперь может накапливаться бесконечно
            drone.setDroneYaw(currentYaw);
            drone.setDronePitch(currentPitch);
        }
    }
//    public static void turnDrone(boolean isGuiOpen, DroneEntity drone) {
//        if (!isGuiOpen) {
//
//            float yawVelocity = (float) mc.mouseHandler.getXVelocity();
//            float pitchVelocity = (float) mc.mouseHandler.getYVelocity();
//
//            if (mc.options.invertYMouse().get()) {
//                pitchVelocity = -pitchVelocity;
//            }
//
//            double sensitivity = mc.options.sensitivity().get();
//            double scale = Math.pow(sensitivity * 0.6 + 0.2, 3) * 2.0;
//
//            // Инкрементальное изменение углов
//            currentYaw = (float) (yawVelocity * scale);
//            currentPitch = (float) (pitchVelocity * scale);
//
//            // Ограничение угла pitch
//            currentPitch = Mth.clamp(currentPitch, -90.0f, 90.0f);
//
//            // Приведение yaw к диапазону [0, 360)
//            currentYaw = (currentYaw % 360 + 360) % 360;
//
//
//
//            drone.setDroneYaw(currentYaw);
//            drone.setDronePitch(currentPitch);
//        }
//    }
}
