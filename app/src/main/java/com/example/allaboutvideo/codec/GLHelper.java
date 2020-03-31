package com.example.allaboutvideo.codec;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.opengl.Matrix;
import android.util.Log;
import android.widget.ImageView;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.Map;

import javax.microedition.khronos.opengles.GL10;

public class GLHelper {
    private static final String TAG = "wqy";
    private static final boolean isDebug = true;
    /**
     * 单位矩阵
     */
    public static final float[] OM = {
            1, 0, 0, 0,
            0, 1, 0, 0,
            0, 0, 1, 0,
            0, 0, 0, 1
    };
    //顶点坐标
    public static final float[] POSITION = {
            -1.0f, 1.0f,
            -1.0f, -1.0f,
            1.0f, 1.0f,
            1.0f, -1.0f,
    };

    //离屏时使用的顶点坐标
    public static final float[] FRAME_POSITION = {
            -1.0f, -1.0f,
            -1.0f, 1.0f,
            1.0f, -1.0f,
            1.0f, 1.0f,
    };
    //普通纹理坐标
    public static final float[] COORDINATE = {
            0.0f, 0.0f,
            0.0f, 1.0f,
            1.0f, 0.0f,
            1.0f, 1.0f,
    };
    //纹理坐标-前置摄像头
    public static final float[] CAMERA_FRONT_COORDINATE = {
            1.0f, 1.0f,
            0.0f, 1.0f,
            1.0f, 0.0f,
            0.0f, 0.0f,
    };
    //纹理坐标-后置摄像头
    public static final float[] CAMERA_BACK_COORDINATE = {
            0.0f, 1.0f,
            1.0f, 1.0f,
            0.0f, 0.0f,
            1.0f, 0.0f,
    };
    public static final int NO_TEXTURE = -1;

    //连接shader，获得程序句柄
    public static int glLoadProgram(String vertexSource, String fragmentSource) {
        int vertex = glLoadShader(GLES20.GL_VERTEX_SHADER, vertexSource);
        if (vertex == 0)
            return 0;
        int fragment = glLoadShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource);
        if (fragment == 0)
            return 0;
        int program = GLES20.glCreateProgram();
        if (program != 0) {
            GLES20.glAttachShader(program, vertex);
            GLES20.glAttachShader(program, fragment);
            GLES20.glLinkProgram(program);
            int[] linkStatus = new int[1];
            GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0);
            if (linkStatus[0] != GLES20.GL_TRUE) {
                String infoLog = GLES20.glGetProgramInfoLog(program);
                glLog("++could not link program:" + infoLog);
                GLES20.glDeleteProgram(program);
                throw new RuntimeException("Could not link program: " + infoLog);
            }
        }
        return program;
    }

    //加载shader
    public static int glLoadShader(int shaderType, String source) {
        int shader = GLES20.glCreateShader(shaderType);
        if (0 != shader) {
            GLES20.glShaderSource(shader, source);
            GLES20.glCompileShader(shader);
            int[] compiled = new int[1];
            GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0);
            if (compiled[0] == 0) {
                String infoLog = GLES20.glGetShaderInfoLog(shader);
                glLog("++could not compile shader:" + shaderType + "++Error:" + infoLog);
                GLES20.glDeleteShader(shader);
                throw new RuntimeException("Could not compile shader " + shaderType + ":" + infoLog);
            }
        }
        return shader;
    }

