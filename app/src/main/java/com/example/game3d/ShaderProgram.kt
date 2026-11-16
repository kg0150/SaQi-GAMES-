package com.example.game3d

import android.opengl.GLES30

class ShaderProgram(vertexSrc: String, fragmentSrc: String) {
    val programId: Int

    init {
        val vs = compile(GLES30.GL_VERTEX_SHADER, vertexSrc)
        val fs = compile(GLES30.GL_FRAGMENT_SHADER, fragmentSrc)
        programId = GLES30.glCreateProgram()
        GLES30.glAttachShader(programId, vs)
        GLES30.glAttachShader(programId, fs)
        GLES30.glLinkProgram(programId)
        val linkStatus = IntArray(1)
        GLES30.glGetProgramiv(programId, GLES30.GL_LINK_STATUS, linkStatus, 0)
        if (linkStatus[0] == 0) {
            val log = GLES30.glGetProgramInfoLog(programId)
            GLES30.glDeleteProgram(programId)
            throw RuntimeException("Shader link error: $log")
        }
        GLES30.glDeleteShader(vs)
        GLES30.glDeleteShader(fs)
    }

    fun use() = GLES30.glUseProgram(programId)

    fun loc(name: String): Int = GLES30.glGetUniformLocation(programId, name)

    fun release() = GLES30.glDeleteProgram(programId)

    private fun compile(type: Int, src: String): Int {
        val shader = GLES30.glCreateShader(type)
        GLES30.glShaderSource(shader, src)
        GLES30.glCompileShader(shader)
        val status = IntArray(1)
        GLES30.glGetShaderiv(shader, GLES30.GL_COMPILE_STATUS, status, 0)
        if (status[0] == 0) {
            val log = GLES30.glGetShaderInfoLog(shader)
            GLES30.glDeleteShader(shader)
            throw RuntimeException("Shader compile error: $log")
        }
        return shader
    }
}

object Shaders {
    const val SHADOW_VERTEX = """#version 300 es
layout(location = 0) in vec3 aPos;
uniform mat4 uLightSpaceMatrix;
uniform mat4 uModel;
void main() {
    gl_Position = uLightSpaceMatrix * uModel * vec4(aPos, 1.0);
}
"""

    const val SHADOW_FRAGMENT = """#version 300 es
precision mediump float;
void main() {
}
"""

    const val SKY_VERTEX = """#version 300 es
layout(location = 0) in vec3 aPos;
uniform mat4 uViewProj;
uniform vec3 uCamPos;
out vec3 vDir;
void main() {
    vDir = normalize(aPos);
    vec4 pos = uViewProj * vec4(aPos * 260.0 + uCamPos, 1.0);
    gl_Position = pos.xyww;
}
"""

    const val SKY_FRAGMENT = """#version 300 es
precision highp float;
in vec3 vDir;
uniform vec3 uZenithColor;
uniform vec3 uHorizonColor;
uniform vec3 uSunDir;
uniform vec3 uSunColor;
uniform float uNightFactor;
uniform float uTimeSec;
out vec4 FragColor;

float hash(vec3 p) {
    p = fract(p * 0.3183099 + 0.1);
    p *= 17.0;
    return fract(p.x * p.y * p.z * (p.x + p.y + p.z));
}

void main() {
    vec3 dir = normalize(vDir);
    float h = clamp(dir.y, 0.0, 1.0);
    vec3 col = mix(uHorizonColor, uZenithColor, pow(h, 0.55));

    // Sun / Moon disc + atmospheric halo
    float sunDot = max(dot(dir, normalize(uSunDir)), 0.0);
    col += uSunColor * pow(sunDot, 12.0) * 0.38;
    col += uSunColor * smoothstep(0.9965, 0.9992, sunDot) * 1.4;

    // Night stars
    if (uNightFactor > 0.05 && dir.y > 0.08) {
        vec3 starCell = floor(dir * 165.0);
        float s = hash(starCell);
        if (s > 0.991) {
            float twinkle = 0.65 + 0.35 * sin(uTimeSec * 3.0 + s * 100.0);
            col += vec3(0.85, 0.92, 1.0) * uNightFactor * twinkle * smoothstep(0.08, 0.3, dir.y);
        }
    }

    FragColor = vec4(col, 1.0);
}
"""

    const val WORLD_VERTEX = """#version 300 es
layout(location = 0) in vec3 aPos;
layout(location = 1) in vec3 aNormal;
layout(location = 2) in vec2 aUv;

uniform mat4 uViewProj;
uniform mat4 uModel;
uniform mat4 uLightSpaceMatrix;
uniform vec2 uUvScale;

out vec3 vWorldPos;
out vec3 vNormal;
out vec2 vUv;
out vec4 vLightSpacePos;

void main() {
    vec4 worldPos = uModel * vec4(aPos, 1.0);
    vWorldPos = worldPos.xyz;
    vNormal = normalize(mat3(uModel) * aNormal);
    vUv = aUv * uUvScale;
    vLightSpacePos = uLightSpaceMatrix * worldPos;
    gl_Position = uViewProj * worldPos;
}
"""

