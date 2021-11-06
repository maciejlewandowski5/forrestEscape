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
import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.util.Log
import com.google.ar.core.Coordinates2d
import com.google.ar.core.Frame
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.lang.RuntimeException
import java.lang.StringBuilder
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.util.*

class BackgroundRenderer {
    private var quadCoords: FloatBuffer? = null
    private var quadTexCoords: FloatBuffer? = null
    private var cameraProgram = 0
    private var depthProgram = 0
    private var cameraPositionAttrib = 0
    private var cameraTexCoordAttrib = 0
    private var cameraTextureUniform = 0
    var textureId = -1
        private set
    private var suppressTimestampZeroRendering = true
    private var depthPositionAttrib = 0
    private var depthTexCoordAttrib = 0
    private var depthTextureUniform = 0
    private var depthTextureId = -1

    @JvmOverloads
    @Throws(IOException::class)
    fun createOnGlThread(context: Context, depthTextureId: Int = -1) {
        generateBackgroundTexture()
        loadRenderCameraFeedShader(context)
        loadDepthMapShader(context)
        this.depthTextureId = depthTextureId
    }

    private fun loadDepthMapShader(context: Context) {
        run {
            val vertexShader: Int = ShaderUtil.loadGLShader(
                TAG,
                context,
                GLES20.GL_VERTEX_SHADER,
                DEPTH_VISUALIZER_VERTEX_SHADER_NAME
            )
            val fragmentShader: Int = ShaderUtil.loadGLShader(
                TAG,
                context,
                GLES20.GL_FRAGMENT_SHADER,
                DEPTH_VISUALIZER_FRAGMENT_SHADER_NAME
            )
            depthProgram = GLES20.glCreateProgram()
            GLES20.glAttachShader(depthProgram, vertexShader)
            GLES20.glAttachShader(depthProgram, fragmentShader)
            GLES20.glLinkProgram(depthProgram)
            GLES20.glUseProgram(depthProgram)
            depthPositionAttrib = GLES20.glGetAttribLocation(depthProgram, "a_Position")
            depthTexCoordAttrib = GLES20.glGetAttribLocation(depthProgram, "a_TexCoord")
            ShaderUtil.checkGLError(TAG, "Program creation")
            depthTextureUniform = GLES20.glGetUniformLocation(depthProgram, "u_DepthTexture")
            ShaderUtil.checkGLError(TAG, "Program parameters")
        }
    }

    private fun loadRenderCameraFeedShader(context: Context) {
        run {
            val vertexShader: Int = ShaderUtil.loadGLShader(
                TAG,
                context,
                GLES20.GL_VERTEX_SHADER,
                CAMERA_VERTEX_SHADER_NAME
            )
            val fragmentShader: Int = ShaderUtil.loadGLShader(
                TAG,
                context,
                GLES20.GL_FRAGMENT_SHADER,
                CAMERA_FRAGMENT_SHADER_NAME
            )
            cameraProgram = GLES20.glCreateProgram()
            GLES20.glAttachShader(cameraProgram, vertexShader)
            GLES20.glAttachShader(cameraProgram, fragmentShader)
            GLES20.glLinkProgram(cameraProgram)
            GLES20.glUseProgram(cameraProgram)
            cameraPositionAttrib = GLES20.glGetAttribLocation(cameraProgram, "a_Position")
            cameraTexCoordAttrib = GLES20.glGetAttribLocation(cameraProgram, "a_TexCoord")
            ShaderUtil.checkGLError(TAG, "Program creation")
            cameraTextureUniform = GLES20.glGetUniformLocation(cameraProgram, "sTexture")
            ShaderUtil.checkGLError(TAG, "Program parameters")
        }
    }

