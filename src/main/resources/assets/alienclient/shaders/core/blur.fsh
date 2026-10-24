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

// Color.xy = region origin in physical pixels (y down)
// Color.zw = region size in physical pixels
// Glow.x   = blur radius

void main() {
    vec2 p = gl_FragCoord.xy;
    vec2 origin = Color.xy;
    vec2 size = Color.zw;
    if (p.x < origin.x || p.y < origin.y || p.x > origin.x + size.x || p.y > origin.y + size.y) {
        fragColor = texture(InSampler, texCoord);
        return;
    }
    float r = clamp(Glow.x, 1.0, 32.0);
    vec2 texel = 1.0 / vec2(textureSize(InSampler, 0));
    vec4 sum = vec4(0.0);
    float count = 0.0;
    for (float x = -r; x <= r; x += 1.0) {
        for (float y = -r; y <= r; y += 1.0) {
            sum += texture(InSampler, texCoord + vec2(x, y) * texel);
            count += 1.0;
        }
    }
    fragColor = sum / count;
}
