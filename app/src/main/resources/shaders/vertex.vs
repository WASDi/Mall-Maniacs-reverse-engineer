#version 330

layout (location=0) in vec3 position;
layout (location=1) in vec2 texCoord;
layout (location=2) in vec3 normal;

out vec2 outTexCoord;
out vec3 surfaceNormal;
out vec3 lightDirection;
out vec3 lightReflection;
out vec3 cameraDirection;

uniform mat4 modelMatrix;
uniform mat4 viewMatrix;
uniform mat4 projectionMatrix;

uniform vec3 lightPosition;
uniform vec3 cameraPosition;

void main()
{
    vec4 worldPosition = modelMatrix * vec4(position, 1.0);
    gl_Position = projectionMatrix * viewMatrix * worldPosition;
    outTexCoord = texCoord;

    // These will be a bit wrong? Because they are calculated per vertex, being interpolated and no longer normalized.
    surfaceNormal = normalize((modelMatrix * vec4(normal, 0.0)).xyz);

    lightDirection = normalize(lightPosition - worldPosition.xyz);
    lightReflection = reflect(lightDirection, surfaceNormal);

    cameraDirection = normalize(worldPosition.xyz - cameraPosition);
}