    private fun generateBackgroundTexture() {
        val textures = IntArray(1)
        GLES20.glGenTextures(1, textures, 0)
        textureId = textures[0]
        val textureTarget = GLES11Ext.GL_TEXTURE_EXTERNAL_OES
        GLES20.glBindTexture(textureTarget, textureId)
        GLES20.glTexParameteri(textureTarget, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(textureTarget, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(textureTarget, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(textureTarget, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        val numVertices = 4
        if (numVertices != QUAD_COORDS.size / COORDS_PER_VERTEX) {
            throw RuntimeException("Unexpected number of vertices in BackgroundRenderer.")
        }
        val bbCoords = ByteBuffer.allocateDirect(QUAD_COORDS.size * FLOAT_SIZE)
        bbCoords.order(ByteOrder.nativeOrder())
        quadCoords = bbCoords.asFloatBuffer()
        quadCoords!!.put(QUAD_COORDS)
        quadCoords!!.position(0)
        val bbTexCoordsTransformed =
            ByteBuffer.allocateDirect(numVertices * TEXCOORDS_PER_VERTEX * FLOAT_SIZE)
        bbTexCoordsTransformed.order(ByteOrder.nativeOrder())
        quadTexCoords = bbTexCoordsTransformed.asFloatBuffer()
    }


    @JvmOverloads
    fun draw(frame: Frame, debugShowDepthMap: Boolean = false) {
        if (frame.hasDisplayGeometryChanged()) {
            frame.transformCoordinates2d(
                Coordinates2d.OPENGL_NORMALIZED_DEVICE_COORDINATES,
                quadCoords,
                Coordinates2d.TEXTURE_NORMALIZED,
                quadTexCoords
            )
        }
        if (frame.timestamp == 0L && suppressTimestampZeroRendering) {
            return
        }
        draw(debugShowDepthMap)
    }

    private fun draw(debugShowDepthMap: Boolean) {
        quadTexCoords!!.position(0)

        GLES20.glDisable(GLES20.GL_DEPTH_TEST)
        GLES20.glDepthMask(false)
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        if (debugShowDepthMap) {
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, depthTextureId)
            GLES20.glUseProgram(depthProgram)
            GLES20.glUniform1i(depthTextureUniform, 0)


            GLES20.glVertexAttribPointer(
                depthPositionAttrib, COORDS_PER_VERTEX, GLES20.GL_FLOAT, false, 0, quadCoords
            )
            GLES20.glVertexAttribPointer(
                depthTexCoordAttrib, TEXCOORDS_PER_VERTEX, GLES20.GL_FLOAT, false, 0, quadTexCoords
            )
            GLES20.glEnableVertexAttribArray(depthPositionAttrib)
            GLES20.glEnableVertexAttribArray(depthTexCoordAttrib)
        } else {
            GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId)
            GLES20.glUseProgram(cameraProgram)
            GLES20.glUniform1i(cameraTextureUniform, 0)

            GLES20.glVertexAttribPointer(
                cameraPositionAttrib, COORDS_PER_VERTEX, GLES20.GL_FLOAT, false, 0, quadCoords
            )
            GLES20.glVertexAttribPointer(
                cameraTexCoordAttrib, TEXCOORDS_PER_VERTEX, GLES20.GL_FLOAT, false, 0, quadTexCoords
            )
            GLES20.glEnableVertexAttribArray(cameraPositionAttrib)
            GLES20.glEnableVertexAttribArray(cameraTexCoordAttrib)
        }
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)

        if (debugShowDepthMap) {
            GLES20.glDisableVertexAttribArray(depthPositionAttrib)
            GLES20.glDisableVertexAttribArray(depthTexCoordAttrib)
        } else {
            GLES20.glDisableVertexAttribArray(cameraPositionAttrib)
            GLES20.glDisableVertexAttribArray(cameraTexCoordAttrib)
        }

        GLES20.glDepthMask(true)
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        ShaderUtil.checkGLError(TAG, "BackgroundRendererDraw")
    }

    companion object {
        private val TAG = BackgroundRenderer::class.java.simpleName

        private const val CAMERA_VERTEX_SHADER_NAME = "shaders/screenquad.vert"
        private const val CAMERA_FRAGMENT_SHADER_NAME = "shaders/screenquad.frag"
        private const val DEPTH_VISUALIZER_VERTEX_SHADER_NAME =
            "shaders/background_show_depth_color_visualization.vert"
        private const val DEPTH_VISUALIZER_FRAGMENT_SHADER_NAME =
            "shaders/background_show_depth_color_visualization.frag"
        private const val COORDS_PER_VERTEX = 2
        private const val TEXCOORDS_PER_VERTEX = 2
        private const val FLOAT_SIZE = 4

        private val QUAD_COORDS = floatArrayOf(
            -1.0f, -1.0f, +1.0f, -1.0f, -1.0f, +1.0f, +1.0f, +1.0f
        )
    }
}

object ShaderUtil {
    @Throws(IOException::class)
    fun loadGLShader(
        tag: String?,
        context: Context,
        type: Int,
        filename: String,
        defineValuesMap: Map<String, Int>
    ): Int {
        var code = readShaderFileFromAssets(context, filename)

        var defines = ""
        for ((key, value) in defineValuesMap) {
            defines += """#define $key $value
"""
        }
        code = defines + code
        var shader = compileShaderCode(type, code)
        val compileStatus = getCompilationStatus(shader)
        shader = cleanUpIfError(compileStatus, tag, shader)
        return shader
    }

