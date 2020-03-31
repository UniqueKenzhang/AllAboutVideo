package com.example.allaboutvideo.codec;

import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.opengl.EGL14;
import android.opengl.GLES20;
import android.opengl.GLUtils;

import com.example.allaboutvideo.filter.LogoFilter;

import javax.microedition.khronos.opengles.GL10;

@TargetApi(18)
public class BaseMixTask extends Thread {

    protected InputSurface encodeSurface;
    protected MyRecorder myRecorder;
    protected Bitmap logo;
    protected int height;
    protected int width;


    protected BaseMixTask(MyRecorder myRecorder) {
        super("mixTask");
        this.myRecorder = myRecorder;
        this.height = myRecorder.getFrameHeight();
        this.width = myRecorder.getFrameWidth();
    }


    protected void initEgl() {
        if (myRecorder.getInputSurface() != null) {
            encodeSurface = new InputSurface(EGL14.EGL_NO_CONTEXT, myRecorder.getInputSurface());
            encodeSurface.makeCurrent();
        }
    }

    protected LogoFilter initBitmapFilter(Bitmap bitmap) {
        LogoFilter showFilter = null;
        if (bitmap != null) {
            showFilter = new LogoFilter();
            showFilter.create();
            showFilter.setLogoTex(initBitmapTex(bitmap));
            showFilter.sizeChanged(bitmap.getWidth(), bitmap.getHeight());
            bitmap.recycle();
        }
        return showFilter;
    }

    void drawLogo(LogoFilter showFilter) {
        if (showFilter != null && showFilter.getTex() > 0) {
            GLES20.glViewport(width - showFilter.mWidth - 14, 14, showFilter.mWidth, showFilter.mHeight);
//            GLES20.glViewport(0, 0, width, height);
            showFilter.draw();
        }
    }

    void releaseLogo(LogoFilter showFilter) {
        if (showFilter != null && showFilter.getTex() > 0) {
            GLES20.glDeleteTextures(0, new int[]{showFilter.getTex()}, 0);
        }
    }

    protected int initBitmapTex(Bitmap btimap) {
        int[] textures = new int[1];

        GLES20.glGenTextures(textures.length, textures, 0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[0]);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_NEAREST);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_NEAREST);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_S, GL10.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_T, GL10.GL_CLAMP_TO_EDGE);
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, btimap, 0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);

        return textures[0];
    }
}
