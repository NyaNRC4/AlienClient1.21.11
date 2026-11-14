// 1.21.1: assets/minecraft/shaders/program/pulse.fsh
#version 150

uniform sampler2D DiffuseSampler;

in vec2 texCoord;
out vec4 fragColor;

layout(std140) uniform AlienShader {
    vec4 color;
    vec4 color2;
    vec4 resolution;
    float radius;
    float quality;
    float divider;
    float maxSample;
    float mixFactor;
    float minAlpha;
    float time;
    float size;
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
    float alpha = 0.0;
    // 时间和横向位置影响渐变
    float gradient = 0.5 + 0.5 * sin(time + texCoord.x * size);
    vec3 animatedColor = mix(color.rgb, color2.rgb, gradient);

    if (centerCol.a != 0.0) {
        fragColor = vec4(animatedColor, mixFactor);
    } else {
        alpha = glowShader();
        fragColor = vec4(animatedColor, alpha);
    }
}
