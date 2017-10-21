#version 330

in vec2 outTexCoord;
in vec3 surfaceNormal;
in vec3 lightDirection;
in vec3 lightReflection;
in vec3 cameraDirection;

out vec4 fragColor;

uniform sampler2D texture_sampler;

float ambient = 0.3;

void main()
{
    float diffuse = max(0.0, dot(surfaceNormal, lightDirection));

    float specularFactor = max(0.0, dot(cameraDirection, lightReflection));
    float specular = 0.8 * pow(specularFactor, 8.0);

    float brightness = ambient + diffuse + specular;

    vec4 color = texture(texture_sampler, outTexCoord);
    fragColor = vec4(color.xyz * brightness, color.w);
}