// 1.21.1: assets/minecraft/shaders/program/rainbow.fsh
#version 150

uniform sampler2D DiffuseSampler;

in vec2 texCoord;
out vec4 fragColor;

layout(std140) uniform AlienShader {
    vec4 resolution;
    float radius;
    float quality;
    float divider;
    float maxSample;
    float alpha2;
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

void main() {
    vec2 uv = gl_FragCoord.xy / resolution.xy;
    vec3 o = -3.1416 * vec3(0.0, .5, 1.0);

    float g = uv.y + time;
    vec3 col = .5 + .5 * -sin(g) * cos(g + o);

    col.g += .25;
    col = .5 + (col * 2.0 - 1.0);
    col.gb *= vec2(.75, .9);
    col = .125 + .75 * col;

    vec4 centerCol = texture(DiffuseSampler, texCoord);

    if (centerCol.a != 0.0) {
        fragColor = vec4(col, alpha2);
    } else {
        fragColor = vec4(col, glowShader());
    }
}
