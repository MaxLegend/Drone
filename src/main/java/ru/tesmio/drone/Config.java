package ru.tesmio.drone;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.common.ForgeConfigSpec;

public class Config {
    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.DoubleValue SPEED_IN_STAB_MODE;

    static {
        BUILDER.push("Drone Movement Settings");

        SPEED_IN_STAB_MODE = BUILDER
                .comment("Max speed multiplier in Stabilize Mode")
                .defineInRange("speedSilentMode", 1f, 0.01f, 10.0f);
        BUILDER.pop();
        SPEC = BUILDER.build();
    }
}
