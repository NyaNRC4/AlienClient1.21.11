// 区域模糊的合成 pass：把模糊后的 target 写回主画面（不混合，直接覆盖）。
#version 150

uniform sampler2D DiffuseSampler;

in vec2 texCoord;
out vec4 fragColor;

void main() {
    fragColor = texture(DiffuseSampler, texCoord);
}
