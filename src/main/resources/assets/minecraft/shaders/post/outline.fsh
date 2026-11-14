// 1.21.1: assets/minecraft/shaders/program/outline.fsh（Solid 模式）
// 1.21.11: 同样的算法，uniform 收进 std140 UBO（Satin 重构版的后处理运行时）。
#version 150

uniform sampler2D DiffuseSampler;

in vec2 texCoord;
out vec4 fragColor;

layout(std140) uniform AlienShader {
    vec4 color;
    vec4 resolution;
    float radius;
    float quality;
    float divider;
    float maxSample;
    float mixFactor;
    float minAlpha;
};

float glowShader() {
    vec2 texelSize = vec2(1.0 / resolution.x * (radius * quality), 1.0 / resolution.y * (radius * quality));
    float alpha = 0.0;

    for (float x = -radius; x < radius; x++) {
        for (float y = -radius; y < radius; y++) {
            vec4 currentColor = texture(DiffuseSampler, texCoord + vec2(texelSize.x * x, texelSize.y * y));

            if (currentColor.a != 0.0)
                alpha += divider > 0.0 ? max(0.0, (maxSample - distance(vec2(x, y), vec2(0.0))) / divider) : 1.0;
            alpha *= minAlpha;
        }
    }

    return alpha;
}

void main() {
    vec4 centerCol = texture(DiffuseSampler, texCoord);

    if (centerCol.a != 0.0) {
        fragColor = vec4(color.rgb, mixFactor);
    } else {
        fragColor = vec4(color.rgb, glowShader());
    }
}
