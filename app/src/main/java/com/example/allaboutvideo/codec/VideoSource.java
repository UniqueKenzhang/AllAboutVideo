package com.example.allaboutvideo.codec;

import android.annotation.TargetApi;
import android.graphics.SurfaceTexture;
import android.os.Build;

import com.example.allaboutvideo.filter.OesContinueFilter;
import com.example.allaboutvideo.filter.ViewPort;

@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class VideoSource {

    public int bitRate = 0;
    public int fps;
    public int rotate;
    public long duration;
    public MediaDecodeTask task;
    public ViewPort port;
    public SurfaceTexture surfaceTexture;
    public boolean isAvailable;
    public int w;
    public int h;
    public long start;
    public long end;
    public OesContinueFilter filter;

    public VideoSource(ViewPort port) {
        this(port, 0, 0);
    }


    public VideoSource(ViewPort port, long start, long end) {
        this.port = port;
        this.start = start;
        MVExtractor extractor = new MVExtractor(port.path, start);
        extractor.setEableAudioTrack(false);
        if (end <= 0) {
            this.end = extractor.getDuration() / 1000;
        } else {
            this.end = end;
        }

        task = new MediaDecodeTask(extractor, this.end);
        w = extractor.getWidth();
        h = extractor.getHeight();
        rotate = extractor.getRotate();
        fps = extractor.getFrameRate();
        if (fps <= 0) {
            fps = 30;
        }
        this.duration = this.end - start;
    }
}
