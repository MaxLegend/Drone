package ru.tesmio.drone.drone;

import net.minecraft.network.chat.Component;

public enum EnumFlightMode {
    SLOW(0.3f),
    NORMAL(0.7f),
    SPORT(1.3f);

    public final float speedMultiplier;

    EnumFlightMode(float speedMultiplier) {
        this.speedMultiplier = speedMultiplier;
    }


    public EnumFlightMode next() {
        return values()[(this.ordinal() + 1) % values().length];
    }


    public Component getDisplayText() {
        return switch (this) {
            case SLOW -> Component.translatable("drone.flight_mode.slow");
            case NORMAL -> Component.translatable("drone.flight_mode.normal");
            case SPORT -> Component.translatable("drone.flight_mode.sport");
        };
    }
}

