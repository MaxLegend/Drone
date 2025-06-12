#version 150

uniform sampler2D DiffuseSampler;

in vec2 uv;
out vec4 fragColor;

void main() {
    vec4 color = texture(DiffuseSampler, uv);

    float coldFactor = clamp(color.b - (color.r + color.g) * 0.5, 0.0, 1.0);
    float gray = dot(color.rgb, vec3(0.4, 0.59, 0.11));

    gray = pow(gray, 0.33);
    gray *= 1.1;
    gray = clamp(gray, 0.0, 1.0);

    gray = gray * (1.0 - coldFactor * 1.8);

    float contrast = 1.3;
    gray = (gray - 0.5) * contrast + 0.5;
    gray = clamp(gray, 0.0, 1.0);

    float dehazeStrength = 0.2;
    gray = mix(gray, gray + (gray - gray * gray), dehazeStrength);
    gray = clamp(gray, 0.0, 1.0);

    float blackToGray = smoothstep(0.0, 0.3, gray);
    float grayToWhite = smoothstep(0.3, 0.7, gray);

    // Чёрно-белый базовый цвет
    vec3 grayColor = mix(
    vec3(0.0),
    mix(vec3(0.3), vec3(0.9), grayToWhite),
    blackToGray
    );

    // Добавляем 50% исходного цвета к чёрно-белому
    vec3 finalColor = mix(grayColor, color.rgb, 0.4);

    fragColor = vec4(finalColor, color.a);
}