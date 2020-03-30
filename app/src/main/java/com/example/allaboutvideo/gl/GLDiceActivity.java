package com.example.allaboutvideo.gl;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PixelFormat;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.GLUtils;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.allaboutvideo.R;
import com.example.allaboutvideo.filter.MatrixHelper;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Created by kenzhang on 2017/10/10.
 */

public class GLDiceActivity extends AppCompatActivity {

    float x = 0.5f, y = 0.5f, z = 0.5f;
    private int[] textures = new int[1];
    private float[] vertices = {
            -x, y, z, // 0, Top Left
            -x, -y, z, // 1, Bottom Left
            x, y, z, // 2, Top Right
            x, -y, z, // 3, Bottom Right
            x, y, -z,//4
            x, -y, -z,//5
            -x, y, -z,//6
            -x, -y, -z,//7
            -x, y, z, // 8, Top Left
            -x, -y, z, // 9, Bottom Left

            -x, y, z, // 10, Top Left
            x, y, z, // 11, Top Right
            x, y, -z,//12
            -x, y, -z,//13

            -x, -y, z, // 14, Bottom Left
            x, -y, z, // 15, Bottom Right
            x, -y, -z,//16
            -x, -y, -z,//17
    };

    private short[] indices = {
            0, 1, 2,
            1, 3, 2,
            2, 3, 4,
            3, 5, 4,
            4, 5, 6,
            5, 7, 6,
            6, 7, 8,
            7, 9, 8,
            10, 11, 13,
            11, 12, 13,
            17, 15, 14,
            17, 16, 15,
    };

//     0,2,6,
//             2,6,4,
//             1,3,5,
//             3,5,7

    float textureCoordinates[] = {
            0.0f, 0.0f,//0
            1.0f, 0.0f,//1
            0.0f, 0.167f,//2
            1.0f, 0.167f,//3
            0.0f, 0.333f,//4
            1.0f, 0.333f,//5
            0.0f, 0.5f,//6
            1.0f, 0.5f,//7

            0.0f, 0.667f,//8
            1.0f, 0.667f,//9

            0.0f, 0.667f,//0
            1.0f, 0.667f,//1
            1.0f, 0.833f,//3
            0.0f, 0.833f,//2

            0.0f, 0.833f,//0
            1.0f, 0.833f,//1
            1.0f, 1f,//3
            0.0f, 1f,//2
    };


    private FloatBuffer vertexBuffer;
    private ShortBuffer indexBuffer;
    private FloatBuffer textureBuffer;
    private Bitmap bitmap;
    private float tangleY;
    private float tangleX;
    private GLSurfaceView surface;


