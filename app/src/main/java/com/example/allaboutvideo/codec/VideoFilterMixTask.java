package com.example.allaboutvideo.codec;

import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.os.Build;
import android.util.Log;
import android.view.Surface;
import android.widget.ImageView;

import androidx.annotation.RequiresApi;

import com.example.allaboutvideo.filter.BaseFilter;
import com.example.allaboutvideo.filter.GpuUtils;
import com.example.allaboutvideo.filter.LogoFilter;
import com.example.allaboutvideo.filter.MatrixHelper;
import com.example.allaboutvideo.filter.OesContinueFilter;
import com.example.allaboutvideo.filter.ShaderHolder;
import com.example.allaboutvideo.filter.ViewPort;

import javax.microedition.khronos.opengles.GL10;


@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
public class VideoFilterMixTask extends BaseMixTask {

    public static int TIME_SCALE = 33;
    private final ProcessListener l;
    private final VideoSource source;
    private final int rotate;
    private long mPts;
    private boolean mRuning = true;

    public VideoFilterMixTask(VideoSource source, MyRecorder myRecorder, Bitmap logo, int rotate, ProcessListener l) {
        super(myRecorder);
        this.source = source;
        this.l = l;
        this.logo = logo;
        this.rotate = rotate;
    }

    public void stopMix() {
        mRuning = false;
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    public void run() {
        try {
            long start = System.currentTimeMillis();
            initEgl();//Egl环境
            myRecorder.startManual();
            l.onStart();

            clearCanvas();

            LogoFilter showFilter = initBitmapFilter(logo);
            int[] textures = new int[1];
            GpuUtils.createTextureID(textures, true);
            int texture = textures[0];

            GLES20.glEnable(GL10.GL_BLEND);
            GLES20.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);

            source.filter = new OesContinueFilter();
            source.filter.create();
            source.filter.sizeChanged(source.w, source.h);

            BaseFilter nor = new BaseFilter(null, BaseFilter.BASE_VERT, ShaderHolder.fStr) {

            };
            nor.create();
            nor.sizeChanged(width, height);

            float[] matrix = MatrixHelper.getIdentityM();

            int videoHeight = source.h, videoWidth = source.w;

            int rotate = source.rotate + this.rotate;
            if (rotate == 90 || rotate == 270) {
                videoHeight = source.w;
                videoWidth = source.h;
            }

            GLHelper.setScaleTypeMatrix(matrix,
                    ImageView.ScaleType.CENTER_INSIDE,
                    videoWidth, videoHeight,
                    width, height);
            Matrix.rotateM(matrix, 0, rotate, 0, 0, -1);


            nor.setVertexMatrix(matrix);

            source.port.texture = texture;
            SurfaceTexture surfaceTexture = new SurfaceTexture(texture);
            Surface surface = new Surface(surfaceTexture);
            source.task.setSurface(surface);
            source.surfaceTexture = surfaceTexture;
            source.surfaceTexture.setOnFrameAvailableListener(new SurfaceTexture.OnFrameAvailableListener() {
                @Override
                public void onFrameAvailable(SurfaceTexture surfaceTexture) {
                    source.isAvailable = true;
                }
            });

            while (mRuning && !isEof()) {
                ViewPort p = source.port;
                while (mRuning && !source.task.isEof()) {
                    if (source.task.decode(mPts + source.start)) {
                        while (mRuning && !source.isAvailable && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            Thread.sleep(20);
                        }
                        source.surfaceTexture.updateTexImage();
                        break;
                    }
                }

                GLES20.glViewport(0, 0, source.w, source.h);
                int texture2d = source.filter.drawToTexture(p.texture);
                GLES20.glViewport(0, 0, width, height);
                nor.draw(texture2d);

                drawLogo(showFilter);

                encodeSurface.setPresentationTime(mPts * 1000 * 1000);
                encodeSurface.swapBuffers();
                clearCanvas();
                myRecorder.addtimeStamp((int) mPts);
                if (mPts % TIME_SCALE * 30 == 0) {
                    l.onProcess((int) (mPts * 100f / source.duration));
                }
                mPts += TIME_SCALE;
                myRecorder.drainEncoder();
            }

            myRecorder.signalEndOfInputStream();
            while (mRuning && !myRecorder.isEof()) {
                myRecorder.drainEncoder();
                Thread.sleep(5);//编码间隔
            }
            Thread.sleep(100);
            l.onFinish();
            Log.e("z", "custom :" + (System.currentTimeMillis() - start));
            myRecorder.releaseEncoder();

            source.surfaceTexture.release();
            source.filter.destroy();
            source.task.releaseDecoder();

            releaseLogo(showFilter);
            encodeSurface.release();
        } catch (Exception e) {
            e.printStackTrace();
            l.onError();
        }
    }

    private void clearCanvas() {
        GLES20.glClearColor(1f, 0f, 0f, 1.0f);//#1a1a1a
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
    }

    private boolean isEof() {
        return source.task.isEof();
    }

    public boolean isRuning() {
        return mRuning;
    }
}
