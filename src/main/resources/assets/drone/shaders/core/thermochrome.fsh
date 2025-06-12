#version 150

uniform sampler2D DiffuseSampler;
uniform float gain;   // Усиление яркости (например, 1.5)
uniform float gamma;  // Гамма для теней (например, 0.4)

in vec2 uv;
out vec4 fragColor;

void main() {
    vec4 color = texture(DiffuseSampler, uv);

    float warmth = max(color.r, color.g) - color.b * 0.5;
    warmth = clamp(warmth, 0.0, 1.0);

    float gray = dot(color.rgb, vec3(0.4, 0.59, 0.11));

    float contrast = 1.0 + warmth * 2.0;
    gray = pow(gray, 1.0 / contrast);

    float coldFactor = clamp(color.b - (color.r + color.g) * 0.3, 0.0, 1.0);
    gray = gray * (1.0 - coldFactor * 0.8);

    gray = smoothstep(0.1, 0.9, gray);
    gray = clamp(gray * 2.2, 0.0, 2.0);

    fragColor = vec4(vec3(gray), color.a);
}