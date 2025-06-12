#version 150

uniform sampler2D DiffuseSampler;
uniform float gain;   // Усиление яркости (например, 1.5)
uniform float gamma;  // Гамма для теней (например, 0.4)

in vec2 uv;
out vec4 fragColor;

float smoothExp(float t, float gamma) {
    // gamma < 1 — резкий переход ближе к 1
    // gamma > 1 — резкий переход ближе к 0
    return pow(t, gamma);
}

void main() {
    vec4 color = texture(DiffuseSampler, uv);

    // Оценка тепла: чем выше, тем теплее пиксель
    float warmthRaw = max(color.r, color.g) - color.b * 0.5;
    warmthRaw = clamp(warmthRaw, 0.0, 1.0);

    // Смещаем warmth в сторону теплых цветов
    // Например, сдвигаем на +0.3 и ограничиваем максимум 1.0
    float warmth = clamp(warmthRaw + 0.1, 0.0, 1.0);

    // Основа яркости в зависимости от тепла
    float gray = dot(color.rgb, vec3(0.4, 0.59, 0.11));
    float contrast = 1.0 + warmth * 2.0;
    gray = pow(gray, 1.0 / contrast);

    // Уменьшаем яркость для холодных пикселей
    float coldFactor = clamp(color.b - (color.r + color.g) * 0.3, 0.0, 1.0);
    gray = gray * (1.0 - coldFactor * 0.8);

    // Нормализация яркости
    gray = smoothstep(0.1, 0.9, gray);
    gray = clamp(gray * 2.2, 0.0, 2.0);

    // Цветовая карта — три цвета
    vec3 coldColor = vec3(0.1, 0.1, 1.0);       // ярко-синий
    vec3 neutralColor = vec3(0.2, 1.0, 0.2);    // ярко-зеленый
    vec3 warmColor = vec3(1.0, 0.0, 0.0);       // оранжево-жёлтый

    vec3 colorFromWarmth;
    if (warmth < 0.5) {
        float t = warmth * 2.0;
        float gamma = 0.4;
        float tExp = pow(t, gamma);
        colorFromWarmth = mix(coldColor, neutralColor, tExp);
    } else {
        float t = (warmth - 0.5) * 2.0;
        float gamma = 0.4;
        float tExp = pow(t, gamma);
        colorFromWarmth = mix(neutralColor, warmColor, tExp);
    }

    vec3 finalColor = colorFromWarmth * gray;
    finalColor = clamp(finalColor, 0.0, 1.0);

    fragColor = vec4(finalColor, color.a);
}