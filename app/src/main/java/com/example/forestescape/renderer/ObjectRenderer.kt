/*
 * Copyright 2017 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.forestescape.renderer

import android.content.Context
import android.graphics.BitmapFactory
import android.opengl.GLES20
import android.opengl.GLUtils
import android.opengl.Matrix
import de.javagl.obj.ObjData
import de.javagl.obj.ObjReader
import de.javagl.obj.ObjUtils
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.*
import kotlin.math.sqrt

class ObjectRenderer {

    enum class BlendMode {
        Shadow,
        AlphaBlending
    }

    private val viewLightDirection = FloatArray(4)

    // Object vertex buffer variables.
    private var vertexBufferId = 0
    private var verticesBaseAddress = 0
    private var texCoordsBaseAddress = 0
    private var normalsBaseAddress = 0
    private var indexBufferId = 0
    private var indexCount = 0
    private var program = 0
    private val textures = IntArray(1)

    // Shader location: model view projection matrix.
    private var modelViewUniform = 0
    private var modelViewProjectionUniform = 0

    // Shader location: object attributes.
    private var positionAttribute = 0
    private var normalAttribute = 0
    private var texCoordAttribute = 0

    // Shader location: texture sampler.
    private var textureUniform = 0

    // Shader location: environment properties.
    private var lightingParametersUniform = 0

    // Shader location: material properties.
    private var materialParametersUniform = 0

    // Shader location: color correction property.
    private var colorCorrectionParameterUniform = 0

    // Shader location: object color property (to change the primary color of the object).
    private var colorUniform = 0

    // Shader location: depth texture.
    private var depthTextureUniform = 0

    // Shader location: transform to depth uvs.
    private var depthUvTransformUniform = 0

    // Shader location: the aspect ratio of the depth texture.
    private var depthAspectRatioUniform = 0
    private var blendMode: BlendMode? = null

    // Temporary matrices allocated here to reduce number of allocations for each frame.
    private val modelMatrix = FloatArray(16)
    private val modelViewMatrix = FloatArray(16)
    private val modelViewProjectionMatrix = FloatArray(16)

    // Set some default material properties to use for lighting.
    private var ambient = 0.3f
    private var diffuse = 1.0f
    private var specular = 1.0f
    private var specularPower = 6.0f
    private var useDepthForOcclusion = false
    private var depthAspectRatio = 0.0f
    private var uvTransform: FloatArray? = null
    private var depthTextureId = 0

    @Throws(IOException::class)
    fun createOnGlThread(
        context: Context,
        objAssetName: String?,
        diffuseTextureAssetName: String?
    ) {
        compileAndLoadShaderProgram(context)
        readTexture(context, diffuseTextureAssetName)


        val objInputStream = context.assets.open(objAssetName!!)
        var obj = ObjReader.read(objInputStream)

        obj = ObjUtils.convertToRenderable(obj)

        val wideIndices = ObjData.getFaceVertexIndices(obj, 3)
        val vertices = ObjData.getVertices(obj)
        val texCoords = ObjData.getTexCoords(obj, 2)
        val normals = ObjData.getNormals(obj)

        val indices = ByteBuffer.allocateDirect(2 * wideIndices.limit())
            .order(ByteOrder.nativeOrder())
            .asShortBuffer()
        while (wideIndices.hasRemaining()) {
            indices.put(wideIndices.get().toShort())
        }
        indices.rewind()
        val buffers = IntArray(2)
        GLES20.glGenBuffers(2, buffers, 0)
        vertexBufferId = buffers[0]
        indexBufferId = buffers[1]

        verticesBaseAddress = 0
        texCoordsBaseAddress = verticesBaseAddress + 4 * vertices.limit()
        normalsBaseAddress = texCoordsBaseAddress + 4 * texCoords.limit()
        val totalBytes = normalsBaseAddress + 4 * normals.limit()
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vertexBufferId)
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, totalBytes, null, GLES20.GL_STATIC_DRAW)
        GLES20.glBufferSubData(
            GLES20.GL_ARRAY_BUFFER, verticesBaseAddress, 4 * vertices.limit(), vertices
        )
        GLES20.glBufferSubData(
            GLES20.GL_ARRAY_BUFFER, texCoordsBaseAddress, 4 * texCoords.limit(), texCoords
        )
        GLES20.glBufferSubData(
            GLES20.GL_ARRAY_BUFFER, normalsBaseAddress, 4 * normals.limit(), normals
        )
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)

        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, indexBufferId)
        indexCount = indices.limit()
        GLES20.glBufferData(
            GLES20.GL_ELEMENT_ARRAY_BUFFER, 2 * indexCount, indices, GLES20.GL_STATIC_DRAW
        )
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0)
        ShaderUtil.checkGLError(TAG, "OBJ buffer load")
        Matrix.setIdentityM(modelMatrix, 0)
    }

    private fun readTexture(context: Context, diffuseTextureAssetName: String?) {
        val textureBitmap =
            BitmapFactory.decodeStream(context.assets.open(diffuseTextureAssetName!!))
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glGenTextures(textures.size, textures, 0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[0])
        GLES20.glTexParameteri(
            GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR_MIPMAP_LINEAR
        )
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, textureBitmap, 0)
        GLES20.glGenerateMipmap(GLES20.GL_TEXTURE_2D)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
        textureBitmap.recycle()
        ShaderUtil.checkGLError(TAG, "Texture loading")
    }


    fun setBlendMode(blendMode: BlendMode?) {
        this.blendMode = blendMode
    }


    @Throws(IOException::class)
    private fun compileAndLoadShaderProgram(context: Context) {
        val defineValuesMap: MutableMap<String, Int> = TreeMap()
        defineValuesMap[USE_DEPTH_FOR_OCCLUSION_SHADER_FLAG] = if (useDepthForOcclusion) 1 else 0
        val vertexShader =
            ShaderUtil.loadGLShader(TAG, context, GLES20.GL_VERTEX_SHADER, VERTEX_SHADER_NAME)
        val fragmentShader = ShaderUtil.loadGLShader(
            TAG, context, GLES20.GL_FRAGMENT_SHADER, FRAGMENT_SHADER_NAME, defineValuesMap
        )
        program = GLES20.glCreateProgram()
        GLES20.glAttachShader(program, vertexShader)
        GLES20.glAttachShader(program, fragmentShader)
        GLES20.glLinkProgram(program)
        GLES20.glUseProgram(program)
        ShaderUtil.checkGLError(TAG, "Program creation")
        modelViewUniform = GLES20.glGetUniformLocation(program, "u_ModelView")
        modelViewProjectionUniform = GLES20.glGetUniformLocation(program, "u_ModelViewProjection")
        positionAttribute = GLES20.glGetAttribLocation(program, "a_Position")
        normalAttribute = GLES20.glGetAttribLocation(program, "a_Normal")
        texCoordAttribute = GLES20.glGetAttribLocation(program, "a_TexCoord")
        textureUniform = GLES20.glGetUniformLocation(program, "u_Texture")
        lightingParametersUniform = GLES20.glGetUniformLocation(program, "u_LightingParameters")
        materialParametersUniform = GLES20.glGetUniformLocation(program, "u_MaterialParameters")
        colorCorrectionParameterUniform =
            GLES20.glGetUniformLocation(program, "u_ColorCorrectionParameters")
        colorUniform = GLES20.glGetUniformLocation(program, "u_ObjColor")


        if (useDepthForOcclusion) {
            depthTextureUniform = GLES20.glGetUniformLocation(program, "u_DepthTexture")
            depthUvTransformUniform = GLES20.glGetUniformLocation(program, "u_DepthUvTransform")
            depthAspectRatioUniform = GLES20.glGetUniformLocation(program, "u_DepthAspectRatio")
        }
        ShaderUtil.checkGLError(TAG, "Program parameters")
    }

    fun updateModelMatrix(modelMatrix: FloatArray?, scaleFactor: Float) {
        val scaleMatrix = FloatArray(16)
        Matrix.setIdentityM(scaleMatrix, 0)
        scaleMatrix[0] = scaleFactor
        scaleMatrix[5] = scaleFactor
        scaleMatrix[10] = scaleFactor
        Matrix.multiplyMM(this.modelMatrix, 0, modelMatrix, 0, scaleMatrix, 0)
    }

    fun setMaterialProperties(
        ambient: Float, diffuse: Float, specular: Float, specularPower: Float
    ) {
        this.ambient = ambient
        this.diffuse = diffuse
        this.specular = specular
        this.specularPower = specularPower
    }

    @JvmOverloads
    fun draw(
        cameraView: FloatArray?,
        cameraPerspective: FloatArray?,
        colorCorrectionRgba: FloatArray?,
        objColor: FloatArray? = DEFAULT_COLOR
    ) {
        ShaderUtil.checkGLError(TAG, "Before draw")

        Matrix.multiplyMM(modelViewMatrix, 0, cameraView, 0, modelMatrix, 0)
        Matrix.multiplyMM(modelViewProjectionMatrix, 0, cameraPerspective, 0, modelViewMatrix, 0)
        GLES20.glUseProgram(program)

        Matrix.multiplyMV(viewLightDirection, 0, modelViewMatrix, 0, LIGHT_DIRECTION, 0)
        normalizeVec3(viewLightDirection)
        GLES20.glUniform4f(
            lightingParametersUniform,
            viewLightDirection[0],
            viewLightDirection[1],
            viewLightDirection[2],
            1f
        )
        GLES20.glUniform4fv(colorCorrectionParameterUniform, 1, colorCorrectionRgba, 0)

        GLES20.glUniform4fv(colorUniform, 1, objColor, 0)

        GLES20.glUniform4f(materialParametersUniform, ambient, diffuse, specular, specularPower)

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[0])
        GLES20.glUniform1i(textureUniform, 0)

        if (useDepthForOcclusion) {
            GLES20.glActiveTexture(GLES20.GL_TEXTURE1)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, depthTextureId)
            GLES20.glUniform1i(depthTextureUniform, 1)

            GLES20.glUniformMatrix3fv(depthUvTransformUniform, 1, false, uvTransform, 0)
            GLES20.glUniform1f(depthAspectRatioUniform, depthAspectRatio)
        }

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vertexBufferId)
        GLES20.glVertexAttribPointer(
            positionAttribute, COORDS_PER_VERTEX, GLES20.GL_FLOAT, false, 0, verticesBaseAddress
        )
        GLES20.glVertexAttribPointer(
            normalAttribute,
            3,
            GLES20.GL_FLOAT,
            false,
            0,
            normalsBaseAddress
        )
        GLES20.glVertexAttribPointer(
            texCoordAttribute, 2, GLES20.GL_FLOAT, false, 0, texCoordsBaseAddress
        )
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)

        GLES20.glUniformMatrix4fv(modelViewUniform, 1, false, modelViewMatrix, 0)
        GLES20.glUniformMatrix4fv(
            modelViewProjectionUniform,
            1,
            false,
            modelViewProjectionMatrix,
            0
        )


        GLES20.glEnableVertexAttribArray(positionAttribute)
        GLES20.glEnableVertexAttribArray(normalAttribute)
        GLES20.glEnableVertexAttribArray(texCoordAttribute)
        if (blendMode != null) {
            GLES20.glEnable(GLES20.GL_BLEND)
            when (blendMode) {
                BlendMode.Shadow -> {
                    GLES20.glDepthMask(false)
                    GLES20.glBlendFunc(GLES20.GL_ZERO, GLES20.GL_ONE_MINUS_SRC_ALPHA)
                }
                BlendMode.AlphaBlending -> {
                    GLES20.glDepthMask(true)
                    GLES20.glBlendFunc(GLES20.GL_ONE, GLES20.GL_ONE_MINUS_SRC_ALPHA)
                }
            }
        }
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, indexBufferId)
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, indexCount, GLES20.GL_UNSIGNED_SHORT, 0)
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0)
        if (blendMode != null) {
            GLES20.glDisable(GLES20.GL_BLEND)
            GLES20.glDepthMask(true)
        }

        GLES20.glDisableVertexAttribArray(positionAttribute)
        GLES20.glDisableVertexAttribArray(normalAttribute)
        GLES20.glDisableVertexAttribArray(texCoordAttribute)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
        ShaderUtil.checkGLError(TAG, "After draw")
    }

    companion object {
        private val TAG = ObjectRenderer::class.java.simpleName

        private const val VERTEX_SHADER_NAME = "shaders/ar_object.vert"
        private const val FRAGMENT_SHADER_NAME = "shaders/ar_object.frag"
        private const val COORDS_PER_VERTEX = 3
        private val DEFAULT_COLOR = floatArrayOf(0f, 0f, 0f, 0f)

        private val LIGHT_DIRECTION = floatArrayOf(0.250f, 0.866f, 0.433f, 0.0f)

        private const val USE_DEPTH_FOR_OCCLUSION_SHADER_FLAG = "USE_DEPTH_FOR_OCCLUSION"
        private fun normalizeVec3(v: FloatArray) {
            val reciprocalLength =
                1.0f / sqrt((v[0] * v[0] + v[1] * v[1] + v[2] * v[2]).toDouble())
                    .toFloat()
            v[0] *= reciprocalLength
            v[1] *= reciprocalLength
            v[2] *= reciprocalLength
        }
    }
}