//    //通过路径加载Assets中的文本内容
//    public static String glLoadShaderRes(String path) {
//        Resources res = SDKManager.getResources();
//        StringBuilder result = new StringBuilder();
//        try {
//            InputStream is = res.getAssets().open(path);
//            int ch;
//            byte[] buffer = new byte[1024];
//            while (-1 != (ch = is.read(buffer))) {
//                result.append(new String(buffer, 0, ch));
//            }
//        } catch (Exception e) {
//            return null;
//        }
//        return result.toString().replaceAll("\\r\\n", "\n");
//    }

    public static void glLog(String msg) {
        if (isDebug) {
            Log.e(TAG, msg);
        }
    }

    //批量获取texture
    public static void getTextures(int size, int[] textures, int start, int gl_format, int width, int height) {
        GLES20.glGenTextures(size, textures, start);
        for (int index = 0; index < size; index++) {
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[index]);
            GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, gl_format, width, height, 0, gl_format, GLES20.GL_UNSIGNED_BYTE, null);

            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        }
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
    }

    public static int loadTexture(final Bitmap img, final int usedTexId, boolean needRecycle) {
        if (img == null || img.isRecycled())

            return NO_TEXTURE;

        int textures[] = new int[1];
        if (usedTexId == NO_TEXTURE) {
            int width = img.getWidth();

            int bytesPerRow = width * 32 / 8;

            if (bytesPerRow % 8 == 0) {
                GLES20.glPixelStorei(GLES20.GL_UNPACK_ALIGNMENT, 8);
            } else if (bytesPerRow % 4 == 0) {
                GLES20.glPixelStorei(GLES20.GL_UNPACK_ALIGNMENT, 4);
            } else if (bytesPerRow % 2 == 0) {
                GLES20.glPixelStorei(GLES20.GL_UNPACK_ALIGNMENT, 2);
            } else {
                GLES20.glPixelStorei(GLES20.GL_UNPACK_ALIGNMENT, 1);
            }

            GLES20.glGenTextures(1, textures, 0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[0]);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, img, 0);
        } else {
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, usedTexId);
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, img, 0);
            textures[0] = usedTexId;
        }
        if (needRecycle)
            img.recycle();
        return textures[0];
    }

    public static int loadTexture(final Bitmap img) {
        return loadTexture(img, -1, false);
    }

    //获取OES纹理，视频，摄像头播放使用
    public static int getExternalOESTexture() {
        int[] texture = new int[1];

        GLES20.glGenTextures(1, texture, 0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, texture[0]);
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_LINEAR);
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GL10.GL_TEXTURE_WRAP_S, GL10.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GL10.GL_TEXTURE_WRAP_T, GL10.GL_CLAMP_TO_EDGE);

        return texture[0];
    }

    //读入浮点数组到端数据栈
    public static FloatBuffer loadBuffer(FloatBuffer buffer, float[] data) {
        if (buffer == null || data.length > buffer.remaining()) {
            ByteBuffer bb = ByteBuffer.allocateDirect(data.length * Float.SIZE / 8);
            bb.order(ByteOrder.nativeOrder());
            buffer = bb.asFloatBuffer();
        } else {
            buffer.clear();
        }
        buffer.put(data);
        buffer.position(0);

        return buffer;
    }

    //读入浮点数组到端数据栈
    public static FloatBuffer loadBuffer(float[] data) {
        return loadBuffer(null, data);
    }

    public static ShortBuffer loadBuffer(ShortBuffer buffer, short[] data) {
        if (buffer == null || data.length > buffer.remaining()) {
            ByteBuffer bb = ByteBuffer.allocateDirect(data.length * Float.SIZE / 8);
            bb.order(ByteOrder.nativeOrder());
            buffer = bb.asShortBuffer();
        } else {
            buffer.clear();
        }
        buffer.put(data);
        buffer.position(0);

        return buffer;
    }

    public static ShortBuffer loadBuffer(short[] data) {
        return loadBuffer(null, data);
    }

    //缩放类型矩阵，居中，拉伸，填充等
    public static void setScaleTypeMatrix(float[] matrix, ImageView.ScaleType type, int imgWidth, int imgHeight, int viewWidth, int viewHeight) {
        if (imgHeight > 0 && imgWidth > 0 && viewWidth > 0 && viewHeight > 0) {
            float[] projection = new float[16];
            float[] camera = new float[16];
            if (type == ImageView.ScaleType.FIT_XY) {
                Matrix.orthoM(projection, 0, -1, 1, -1, 1, 1, 3);
                Matrix.setLookAtM(camera, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0);
                Matrix.multiplyMM(matrix, 0, projection, 0, camera, 0);
                return;
            }
            float sWhView = (float) viewWidth / viewHeight;
            float sWhImg = (float) imgWidth / imgHeight;
            if (sWhImg > sWhView) {
                switch (type) {
                    case CENTER_CROP:
                        Matrix.orthoM(projection, 0, -sWhView / sWhImg, sWhView / sWhImg, -1, 1, 1, 3);
                        break;
                    case CENTER_INSIDE:
                        Matrix.orthoM(projection, 0, -1, 1, -sWhImg / sWhView, sWhImg / sWhView, 1, 3);
                        break;
                    case FIT_START:
                        Matrix.orthoM(projection, 0, -1, 1, 1 - 2 * sWhImg / sWhView, 1, 1, 3);
                        break;
                    case FIT_END:
                        Matrix.orthoM(projection, 0, -1, 1, -1, 2 * sWhImg / sWhView - 1, 1, 3);
                        break;
                }
            } else {
                switch (type) {
                    case CENTER_CROP:
                        Matrix.orthoM(projection, 0, -1, 1, -sWhImg / sWhView, sWhImg / sWhView, 1, 3);
                        break;
                    case CENTER_INSIDE:
                        Matrix.orthoM(projection, 0, -sWhView / sWhImg, sWhView / sWhImg, -1, 1, 1, 3);
                        break;
                    case FIT_START:
                        Matrix.orthoM(projection, 0, -1, 2 * sWhView / sWhImg - 1, -1, 1, 1, 3);
                        break;
                    case FIT_END:
                        Matrix.orthoM(projection, 0, 1 - 2 * sWhView / sWhImg, 1, -1, 1, 1, 3);
                        break;
                }
            }
            Matrix.setLookAtM(camera, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0);
            Matrix.multiplyMM(matrix, 0, projection, 0, camera, 0);
        }
    }

    public static void checkGlError(String op) {
        boolean hasError = false;
        int error = 0;
        while ((error = GLES20.glGetError()) != 0) {
            hasError = true;
            Log.e("RendererUtils", op + ": glError " + error + " errorInfo : " + GLUtils.getEGLErrorString(error));
        }
        if (hasError) {
            Map<Thread, StackTraceElement[]> ts = Thread.getAllStackTraces();
            StackTraceElement[] ste = ts.get(Thread.currentThread());

            for (StackTraceElement s : ste) {
                Log.e("RendererUtils", s.toString());
            }
        }

    }

    public static int[] getFrameBuffers(int size) {
        int[] frames = new int[size];
        genFrameBuffers(frames);
        return frames;
    }

    public static void genFrameBuffers(int[] frames) {
        GLES20.glGenFramebuffers(frames.length, frames, 0);
    }

    public static int[] get2DTextures(int size) {
        int[] textures = new int[size];
        gen2DTextures(textures);
        return textures;
    }

    public static void gen2DTextures(int[] textures) {
        GLES20.glGenTextures(textures.length, textures, 0);
        for (int texture : textures) {
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        }
    }
}
