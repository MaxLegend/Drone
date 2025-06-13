package ru.tesmio.drone.shader;

import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import ru.tesmio.drone.Dronecraft;

import java.io.IOException;

@Mod.EventBusSubscriber(modid = Dronecraft.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ShaderRegistry {
    public static ShaderInstance MONOCHROME, THERMOCHROME, GREENCHROME, THERMAL;

    @SubscribeEvent
    public static void onRegisterShaders(RegisterShadersEvent evt) throws IOException {
        evt.registerShader(new ShaderInstance(evt.getResourceProvider(), "drone:monochrome", DefaultVertexFormat.POSITION_TEX),
                shader -> MONOCHROME = shader);
        evt.registerShader(new ShaderInstance(evt.getResourceProvider(), "drone:thermochrome", DefaultVertexFormat.POSITION_TEX),
                shader -> THERMOCHROME = shader);
        evt.registerShader(new ShaderInstance(evt.getResourceProvider(), "drone:greenchrome", DefaultVertexFormat.POSITION_TEX),
                shader -> GREENCHROME = shader);
        evt.registerShader(new ShaderInstance(evt.getResourceProvider(), "drone:thermal", DefaultVertexFormat.POSITION_TEX),
                shader -> THERMAL = shader);
    }

}