    String vStr =
            "attribute vec4 vPosition;" +
                    "attribute vec2 vCoordinate;" +
                    "varying vec2 aCoordinate;" +
                    "uniform mat4 vMatrix;" +
                    "void main() { " +
                    "   gl_Position = vMatrix*vPosition;" +
                    "   aCoordinate=vCoordinate;" +
                    "}";
    String fStr =
            " precision mediump float; " +
                    "uniform sampler2D vTexture; " +
                    "varying vec2 aCoordinate;  " +
                    "void main() {" +
                    "   gl_FragColor=texture2D(vTexture,aCoordinate);" +
                    "}";
    private int mProgram;
    private VelocityTracker mTracker;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gl_sur);
        surface = (GLSurfaceView) findViewById(R.id.content);
        surface.setEGLConfigChooser(8, 8, 8, 8, 16, 0);
        surface.getHolder().setFormat(PixelFormat.TRANSLUCENT);
        surface.setZOrderOnTop(true);
        View root = findViewById(R.id.root);

        mTracker = VelocityTracker.obtain();

        root.setOnTouchListener(new View.OnTouchListener() {
            private static final float PI = 3.1415926f;
            public float velocityY;
            public float velocityX;
            private float y;
            private float x;
            private float tangleX0;
            private float tangleY0;
            private Handler handler = new Handler();

            Runnable scorllLastTask = new Runnable() {

                @Override
                public void run() {
                    float detaY = velocityY * 0.05f;
                    float detaX = velocityX * 0.05f;
                    tangleX += detaY * 360f / 1080f;
                    tangleY += detaX * 360f / 1080f;
                    MatrixHelper.setIdentityM();
                    MatrixHelper.rotate(1, 0, 0, tangleX);
                    MatrixHelper.rotate(0, 1, 0, tangleY);
                    surface.requestRender();
                    velocityY = velocityY / 1.05f;
                    velocityX = velocityX / 1.05f;
                    if (Math.abs(velocityY) < 50 && Math.abs(velocityX) < 50) {

                    } else {
                        handler.postDelayed(this, 16);
                    }
                }
            };

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                mTracker.addMovement(event);
                int action = event.getAction();
                switch (action) {
                    case MotionEvent.ACTION_DOWN:
                        handler.removeCallbacks(scorllLastTask);
                        y = event.getY();
                        tangleX0 = tangleX;
                        x = event.getX();
                        tangleY0 = tangleY;
                        break;
                    case MotionEvent.ACTION_MOVE:
                        float detaY = event.getY() - y;
                        float detaX = event.getX() - x;
                        tangleX = tangleX0 + detaY * 360f / 1080f;
                        tangleY = tangleY0 + detaX * 360f / 1080f;
                        MatrixHelper.setIdentityM();
                        MatrixHelper.rotate(1, 0, 0, tangleX);
                        MatrixHelper.rotate(0, 1, 0, tangleY);
                        surface.requestRender();
                        break;
                    case MotionEvent.ACTION_UP:

                        mTracker.computeCurrentVelocity(1000);
                        velocityX = mTracker.getXVelocity();
                        velocityY = mTracker.getYVelocity();
                        handler.postDelayed(scorllLastTask, 50);
                        break;
                }
                return true;
            }
        });

        bitmap = BitmapFactory.decodeResource(getResources(), R.mipmap.dice);

        surface.setEGLContextClientVersion(2);
        // vertices with 4.
        ByteBuffer vbb = ByteBuffer.allocateDirect(vertices.length * 4);
        vbb.order(ByteOrder.nativeOrder());
        vertexBuffer = vbb.asFloatBuffer();
        vertexBuffer.put(vertices);
        vertexBuffer.position(0);

        // To gain some performance we also put this ones in a byte buffer.
        // short is 2 bytes, therefore we multiply the number if vertices with
        ByteBuffer ibb = ByteBuffer.allocateDirect(indices.length * 2);
        ibb.order(ByteOrder.nativeOrder());
        indexBuffer = ibb.asShortBuffer();
        indexBuffer.put(indices);
        indexBuffer.position(0);


        ByteBuffer byteBuf = ByteBuffer.allocateDirect(textureCoordinates.length * 4);
        byteBuf.order(ByteOrder.nativeOrder());
        textureBuffer = byteBuf.asFloatBuffer();
        textureBuffer.put(textureCoordinates);
        textureBuffer.position(0);


        surface.setRenderer(new GLSurfaceView.Renderer() {

            private ByteBuffer byteBuffer;
            private int width;
            private int height;
            public int glMatrix;
            public int glHCoordinate;
            public int glTexture;
            public int fragmentShader;
            public int vertexShader;
            public int glPosition;
            private int[] compile = new int[1];

            @Override
            public void onSurfaceCreated(GL10 gl, EGLConfig config) {

                int texture = createTexture();
                GLES20.glEnable(GLES20.GL_CULL_FACE);
                GLES20.glEnable(GLES20.GL_DEPTH_TEST);
                vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vStr);
                fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fStr);
                //创建一个空的OpenGLES程序
                mProgram = GLES20.glCreateProgram();
                //将顶点着色器加入到程序
                GLES20.glAttachShader(mProgram, vertexShader);
                //将片元着色器加入到程序中
                GLES20.glAttachShader(mProgram, fragmentShader);
                //连接到着色器程序
                GLES20.glLinkProgram(mProgram);

                //获取顶点着色器的vPosition成员句柄
                //启用三角形顶点的句柄
                glPosition = GLES20.glGetAttribLocation(mProgram, "vPosition");
                //
                glTexture = GLES20.glGetUniformLocation(mProgram, "vTexture");
                //世界矩阵
                glMatrix = GLES20.glGetUniformLocation(mProgram, "vMatrix");
                //texture 顶点
                glHCoordinate = GLES20.glGetAttribLocation(mProgram, "vCoordinate");

                surface.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
            }

            @Override
            public void onSurfaceChanged(GL10 gl, int width, int height) {
                this.width = width;
                this.height = height;
                this.byteBuffer = ByteBuffer.allocate(width * height * 4);
                float ratio = (float) height / width;
                GLES20.glViewport(0, 0, width, height);
                MatrixHelper.setProjectFrustum(-1, 1, -ratio, ratio, 1f, 20f);
            }

            @Override
            public void onDrawFrame(GL10 gl) {
                GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

                GLES20.glUseProgram(mProgram);

                //准备三角形的坐标数据
                GLES20.glEnableVertexAttribArray(glPosition);
                GLES20.glVertexAttribPointer(glPosition, 3, GLES20.GL_FLOAT, false, 0, vertexBuffer);
                //输入矩阵
                GLES20.glUniformMatrix4fv(glMatrix, 1, false, MatrixHelper.getFinalMatrix(), 0);
                //输入纹理
                GLES20.glUniform1i(glTexture, 0);
                //
                GLES20.glEnableVertexAttribArray(glHCoordinate);
                GLES20.glVertexAttribPointer(glHCoordinate, 2, GLES20.GL_FLOAT, false, 0, textureBuffer);

                GLES20.glDrawElements(GLES20.GL_TRIANGLES, indices.length, GLES20.GL_UNSIGNED_SHORT, indexBuffer);
//                GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 10);
                //禁止顶点数组的句柄
                GLES20.glDisableVertexAttribArray(glPosition);
                GLES20.glDisableVertexAttribArray(glHCoordinate);

                GLES20.glReadPixels(0, 0, width, height, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, byteBuffer);
            }

            private int loadShader(int type, String str) {
//                1:创建Shader glCreateShader
//                2:指定Shader源代码 glShaderSource
//                3:编译Shader glCompileShader
//                4:获取shader状态 glGetShaderiv
//                5:如果出错 就 获取shader日志信息 glGetShaderInfoLog
                int id = GLES20.glCreateShader(type);
                GLES20.glShaderSource(id, str);
                GLES20.glCompileShader(id);
                GLES20.glGetShaderiv(id, GLES20.GL_COMPILE_STATUS, compile, 0);
                if (compile[0] == GLES20.GL_FALSE) {
                    Log.e("ES20_ERROR", GLES20.glGetShaderInfoLog(id));
                    GLES20.glDeleteShader(id);
                    return -1;
                } else {
                    return id;
                }
            }

            private int createTexture() {
                int[] texture = new int[1];
                if (bitmap != null && !bitmap.isRecycled()) {
                    //生成纹理
                    GLES20.glGenTextures(1, texture, 0);
                    //生成纹理
                    GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture[0]);
                    //设置缩小过滤为使用纹理中坐标最接近的一个像素的颜色作为需要绘制的像素颜色
                    GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
                    //设置放大过滤为使用纹理中坐标最接近的若干个颜色，通过加权平均算法得到需要绘制的像素颜色
                    GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
                    //设置环绕方向S，截取纹理坐标到[1/2n,1-1/2n]。将导致永远不会与border融合
                    GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
                    //设置环绕方向T，截取纹理坐标到[1/2n,1-1/2n]。将导致永远不会与border融合
                    GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
                    //根据以上指定的参数，生成一个2D纹理
                    GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);
                    return texture[0];
                }
                return 0;
            }
        });

    }
}
