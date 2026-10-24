#version 150

uniform sampler2D InSampler;
in vec2 texCoord;
out vec4 fragColor;

uniform AlienParams {
    vec4 Color;
    vec4 Color2;
    vec4 Resolution;
    vec4 Glow;
    vec4 Color3;
    vec4 Color4;
    vec4 Extra;
};

#define quality Glow.y
#define radius Glow.x
#define divider Glow.z
#define maxSample Glow.w
#define InSize Resolution.xy
#define alpha2 Color.a
#define resolution Resolution.xy
#define time Resolution.z

float glowShader() {

    vec2 texelSize = vec2(1.0 / resolution.x * (radius * quality), 1.0 / resolution.y * (radius * quality));
    float alpha = 0;

    for (float x = -radius; x < radius; x++) {
        for (float y = -radius; y < radius; y++) {
            vec4 currentColor = texture(InSampler, texCoord + vec2(texelSize.x * x, texelSize.y * y));

            if (currentColor.a != 0)
            alpha += divider > 0 ? max(0.0, (maxSample - distance(vec2(x, y), vec2(0))) / divider) : 1;
        }
    }

    return alpha;
}

void main() {
    vec2 uv = gl_FragCoord.xy / resolution.xy;
    vec3 o = -3.1416*vec3(0., .5, 1.);

    float g = uv.y+time;
    vec3 col = .5+.5*-sin(g)*cos(g+o);

    col.g += .25;
    col = .5+(col*2.-1.);
    col.gb *= vec2(.75, .9);
    col = .125+.75*col;

    vec4 centerCol = texture(InSampler, texCoord);

    if (centerCol.a != 0) {
        fragColor = vec4(col, alpha2);
    } else {
        fragColor = vec4(col, glowShader());
    }
}
