#version 150

uniform sampler2D DiffuseSampler;

in vec2 texCoord;
out vec4 fragColor;

layout(std140) uniform AlienBlur {
    vec4 BlurXY;
    vec4 BlurCoord;
    vec4 InSize;
    vec4 Dir;
    float Radius;
};

void main() {
    vec2 p = gl_FragCoord.xy;
    vec2 origin = BlurXY.xy;
    vec2 size = BlurCoord.xy;
    vec4 source = texture(DiffuseSampler, texCoord);

    if (p.x < origin.x || p.y < origin.y || p.x > origin.x + size.x || p.y > origin.y + size.y) {
        fragColor = source;
        return;
    }

    vec2 texel = 1.0 / InSize.xy;
    float r = clamp(Radius, 1.0, 64.0);
    float sigma = max(r * 0.5, 0.0001);
    float sigma2 = 2.0 * sigma * sigma;

    vec4 sum = vec4(0.0);
    float total = 0.0;

    for (float i = -r; i <= r; i += 1.0) {
        float weight = exp(-(i * i) / sigma2);
        sum += texture(DiffuseSampler, texCoord + Dir.xy * texel * i) * weight;
        total += weight;
    }

    fragColor = vec4((sum / max(total, 0.0001)).rgb, 1.0);
}
