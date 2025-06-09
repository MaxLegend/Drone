package ru.tesmio.drone.droneold;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

import ru.tesmio.drone.drone.DroneEntity;
import ru.tesmio.drone.packets.PacketSystem;
import ru.tesmio.drone.packets.client.DroneMovePacket;

public class DroneController {
    private static float lastYaw = 0, lastPitch = 0;
    private static int tickCounter = 0;
    public static final KeyMapping EXIT_CONTROL_KEY = new KeyMapping(
            "key.drone.exit_drone", // ID (должен быть уникальным)
            KeyConflictContext.IN_GAME, // Контекст (например, только в игре)
            InputConstants.Type.KEYSYM, // Тип (клавиатура/мышь)
            GLFW.GLFW_KEY_R, // Клавиша по умолчанию (R)
            "key.category.drone" // Категория в настройках управления
    );
    public static final KeyMapping FLIGHT_MODE_KEY = new KeyMapping(
            "key.drone.flight_mode",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_Z,
            "key.categories.drone"
    );
    public static final KeyMapping STAB_MODE_KEY = new KeyMapping(
            "key.drone.stab_mode",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_C,
            "key.categories.drone"
    );
    private static float currentYaw = 0;
    private static float currentPitch = 0;
    private static float currentRoll = 0;
    public static boolean mouseGrabbed = false;
    private static Minecraft mc = Minecraft.getInstance();
    private static float targetRoll = 0;
    private static float rollVelocity = 0;
    private static final float MAX_ROLL_ANGLE = 30f;
    private static final float ROLL_SPEED = 0.15f;
    private static final float DAMPING = 0.4f;

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
                currentRoll = drone.getDroneRoll();
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
        float speed = drone.getSpeed();
        double rad = Math.toRadians(drone.getYRot());
        Vec3 forward = new Vec3(-Math.sin(rad), 0, Math.cos(rad));
        Vec3 right = forward.cross(new Vec3(0, 1, 0)).normalize();
        Vec3 up = new Vec3(0, 1, 0);

        if (mc.options.keyUp.isDown()) movement = movement.add(forward.scale(speed));
        if (mc.options.keyDown.isDown()) movement = movement.subtract(forward.scale(speed));
        if (mc.options.keyLeft.isDown()) movement = movement.subtract(right.scale(speed));
        if (mc.options.keyRight.isDown()) movement = movement.add(right.scale(speed));
        if (mc.options.keyJump.isDown()) movement = movement.add(up.scale(speed));
        if (mc.options.keyShift.isDown()) movement = movement.subtract(up.scale(speed));

        turnDrone();
        if(drone.getStabMode() == DroneEntity.StabMode.FPV) rollDrone(drone, movement, right);
        drone.applyClientMovement(movement, currentYaw, currentPitch, currentRoll);

        PacketSystem.CHANNEL.sendToServer(new DroneMovePacket(
                drone.getUUID(),
                movement,
                currentYaw, currentPitch, currentRoll
        ));
    }
    private static void rollDrone(DroneEntity drone, Vec3 movement, Vec3 right) {
        float lateralInput = (float)movement.dot(right);
        float newTargetRoll = -lateralInput * MAX_ROLL_ANGLE;
        targetRoll = Mth.lerp(ROLL_SPEED, targetRoll, newTargetRoll);
        float rollAcceleration = (targetRoll - currentRoll) * ROLL_SPEED;
        rollVelocity = rollVelocity * (1 - DAMPING) + rollAcceleration;
        currentRoll += rollVelocity;
        currentRoll = Mth.clamp(currentRoll, -MAX_ROLL_ANGLE, MAX_ROLL_ANGLE);
        drone.setDroneRoll(currentRoll);
    }
    public static void turnDrone() {
            float yawVelocity = (float) mc.mouseHandler.getXVelocity();
            float pitchVelocity = (float) mc.mouseHandler.getYVelocity();
            if (mc.options.invertYMouse().get()) {
                pitchVelocity = -pitchVelocity;
            }
            double sensitivity = mc.options.sensitivity().get();
            double scale = Math.pow(sensitivity * 0.6 + 0.2, 3);
            currentYaw = (float) (yawVelocity * scale);
            currentPitch = (float) (pitchVelocity * scale);
            currentPitch = Mth.clamp(currentPitch, -45.0f, 90.0f);

        }

}
