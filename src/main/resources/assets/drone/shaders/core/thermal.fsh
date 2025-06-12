#version 150

uniform sampler2D DiffuseSampler;
uniform sampler2D EntityMask;
in vec2 uv;
out vec4 fragColor;

float smoothExp(float t, float gamma) {
    return pow(t, gamma);
}

void main() {
    vec4 color = texture(DiffuseSampler, uv);
    vec4 mask = texture(EntityMask, uv);

    float warmthRaw = max(color.r, color.g) - color.b * 0.5;
    warmthRaw = clamp(warmthRaw, 0.0, 1.0);

    // Уменьшаем вклад "тепла", чтобы оно было мягче
    float warmth = clamp(warmthRaw + 0.1, 0.0, 1.0);

    float gray = dot(color.rgb, vec3(0.4, 0.59, 0.11));
    float contrast = 1.0 + warmth * 2.0;
    gray = pow(gray, 1.0 / contrast);

    float coldFactor = clamp(color.b - (color.r + color.g) * 0.3, 0.0, 1.0);
    gray *= (1.0 - coldFactor * 0.7);

    gray = smoothstep(0.1, 0.9, gray);
    gray = clamp(gray * 2.2, 0.0, 2.0); // Было *2.2, стало *1.8

    vec3 coldColor = vec3(0.1, 0.1, 1.0);
    vec3 neutralColor = vec3(0.2, 1.0, 0.2);
    vec3 warmColor = vec3(1.0, 0.0, 0.0); // Было (1.0, 0.0, 0.0), сделали мягче (оранжевее)

    vec3 colorFromWarmth;
    if (warmth < 0.5) {
        float t = warmth * 2.0;
        float gamma = 0.5; // Было 0.4, чуть мягче переход
        float tExp = pow(t, gamma);
        colorFromWarmth = mix(coldColor, neutralColor, tExp);
    } else {
        float t = (warmth - 0.5) * 2.0;
        float gamma = 0.5;
        float tExp = pow(t, gamma);
        colorFromWarmth = mix(neutralColor, warmColor, tExp);
    }

    vec3 finalColor = colorFromWarmth * gray;
    finalColor = clamp(finalColor, 0.0, 1.0);

    // Подсветка сущностей
    float maskIntensity = max(max(mask.r, mask.g), mask.b);

    if (maskIntensity > 0.1) {
        vec3 entityHighlight = vec3(1.0, 0.4, 0.1); // Было (1.0, 0.3, 0.0), сделали чуть мягче
        float highlightStrength = 0.7;              // Было 0.9, сделаем мягче
        float t = clamp(maskIntensity, 0.0, 1.0) * highlightStrength;
        finalColor = mix(finalColor, entityHighlight, t);

        // Уменьшаем дополнительную яркость сущностей
        finalColor = clamp(finalColor * 1.2, 0.0, 1.0); // Было *1.5
    } else {
        finalColor *= 0.75; // Фон чуть темнее
    }

    fragColor = vec4(finalColor, color.a);
}