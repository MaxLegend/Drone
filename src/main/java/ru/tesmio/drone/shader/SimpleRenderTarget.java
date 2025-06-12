package ru.tesmio.drone.shader;

import com.mojang.blaze3d.pipeline.RenderTarget;

public class SimpleRenderTarget extends RenderTarget {
    public SimpleRenderTarget(int width, int height, boolean useDepth, boolean onMac) {
        super(useDepth);
        this.resize(width, height, onMac);
    }
}