    private fun cleanUpIfError(
        compileStatus: IntArray,
        tag: String?,
        shader: Int
    ): Int {
        var shader1 = shader
        if (compileStatus[0] == 0) {
            Log.e(tag, "Error compiling shader: " + GLES20.glGetShaderInfoLog(shader1))
            GLES20.glDeleteShader(shader1)
            shader1 = 0
        }
        if (shader1 == 0) {
            throw RuntimeException("Error creating shader.")
        }
        return shader1
    }

    private fun getCompilationStatus(shader: Int): IntArray {
        val compileStatus = IntArray(1)
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compileStatus, 0)
        return compileStatus
    }

    private fun compileShaderCode(type: Int, code: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, code)
        GLES20.glCompileShader(shader)
        return shader
    }

    /** Overload of loadGLShader that assumes no additional #define values to add.  */
    @Throws(IOException::class)
    fun loadGLShader(tag: String?, context: Context, type: Int, filename: String): Int {
        val emptyDefineValuesMap: Map<String, Int> = TreeMap()
        return loadGLShader(tag, context, type, filename, emptyDefineValuesMap)
    }

    /**
     * Checks if we've had an error inside of OpenGL ES, and if so what that error is.
     *
     * @param label Label to report in case of error.
     * @throws RuntimeException If an OpenGL error is detected.
     */
    fun checkGLError(tag: String?, label: String) {
        var lastError = GLES20.GL_NO_ERROR
        // Drain the queue of all errors.
        var error: Int
        while (GLES20.glGetError().also { error = it } != GLES20.GL_NO_ERROR) {
            Log.e(tag, "$label: glError $error")
            lastError = error
        }
        if (lastError != GLES20.GL_NO_ERROR) {
            throw RuntimeException("$label: glError $lastError")
        }
    }

    /**
     * Converts a raw shader file into a string.
     *
     * @param filename The filename of the shader file about to be turned into a shader.
     * @return The context of the text file, or null in case of error.
     */
    @Throws(IOException::class)
    private fun readShaderFileFromAssets(context: Context, filename: String): String {
        context.assets.open(filename).use { inputStream ->
            BufferedReader(InputStreamReader(inputStream)).use { reader ->
                val sb = StringBuilder()
                var line: String?
                Log.i("TAG", filename)
                while (reader.readLine().also { line = it } != null) {
                    val tokens =
                        line!!.split(" ").dropLastWhile { it.isEmpty() }
                            .toTypedArray()
                    if (tokens.isNotEmpty() && tokens[0] == "#include") {
                        var includeFilename = tokens[1]
                        includeFilename = includeFilename.replace("\"", "")
                        if (includeFilename == filename) {
                            throw IOException("Do not include the calling file.")
                        }
                        sb.append(readShaderFileFromAssets(context, includeFilename))
                    } else {
                        sb.append(line).append("\n")
                    }
                }
                return sb.toString()
            }
        }
    }
}