    const val WORLD_FRAGMENT = """#version 300 es
precision highp float;

in vec3 vWorldPos;
in vec3 vNormal;
in vec2 vUv;
in vec4 vLightSpacePos;

uniform sampler2D uAlbedoTex;
uniform sampler2D uShadowMap;

uniform vec4 uBaseColor;
uniform vec3 uEmissiveColor;
uniform float uEmissiveStrength;
uniform float uSpecularStrength;
uniform float uShininess;
uniform float uRimStrength;
uniform float uNightWindowBoost;

uniform vec3 uCamPos;
uniform vec3 uSunDir;
uniform vec3 uSunColor;
uniform vec3 uSkyAmbient;
uniform vec3 uGroundAmbient;
uniform vec3 uFogColor;
uniform float uFogDensity;
uniform float uShadowEnabled;

uniform int uPointLightCount;
uniform vec3 uPointLightPos[6];
uniform vec3 uPointLightColor[6];
uniform float uPointLightRadius[6];

out vec4 FragColor;

float computeShadow(vec4 lightSpacePos, vec3 normal, vec3 lightDir) {
    if (uShadowEnabled < 0.5) return 0.0;
    vec3 proj = lightSpacePos.xyz / lightSpacePos.w;
    proj = proj * 0.5 + 0.5;
    if (proj.z > 1.0 || proj.x < 0.0 || proj.x > 1.0 || proj.y < 0.0 || proj.y > 1.0) {
        return 0.0;
    }
    float bias = max(0.0028 * (1.0 - dot(normal, lightDir)), 0.0009);
    vec2 texelSize = 1.0 / vec2(textureSize(uShadowMap, 0));
    float shadow = 0.0;
    for (int x = -1; x <= 1; x += 2) {
        for (int y = -1; y <= 1; y += 2) {
            float pcfDepth = texture(uShadowMap, proj.xy + vec2(x, y) * texelSize).r;
            shadow += (proj.z - bias > pcfDepth) ? 1.0 : 0.0;
        }
    }
    shadow *= 0.25;
    // Fade shadow softly near edge of shadow frustum
    float edgeDist = max(abs(proj.x - 0.5), abs(proj.y - 0.5)) * 2.0;
    shadow *= 1.0 - smoothstep(0.82, 1.0, edgeDist);
    return shadow;
}

void main() {
    vec4 texCol = texture(uAlbedoTex, vUv);
    vec3 albedo = texCol.rgb * uBaseColor.rgb;
    float alpha = texCol.a * uBaseColor.a;

    vec3 N = normalize(vNormal);
    vec3 V = normalize(uCamPos - vWorldPos);
    vec3 L = normalize(uSunDir);

    // Hemisphere ambient
    float hemi = N.y * 0.5 + 0.5;
    vec3 ambient = mix(uGroundAmbient, uSkyAmbient, hemi) * albedo;

    // Directional sun/moon + soft PCF shadow
    float NdotL = max(dot(N, L), 0.0);
    float shadow = computeShadow(vLightSpacePos, N, L);
    vec3 H = normalize(L + V);
    float spec = pow(max(dot(N, H), 0.0), uShininess) * uSpecularStrength;
    vec3 dirLight = (albedo * NdotL + vec3(spec) * step(0.01, NdotL)) * uSunColor * (1.0 - shadow * 0.78);

    // Dynamic point lights (siren red/blue, street lamps, mission beacons)
    vec3 pointContrib = vec3(0.0);
    for (int i = 0; i < 6; i++) {
        if (i >= uPointLightCount) break;
        vec3 toLight = uPointLightPos[i] - vWorldPos;
        float dist = length(toLight);
        float rad = uPointLightRadius[i];
        if (dist < rad) {
            vec3 plDir = toLight / max(dist, 0.001);
            float att = 1.0 - (dist / rad);
            att = att * att;
            float pDiff = max(dot(N, plDir), 0.0);
            vec3 pH = normalize(plDir + V);
            float pSpec = pow(max(dot(N, pH), 0.0), uShininess) * uSpecularStrength * 0.8;
            pointContrib += (albedo * pDiff + vec3(pSpec)) * uPointLightColor[i] * att;
        }
    }

    // Subtle Fresnel rim light for 3D silhouette clarity
    float fresnel = pow(1.0 - max(dot(N, V), 0.0), 3.2);
    vec3 rim = uSkyAmbient * fresnel * uRimStrength;

    // Emissive + warm night window boost
    float warmMask = clamp((texCol.r - texCol.b) * 2.2, 0.0, 1.0);
    vec3 emissive = uEmissiveColor * uEmissiveStrength + texCol.rgb * warmMask * uNightWindowBoost;

    vec3 color = ambient + dirLight + pointContrib + rim + emissive;

    // Atmospheric distance fog
    float viewDist = length(uCamPos - vWorldPos);
    float fogFactor = 1.0 - exp(-pow(viewDist * uFogDensity, 1.6));
    color = mix(color, uFogColor, clamp(fogFactor, 0.0, 0.92));

    FragColor = vec4(color, alpha);
}
"""
}
