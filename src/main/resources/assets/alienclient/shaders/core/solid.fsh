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

void main() {
    vec4 centerCol = texture(InSampler, texCoord);
    if (centerCol.a != 0.0) {
        fragColor = vec4(Color.rgb, Resolution.w);
    } else {
        fragColor = vec4(0.0, 0.0, 0.0, 0.0);
    }
}
