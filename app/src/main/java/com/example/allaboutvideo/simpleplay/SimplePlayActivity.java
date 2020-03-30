package com.example.allaboutvideo.simpleplay;

import android.content.Intent;
import android.graphics.SurfaceTexture;
import android.media.MediaPlayer;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.view.Surface;

import androidx.appcompat.app.AppCompatActivity;

import com.example.allaboutvideo.R;
import com.example.allaboutvideo.filter.BaseFilter;
import com.example.allaboutvideo.filter.GpuUtils;
import com.example.allaboutvideo.filter.OesFilter;
import com.example.allaboutvideo.filter.ShaderHolder;

import java.io.IOException;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class SimplePlayActivity extends AppCompatActivity {

    public static final String VIDEO_PATH = "video_path";
    SurfaceTexture surfaceTexture;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gl_sur);


        final GLSurfaceView content = findViewById(R.id.content);

        content.setEGLConfigChooser(8, 8, 8, 8, 16, 0);
        content.setEGLContextClientVersion(2);
        content.setRenderer(new GLSurfaceView.Renderer() {


            private OesFilter mOesFilter;
            private BaseFilter mBaseFilter;
            private int[] oesTextures = new int[1];

            @Override
            public void onSurfaceCreated(GL10 gl10, EGLConfig eglConfig) {
//                content.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);

                GpuUtils.createTextureID(oesTextures, true);
                try {
                    Intent intent = getIntent();
                    String path = intent.getStringExtra(VIDEO_PATH);

                    final MediaPlayer mediaPlayer = new MediaPlayer();
                    mediaPlayer.setDataSource(path);
                    mediaPlayer.setVolume(0, 0);
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

                mBaseFilter = new BaseFilter(null, BaseFilter.BASE_VERT, ShaderHolder.fStr) {

                };
                mBaseFilter.create();
                mOesFilter = new OesFilter();
                mOesFilter.create();
            }

            @Override
            public void onSurfaceChanged(GL10 gl10, int w, int h) {
                mBaseFilter.sizeChanged(w, h);
                mOesFilter.sizeChanged(w, h);
            }

            @Override
            public void onDrawFrame(GL10 gl10) {
                surfaceTexture.updateTexImage();
                int texture2d = mOesFilter.drawToTexture(oesTextures[0]);
                mBaseFilter.draw(texture2d);
            }

        });

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (surfaceTexture != null)
            surfaceTexture.release();

    }
}
