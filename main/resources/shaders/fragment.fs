#version 330

in vec2 outTexCoord;
in vec3 surfaceNormal;
in vec3 deltaLightPos;

out vec4 fragColor;

uniform sampler2D texture_sampler;

float ambientLight = 0.3;

void main()
{
    vec3 unitNormal = normalize(surfaceNormal);
    vec3 unitLightVector = normalize(deltaLightPos);

    float nDot1 = dot(unitNormal, unitLightVector);
    float brightness = max(nDot1, ambientLight);

    vec4 color = texture(texture_sampler, outTexCoord);
    fragColor = vec4(color.xyz * brightness, color.w);
}