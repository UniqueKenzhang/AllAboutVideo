package com.example.allaboutvideo.videofilter;

import android.opengl.GLES20;

import com.example.allaboutvideo.filter.BaseFilter;
import com.example.allaboutvideo.filter.GpuUtils;
import com.example.allaboutvideo.filter.ShaderHolder;
import com.example.allaboutvideo.simpleplay.SimplePlayActivity;

public class VideoFilterActivity extends SimplePlayActivity {


    private BaseFilter mBaseFilter;
    private BaseFilter mBlurFilterX;
    private BaseFilter mBlurFilterY;

    @Override
    public void createFilter() {
        mBlurFilterX = new BaseFilter(null, BaseFilter.BASE_VERT, ShaderHolder.fStr5) {

            private int mGLVideoWidth;
            private int mGLVideoHeight;

            @Override
            protected void onCreate() {
                super.onCreate();
                mGLVideoWidth = GLES20.glGetUniformLocation(mGLProgram, "aVideoWidth");
                mGLVideoHeight = GLES20.glGetUniformLocation(mGLProgram, "aVideoHeight");
            }

            @Override
            protected void onSetExpandData() {
                super.onSetExpandData();
                GLES20.glUniform1f(mGLVideoWidth, mediaPlayer.getVideoWidth());
                GLES20.glUniform1f(mGLVideoHeight, mediaPlayer.getVideoHeight());
            }
        };


        mBlurFilterY = new BaseFilter(null, BaseFilter.BASE_VERT, ShaderHolder.fStr6) {

            private int mGLVideoWidth;
            private int mGLVideoHeight;

            @Override
            protected void onCreate() {
                super.onCreate();
                mGLVideoWidth = GLES20.glGetUniformLocation(mGLProgram, "aVideoWidth");
                mGLVideoHeight = GLES20.glGetUniformLocation(mGLProgram, "aVideoHeight");
            }

            @Override
            protected void onSetExpandData() {
                super.onSetExpandData();
                GLES20.glUniform1f(mGLVideoWidth, mediaPlayer.getVideoWidth());
                GLES20.glUniform1f(mGLVideoHeight, mediaPlayer.getVideoHeight());
            }
        };
        mBlurFilterY.create();
        mBlurFilterX.create();

        mBaseFilter = new BaseFilter(null, BaseFilter.BASE_VERT, ShaderHolder.fStr) {

            @Override
            protected void onClear() {

            }
        };
        mBaseFilter.create();
    }

    @Override
    public void onSizeChanged(int w, int h) {
        mBaseFilter.sizeChanged(w, h);
        mBlurFilterX.sizeChanged(w, h);
        mBlurFilterY.sizeChanged(w, h);
    }

    @Override
    public void draw(int texture2d) {
        GLES20.glViewport(0, 0, mBaseFilter.mWidth, mBaseFilter.mHeight);
        int texture = mBlurFilterY.drawToTexture(texture2d);
        mBlurFilterX.draw(texture);
        GLES20.glViewport(0, mBaseFilter.mHeight / 3, mBaseFilter.mWidth, mBaseFilter.mHeight / 3);
        mBaseFilter.draw(texture2d);
    }
}
