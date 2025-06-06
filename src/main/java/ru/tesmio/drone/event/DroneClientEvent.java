package ru.tesmio.drone.event;

import com.mojang.blaze3d.Blaze3D;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.SmoothDouble;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.*;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;
import ru.tesmio.drone.Core;
import ru.tesmio.drone.entity.DroneEntity;
import ru.tesmio.drone.entity.DroneModel;
import ru.tesmio.drone.entity.DroneRenderer;
import ru.tesmio.drone.packets.DroneMovePacket;
import ru.tesmio.drone.packets.DroneSpeedUpdatePacket;

@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class DroneClientEvent {
    private static float currentYaw = 0;
    private static float currentPitch = 0;
    private static boolean mouseGrabbed = false;

    public static final KeyMapping EXIT_CONTROL_KEY = new KeyMapping(
            "key.drone.exit_control", GLFW.GLFW_KEY_R, "key.categories.drone"
    );


    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.getCameraEntity() instanceof DroneEntity) {
            event.setCanceled(true);
        }
    }

    static Minecraft mc = Minecraft.getInstance();
    static boolean guiOpen = mc.screen != null;

    @SubscribeEvent
    public static void onMouseScroll(InputEvent.MouseScrollingEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        if (mc.cameraEntity instanceof DroneEntity drone) {
            double delta = event.getScrollDelta();
            drone.adjustSpeed((float) delta * 0.1f); // масштаб изменения
            Core.CHANNEL.sendToServer(new DroneSpeedUpdatePacket(drone.getUUID(), drone.getSpeed()));
            event.setCanceled(true);
        }
    }
      @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        DroneEntity drone;
        if(mc.getCameraEntity() instanceof DroneEntity) drone = (DroneEntity) mc.getCameraEntity();
        else return;
        if (event.phase != TickEvent.Phase.END) return;
        // Управление захватом мыши в зависимости от состояния
        mouseGrabber();
        // Прекращаем движение игрока
        stopPlayer(mc.player);
        // Управление камерой
        if (!guiOpen) {
            // 1. Получаем мгновенные дельты мыши
            float deltaYaw = (float) mc.mouseHandler.getXVelocity();
            float deltaPitch = (float) mc.mouseHandler.getYVelocity();

            if (mc.options.invertYMouse().get()) {
                deltaPitch = -deltaPitch;
            }

            // 2. Применяем чувствительность (ванильный Minecraft стиль)
            double sensitivity = mc.options.sensitivity().get();
            double scale = Math.pow(sensitivity * 0.6 + 0.2, 3) * 2.0;

            // 3. Мгновенное обновление углов
            currentYaw = deltaYaw * (float)scale;
            currentPitch = deltaPitch * (float)scale;

            // 4. Полная остановка при отсутствии ввода
            if (Math.abs(deltaYaw) < 0.001f) {
                currentYaw = 0;
            }
            if (Math.abs(deltaPitch) < 0.001f) {
                currentPitch = 0;
            }

            // 5. Ограничение углов
            currentPitch = Math.max(-90.0f, Math.min(90.0f, currentPitch));
            currentYaw %= 360.0f;

            // 6. Применение к дрону
       //     drone.setYRot(currentYaw);
       //     drone.setXRot(currentPitch);
      //      drone.yRotO = currentYaw;
       //     drone.xRotO = currentPitch;
        }
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

        drone.applyClientMovement(movement);

        // Отправляем движение и ориентацию на сервер
        Core.CHANNEL.sendToServer(new DroneMovePacket(
                drone.getUUID(),
                movement,
                currentYaw,
                currentPitch
        ));

        // Выход из режима управления дроном
        if (EXIT_CONTROL_KEY.consumeClick()) {
            mc.setCameraEntity(mc.player);
            if (mouseGrabbed) {
                mc.mouseHandler.releaseMouse();
                mouseGrabbed = false;
            }
        }
    }
    public static void stopPlayer(Player player) {
        if (player != null) {
            player.setDeltaMovement(Vec3.ZERO);
            player.zza = 0;
            player.xxa = 0;
            player.yya = 0;
        }
    }
    public static void mouseGrabber() {
        if (mc.getCameraEntity() instanceof DroneEntity drone) {
            if (!mouseGrabbed && !guiOpen) {
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
        if (guiOpen && mouseGrabbed) {
            mc.mouseHandler.releaseMouse();
            mouseGrabbed = false;
        }
        if (!(mc.getCameraEntity() instanceof DroneEntity drone)) {
            return;
        }
    }
    public static void turnCameraTest(DroneEntity drone) {

    }
    public static void turnCamera(DroneEntity drone) {
        // 1. Получаем ТОЛЬКО последние дельты (без накопления)
        float deltaYaw = (float) mc.mouseHandler.getXVelocity();
        float deltaPitch = (float) mc.mouseHandler.getYVelocity();

        if (mc.options.invertYMouse().get()) {
            deltaPitch = -deltaPitch;
        }

        // 2. Чувствительность (фиксированный масштаб)
        double sensitivity = mc.options.sensitivity().get();
        double scale = Math.pow(sensitivity * 0.6 + 0.2, 3) * 0.1; // Уменьшенный множитель

        // 3. Не накапливаем, а используем мгновенные значения
        float targetYaw = deltaYaw * (float)scale;
        float targetPitch = deltaPitch * (float)scale;

        // 4. Плавное движение к цели
        float smoothing = 0.2f;
        currentYaw = Mth.lerp(smoothing, currentYaw, currentYaw + targetYaw);
        currentPitch = Mth.lerp(smoothing, currentPitch, currentPitch + targetPitch);

        // 5. Автоматическое торможение при отсутствии ввода
        if (Math.abs(deltaYaw) < 0.001f && Math.abs(deltaPitch) < 0.001f) {
            currentYaw *= 0.9f;
            currentPitch *= 0.9f;
        }

        // Ограничение углов
        currentPitch = Math.max(-90.0f, Math.min(90.0f, currentPitch));
        currentYaw %= 360.0f;

        // Применение к дрону
        drone.setYRot(currentYaw);
        drone.setXRot(currentPitch);
        drone.yRotO = currentYaw;
        drone.xRotO = currentPitch;
    }
    public static void moveDrone(Minecraft mc, float speed, DroneEntity drone, Vec3 movement) {

    }
}

