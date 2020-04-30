package com.example.allaboutvideo.videofilter;

import android.opengl.GLES20;

import com.example.allaboutvideo.filter.BaseFilter;
import com.example.allaboutvideo.filter.ShaderHolder;
import com.example.allaboutvideo.simpleplay.SimplePlayActivity;

public class VideoFilterActivity extends SimplePlayActivity {


    private BaseFilter mBaseFilter;
    private BaseFilter mBlurFilter;

    @Override
    public void createFilter() {
        mBlurFilter = new BaseFilter(null, BaseFilter.BASE_VERT, ShaderHolder.fStr6) {

        };
        mBlurFilter.create();

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
        mBlurFilter.sizeChanged(w, h);
    }

    @Override
    public void draw(int texture2d) {
        GLES20.glViewport(0, 0, mBaseFilter.mWidth, mBaseFilter.mHeight);
        mBlurFilter.draw(texture2d);
        GLES20.glViewport(0, mBaseFilter.mHeight / 3, mBaseFilter.mWidth, mBaseFilter.mHeight / 3);
        mBaseFilter.draw(texture2d);
    }
}
