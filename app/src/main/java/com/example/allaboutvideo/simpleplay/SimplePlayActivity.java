package com.example.allaboutvideo.simpleplay;

import android.graphics.SurfaceTexture;
import android.media.MediaPlayer;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.view.Surface;

import androidx.appcompat.app.AppCompatActivity;

import com.example.allaboutvideo.R;
import com.example.allaboutvideo.base.PageParam;
import com.example.allaboutvideo.filter.BaseFilter;
import com.example.allaboutvideo.filter.GpuUtils;
import com.example.allaboutvideo.filter.OesFilter;
import com.example.allaboutvideo.filter.ShaderHolder;

import java.io.IOException;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class SimplePlayActivity extends AppCompatActivity {

    SurfaceTexture surfaceTexture;
    protected String mPath;
    protected MediaPlayer mediaPlayer;
    private OesFilter mOesFilter;
    private BaseFilter mBaseFilter;
    private int[] oesTextures = new int[1];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gl_sur);
        mPath = getIntent().getStringExtra(PageParam.VIDEO_PATH);

        final GLSurfaceView content = findViewById(R.id.content);

        content.setEGLConfigChooser(8, 8, 8, 8, 16, 0);
        content.setEGLContextClientVersion(2);
        content.setRenderer(new GLSurfaceView.Renderer() {


            @Override
            public void onSurfaceCreated(GL10 gl10, EGLConfig eglConfig) {
//                content.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);

                GpuUtils.createTextureID(oesTextures, true);
                try {
                    mediaPlayer = new MediaPlayer();
                    mediaPlayer.setDataSource(mPath);
                    mediaPlayer.setLooping(true);
                    mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                        @Override
                        public void onPrepared(MediaPlayer mp) {
                            surfaceTexture = new SurfaceTexture(oesTextures[0]);
                            mediaPlayer.setSurface(new Surface(surfaceTexture));
                            mediaPlayer.start();
                        }
                    });

                    mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                        @Override
                        public void onCompletion(MediaPlayer mp) {
                            mediaPlayer.start();
                        }
                    });
                    mediaPlayer.prepare();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                createFilter();
                mOesFilter = new OesFilter();
                mOesFilter.create();
            }

            @Override
            public void onSurfaceChanged(GL10 gl10, int w, int h) {
                onSizeChanged(w, h);
                mOesFilter.sizeChanged(w, h);
            }

            @Override
            public void onDrawFrame(GL10 gl10) {
                surfaceTexture.updateTexImage();
                GLES20.glViewport(0, 0, mOesFilter.mWidth, mOesFilter.mHeight);
                int texture2d = mOesFilter.drawToTexture(oesTextures[0]);
                draw(texture2d);
            }

        });
    }

    public void onSizeChanged(int w, int h) {
        mBaseFilter.sizeChanged(w, h);

    }

    public void createFilter() {
        mBaseFilter = new BaseFilter(null, BaseFilter.BASE_VERT, ShaderHolder.fStr) {

        };
        mBaseFilter.create();

    }

    public void draw(int texture2d) {
        mBaseFilter.draw(texture2d);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            if (surfaceTexture != null)
                surfaceTexture.release();

            if (mediaPlayer != null) {
                mediaPlayer.stop();
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (mediaPlayer != null) {
                mediaPlayer.release();
            }
        }


    }
}
