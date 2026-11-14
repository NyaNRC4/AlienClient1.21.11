// 1.21.1: assets/minecraft/shaders/program/gradient.fsh
// 说明：原版把 uniform 命名为 step / 局部变量命名为 distance（与 GLSL 内建函数同名），
// 这里分别改成 stepSize / dist，算法与取值完全一致。
#version 150

uniform sampler2D DiffuseSampler;

in vec2 texCoord;
out vec4 fragColor;

layout(std140) uniform AlienShader {
    vec4 rgb;
    vec4 rgb1;
    vec4 rgb2;
    vec4 rgb3;
    vec4 resolution;
    float radius;
    float quality;
    float divider;
    float maxSample;
    float alpha2;
    float stepSize;
    float time;
};

float glowShader() {
    vec2 texelSize = vec2(1.0 / resolution.x * (radius * quality), 1.0 / resolution.y * (radius * quality));
    float alpha = 0.0;

    for (float x = -radius; x < radius; x++) {
        for (float y = -radius; y < radius; y++) {
            vec4 currentColor = texture(DiffuseSampler, texCoord + vec2(texelSize.x * x, texelSize.y * y));

            if (currentColor.a != 0.0)
                alpha += divider > 0.0 ? max(0.0, (maxSample - distance(vec2(x, y), vec2(0.0))) / divider) : 1.0;
        }
    }

    return alpha;
}

vec3 getColor(vec4 centerCol) {
    // step 滑条为 0 时原实现会除零，这里给一个下限
    float st = max(stepSize, 0.001);

    float dist = sqrt(gl_FragCoord.x * gl_FragCoord.x + gl_FragCoord.y * gl_FragCoord.y) + time;
    float dist2 = sqrt((gl_FragCoord.x - 1920.0) * (gl_FragCoord.x - 1920.0) + gl_FragCoord.y * gl_FragCoord.y) + time;
    float dist3 = sqrt((gl_FragCoord.x - 1080.0) * (gl_FragCoord.x - 1080.0) + (gl_FragCoord.y - 1080.0) * (gl_FragCoord.y - 1080.0)) + time;

    dist = sin(dist / st + sin(gl_FragCoord.y / st)) * 0.5 + 0.5;
    dist2 = sin(dist2 / st + sin(gl_FragCoord.x / st)) * 0.5 + 0.5;
    dist3 = sin(dist3 / st + sin(gl_FragCoord.y / st + gl_FragCoord.x / st)) * 0.5 + 0.5;

    float ripple = sin(dist * 10.0 + st * 10000.0) * 0.05;
    float swirl = cos((gl_FragCoord.x - 960.0) * 0.05 + (gl_FragCoord.y - 540.0) * 0.05 + st) * 0.05;

    dist += ripple + swirl;
    dist2 += ripple - swirl;
    dist3 += ripple + swirl;

    float distInv = 1.0 - dist;
    float r = rgb.r * dist + rgb1.r * distInv + rgb2.r * dist2 + rgb3.r * dist3;
    float g = rgb.g * dist + rgb1.g * distInv + rgb2.g * dist2 + rgb3.g * dist3;
    float b = rgb.b * dist + rgb1.b * distInv + rgb2.b * dist2 + rgb3.b * dist3;
    return vec3(r, g, b);
}

void main() {
    vec4 centerCol = texture(DiffuseSampler, texCoord);

    if (centerCol.a != 0.0) {
        fragColor = vec4(getColor(centerCol), alpha2);
    } else {
        fragColor = vec4(getColor(centerCol), glowShader());
    }
}
