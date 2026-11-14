// 1.21.1: assets/minecraft/shaders/program/snow.fsh
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
    float time;
};

float snow(vec2 uv, float scale) {
    float w = smoothstep(1.0, 0.0, -uv.y * (scale / 10.0));
    if (w < .1) return 0.0;
    uv += time / scale;
    uv.y += time * 2.0 / scale;
    uv.x += sin(uv.y + time * .5) / scale;
    uv *= scale;
    vec2 s = floor(uv), f = fract(uv), p;
    float k = 3.0, d;
    p = .5 + .35 * sin(11.0 * fract(sin((s + p + scale) * mat2(7, 3, 6, 5)) * 5.0)) - f;
    d = length(p);
    k = min(d, k);
    k = smoothstep(0.0, k, sin(f.x + f.y) * 0.01);
    return k * w;
}

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

void main() {
    vec4 centerCol = texture(DiffuseSampler, texCoord);

    vec2 uv = (gl_FragCoord.xy * 2.0 - resolution.xy) / min(resolution.x, resolution.y);
    vec3 finalColor = vec3(0.0);
    float c = smoothstep(1.0, 0.3, clamp(uv.y * .3 + .8, 0.0, .75));
    c += snow(uv, 30.0) * .0;
    c += snow(uv, 20.0) * .0;
    c += snow(uv, 15.0) * .0;
    c += snow(uv, 10.0);
    c += snow(uv, 8.0);
    c += snow(uv, 6.0);
    c += snow(uv, 5.0);
    finalColor = vec3(c);
    finalColor *= vec3(color[0], color[1], color[2]);

    float alpha = 0.0;
    if (centerCol.a != 0.0) {
        alpha = color[3];
    } else {
        alpha = glowShader();
    }

    fragColor = vec4(finalColor, alpha);
}
