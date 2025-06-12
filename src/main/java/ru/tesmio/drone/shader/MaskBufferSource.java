package ru.tesmio.drone.shader;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;

import java.util.HashMap;
import java.util.Map;

public class MaskBufferSource implements MultiBufferSource {

    private final MultiBufferSource parent;
    private final Map<RenderType, VertexConsumer> maskBuffers = new HashMap<>();

    public MaskBufferSource(MultiBufferSource parent) {
        this.parent = parent;
    }

    @Override
    public VertexConsumer getBuffer(RenderType renderType) {
        // Для всех типов рендеринга возвращаем маскированный буфер
        return maskBuffers.computeIfAbsent(renderType, this::createMaskBuffer);
    }

    private VertexConsumer createMaskBuffer(RenderType renderType) {
        // Получаем оригинальный буфер из нашего ENTITY_MASK_TYPE
        VertexConsumer originalBuffer = parent.getBuffer(RenderEntityMask.ENTITY_MASK_TYPE);

        // Возвращаем обёртку, которая будет рендерить всё белым цветом
        return new MaskVertexConsumer(originalBuffer);
    }

    // Внутренний класс для перехвата и изменения цвета вершин
    private static class MaskVertexConsumer implements VertexConsumer {
        private final VertexConsumer delegate;
        private static final int WHITE_COLOR = 0xFFFFFFFF;

        public MaskVertexConsumer(VertexConsumer delegate) {
            this.delegate = delegate;
        }

        @Override
        public VertexConsumer vertex(double x, double y, double z) {
            return delegate.vertex(x, y, z);
        }

        @Override
        public VertexConsumer color(int red, int green, int blue, int alpha) {
            // Игнорируем оригинальный цвет и всегда используем белый
            return delegate.color(255, 255, 255, 255);
        }

        @Override
        public VertexConsumer uv(float u, float v) {
            return delegate.uv(u, v);
        }

        @Override
        public VertexConsumer overlayCoords(int u, int v) {
            return delegate.overlayCoords(u, v);
        }

        @Override
        public VertexConsumer uv2(int u, int v) {
            return delegate.uv2(u, v);
        }

        @Override
        public VertexConsumer normal(float x, float y, float z) {
            return delegate.normal(x, y, z);
        }

        @Override
        public void endVertex() {
            delegate.endVertex();
        }

        @Override
        public void defaultColor(int red, int green, int blue, int alpha) {
            // Всегда устанавливаем белый как цвет по умолчанию
            delegate.defaultColor(255, 255, 255, 255);
        }

        @Override
        public void unsetDefaultColor() {
            delegate.unsetDefaultColor();
        }
    }
}
