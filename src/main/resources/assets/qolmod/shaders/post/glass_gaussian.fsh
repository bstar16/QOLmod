#version 330

uniform sampler2D InSampler;

layout(std140) uniform SamplerInfo {
    vec2 OutSize;
    vec2 InSize;
};

layout(std140) uniform BlurConfig {
    vec2 Direction;
};

in vec2 texCoord;

out vec4 fragColor;

// Nine-tap Gaussian represented by five bilinear samples. The 3x spread gives a moderate
// physical-pixel radius while the padded capture prevents clamping at QOLmod surface edges.
void main() {
    vec2 stepSize = Direction * 3.0 / InSize;
    vec4 color = texture(InSampler, texCoord) * 0.2270270270;
    color += texture(InSampler, texCoord + stepSize * 1.3846153846) * 0.3162162162;
    color += texture(InSampler, texCoord - stepSize * 1.3846153846) * 0.3162162162;
    color += texture(InSampler, texCoord + stepSize * 3.2307692308) * 0.0702702703;
    color += texture(InSampler, texCoord - stepSize * 3.2307692308) * 0.0702702703;
    fragColor = color;
}
