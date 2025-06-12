#version 150

in vec3 Position;
in vec2 UV0;

out vec2 uv;

void main() {
    uv = UV0;
    gl_Position = vec4(Position, 1.0);
}