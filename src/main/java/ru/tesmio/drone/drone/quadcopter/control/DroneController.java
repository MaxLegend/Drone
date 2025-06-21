package ru.tesmio.drone.drone.quadcopter.control;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

import ru.tesmio.drone.drone.quadcopter.DroneEntity;
import ru.tesmio.drone.packets.PacketSystem;
import ru.tesmio.drone.packets.both.DroneUpdateTilts;
import ru.tesmio.drone.packets.server.*;

//TODO: Чистка кода, стилизация, комментирование
@OnlyIn(Dist.CLIENT)
public class DroneController {

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
    public static final KeyMapping ZOOM_MODE_KEY = new KeyMapping(
            "key.drone.zoom_mode",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_X,
            "key.categories.drone"
    );
    public static final KeyMapping CTRL_KEY = new KeyMapping(
            "key.drone.ctrl_mode",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_LEFT_CONTROL,
            "key.categories.drone"
    );
    public static final KeyMapping VISION_MODE_KEY = new KeyMapping(
            "key.drone.vision_mode",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_V,
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

    public static void freezePlayer(Player player) {
        if (player != null) {
            player.setDeltaMovement(Vec3.ZERO);
            player.zza = 0;
            player.xxa = 0;
            player.yya = 0;
            player.setShiftKeyDown(false);
            player.setSprinting(false);
        }
    }
    public static void stopControl() {
        if (EXIT_CONTROL_KEY.consumeClick()) {
            mc.setCameraEntity(mc.player);
            if (mouseGrabbed) {
                mc.mouseHandler.releaseMouse();
                mouseGrabbed = false;
            }
        }
    }

    public static void useKey() {
        if (FLIGHT_MODE_KEY.consumeClick()) {
            if(mc.getCameraEntity() instanceof DroneEntity drone) {
                drone.cycleFlightMode();
            }
        }
        if (STAB_MODE_KEY.consumeClick()) {
            if(mc.getCameraEntity() instanceof DroneEntity drone) {
                drone.cycleStabMode();
            }
        }
        if (ZOOM_MODE_KEY.consumeClick()) {
            if(mc.getCameraEntity() instanceof DroneEntity drone) {
                drone.cycleZoomMode();
            }
        }
        if (VISION_MODE_KEY.consumeClick()) {
            if(mc.getCameraEntity() instanceof DroneEntity drone) {
                    drone.cycleVisionMode();
            }
        }
    }
    public static void tiltDrone(DroneEntity drone) {
        float maxTilt = 0.38f;

        float forwardInput = 0;
        float sidewaysInput = 0;

        if (mc.options.keyUp.isDown()) forwardInput -= 1;
        if (mc.options.keyDown.isDown()) forwardInput += 1;
        if (mc.options.keyLeft.isDown()) sidewaysInput += 1;
        if (mc.options.keyRight.isDown()) sidewaysInput -= 1;

        float length = Mth.sqrt(forwardInput * forwardInput + sidewaysInput * sidewaysInput);
        if (length > 0) {
            forwardInput /= length;
            sidewaysInput /= length;
        }

        float combinedTilt = maxTilt * length;
        sidewaysInput = -sidewaysInput;

        float targetTiltX = forwardInput * combinedTilt;
        float targetTiltZ = sidewaysInput * combinedTilt;
        drone.updateTilt(targetTiltX,targetTiltZ);
        PacketSystem.CHANNEL.sendToServer(new DroneUpdateTilts(drone.getUUID(), targetTiltX, targetTiltZ));

    }
    public static void moveDrone(DroneEntity drone) {
        Vec3 movement = Vec3.ZERO;
        double targetSpeed = 0.0;
        double baseSpeed = drone.getSpeed();
        double acceleration = drone.acceleration;

        double yawRad = Math.toRadians(drone.getDroneYaw());
        double pitchRad = Math.toRadians(drone.getDronePitch());
        double rollRad = Math.toRadians(drone.getDroneRoll());

        Vec3 forward = new Vec3(-Math.sin(yawRad), 0, Math.cos(yawRad));
        Vec3 right = forward.cross(new Vec3(0, 1, 0)).normalize();
        Vec3 up = new Vec3(0, 1, 0);

        if (drone.getStabMode() == DroneEntity.StabMode.MANUAL) {
            forward = new Vec3(
                    -Math.sin(yawRad) * Math.cos(pitchRad),
                    -Math.sin(pitchRad),
                    Math.cos(yawRad) * Math.cos(pitchRad)
            ).normalize();

            Vec3 baseUp = new Vec3(-Math.sin(pitchRad) * Math.sin(yawRad), Math.cos(pitchRad),
                    -Math.sin(pitchRad) * Math.cos(yawRad)).normalize();

            right = baseUp.cross(forward).normalize();
            up = forward.cross(right).normalize();

            double cosRoll = Math.cos(rollRad);
            double sinRoll = Math.sin(rollRad);

            Vec3 rotatedRight = right.scale(cosRoll).add(up.scale(sinRoll)).normalize();
            Vec3 rotatedUp = up.scale(cosRoll).subtract(right.scale(sinRoll)).normalize();

            right = rotatedRight;
            up = rotatedUp;
        }

        // Проверяем направление движения
        boolean hasMovement = false;
        Vec3 desiredMovement = Vec3.ZERO;

        if (mc.options.keyUp.isDown()) {
            desiredMovement = desiredMovement.add(forward);
            hasMovement = true;
        }
        if (mc.options.keyDown.isDown()) {
            desiredMovement = desiredMovement.subtract(forward);
            hasMovement = true;
        }
        if (mc.options.keyLeft.isDown()) {
            desiredMovement = desiredMovement.subtract(right);
            hasMovement = true;
        }
        if (mc.options.keyRight.isDown()) {
            desiredMovement = desiredMovement.add(right);
            hasMovement = true;
        }
        if (mc.options.keyJump.isDown()) {
            desiredMovement = desiredMovement.add(up);
            hasMovement = true;
        }
        if (mc.options.keyShift.isDown()) {
            desiredMovement = desiredMovement.subtract(up);
            hasMovement = true;
        }

        // Нормализуем направление, если есть движение
        if (hasMovement) {
            desiredMovement = desiredMovement.normalize();
            targetSpeed = baseSpeed;
        } else {
            targetSpeed = 0.0;
        }

        // Плавное изменение скорости: currentSpeed стремится к targetSpeed
        double currentSpeed = drone.currentSpeed;
        if(currentSpeed < 0.1f) {
            currentSpeed = 0.1f;
        }
        if (currentSpeed < targetSpeed) {
            currentSpeed = Math.min(currentSpeed + acceleration, targetSpeed);
        } else if (currentSpeed > targetSpeed) {
            currentSpeed = Math.max(currentSpeed - acceleration, targetSpeed);
        }
        drone.currentSpeed = currentSpeed;

        // Итоговый вектор движения
        movement = desiredMovement.scale(currentSpeed);

        if (drone.getStabMode() == DroneEntity.StabMode.FPV) {
            rollDrone(drone, movement, right, 30);
        }
        if (drone.getStabMode() == DroneEntity.StabMode.MANUAL) {
            rollDrone(drone, movement, right, 45);
        }

        drone.applyMovement(movement);
        tiltDrone(drone);

        PacketSystem.CHANNEL.sendToServer(new DroneMovePacket(drone.getUUID(), movement));

    }
    public static void rollDrone(DroneEntity drone, Vec3 movement, Vec3 right, float maxRollAngle) {
        float lateralInput = (float)movement.dot(right);
        float newTargetRoll = -lateralInput * maxRollAngle;
        targetRoll = Mth.lerp(ROLL_SPEED, targetRoll, newTargetRoll);
        float rollAcceleration = (targetRoll - currentRoll) * ROLL_SPEED;
        rollVelocity = rollVelocity * (1 - DAMPING) + rollAcceleration;
        currentRoll += rollVelocity;
        currentRoll = Mth.clamp(currentRoll, -maxRollAngle, maxRollAngle);

        drone.setDroneRoll(currentRoll);
    }
    public static void turnDrone(DroneEntity drone) {
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

            drone.applyView(currentYaw, currentPitch, currentRoll);

        }

}
