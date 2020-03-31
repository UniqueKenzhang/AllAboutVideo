/*
 * Copyright (C) 2013 MorihiroSoft
 * Copyright 2013 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.allaboutvideo.codec;

import android.annotation.TargetApi;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.Build;
import android.util.Log;
import android.view.Surface;

import java.io.FileNotFoundException;
import java.nio.ByteBuffer;
import java.util.LinkedList;

@TargetApi(18)
public class MyRecorder {


    private static final String MIME = "video/avc";
    private static final String TAG = MyRecorder.class.getSimpleName();
    //---------------------------------------------------------------------
    // MEMBERS
    //---------------------------------------------------------------------
    private final int frameWidth;
    private final int frameHeight;
    private static int level = 2;
    private static int sublevel = 2;
    private final int fps;
    private int gop = 2;
    private int bitrate;

    private MediaCodec mMediaCodec = null;
    private MediaCodec.BufferInfo mBufferInfo = null;

    private String mOutput;
    private Surface mSurface;
    private OnEcodecListener l;
    private long mFirstStamp;

    private final LinkedList<Integer> mStamps = new LinkedList<>();
    private boolean enableHardWare = true;
    private boolean prepare;
    private int mLastPts = -1;
    private boolean mMediaCodecStarted;
    private boolean mMuxerStarted;
    private MediaMuxer mMuxer;
    private int mTrackIndex;


    public MyRecorder(int w, int h, int bitrate, int gop, int fps, String outputPath) {
        this.frameWidth = w;
        this.frameHeight = h;
        this.mOutput = outputPath;
        this.bitrate = bitrate;
        this.fps = fps;
        this.gop = gop;

        prepareEncoder(level, sublevel);

    }

    public void setOnEncodedListener(OnEcodecListener l) {
        this.l = l;
    }

    public void addtimeStamp() {
        long l = System.currentTimeMillis();
        if (mFirstStamp == 0) {
            mFirstStamp = l;
        }
        try {
            long pts = l - mFirstStamp;
            if (mLastPts >= 0) {
                long detaT = pts - mLastPts;
                if (detaT < 50) {
                    Thread.sleep(50 - detaT);
                }
            }
            mLastPts = (int) (System.currentTimeMillis() - mFirstStamp);
            synchronized (mStamps) {
                mStamps.add(mLastPts);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void addtimeStamp(int pts) {
        try {
            synchronized (mStamps) {
                mStamps.add(pts);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public void setEnableHardWare(boolean enableHardWare) {
        this.enableHardWare = enableHardWare;
    }

    public boolean isPrepare() {
        return prepare;
    }

    public OnEcodecListener getOnEcodecListener() {
        return l;
    }

    public interface OnEcodecListener {
        void onEncodecd(MyRecorder recorder, ByteBuffer buffer, MediaCodec.BufferInfo info, int timestemp);

        void onGetData(byte[] bytes);
    }

    private void prepareEncoder(int encodeLevel, int modeLevel) {


        mBufferInfo = new MediaCodec.BufferInfo();
        try {
            MediaFormat format = MediaFormat.createVideoFormat(MIME, frameWidth, frameHeight);

            float rate = 1;
            if (gop >= 12 && modeLevel < 2) {
                rate = 1.5f;
            }

            format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
            format.setInteger(MediaFormat.KEY_BIT_RATE, bitrate > 0 ? bitrate : (int) (fps / 10 * 1024 * 1024 * rate));
            format.setInteger(MediaFormat.KEY_FRAME_RATE, fps);
            format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, gop);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                if (encodeLevel > 1) {
                    format.setInteger(MediaFormat.KEY_PROFILE, MediaCodecInfo.CodecProfileLevel.AVCProfileHigh);
                    format.setInteger("level", MediaCodecInfo.CodecProfileLevel.AVCLevel41);
                } else if (encodeLevel > 0) {
                    format.setInteger(MediaFormat.KEY_PROFILE, MediaCodecInfo.CodecProfileLevel.AVCProfileMain);
                    format.setInteger("level", MediaCodecInfo.CodecProfileLevel.AVCLevel32);
                }


                if (gop < 12) {
                    if (modeLevel > 1) {
                        format.setInteger(MediaFormat.KEY_BITRATE_MODE, MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_CQ);
                    } else if (modeLevel > 0) {
                        format.setInteger(MediaFormat.KEY_BITRATE_MODE, MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_CBR);
                    }
                } else {
                    if (modeLevel > 1) {
                        format.setInteger(MediaFormat.KEY_BITRATE_MODE, MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_CQ);
                    } else if (modeLevel > 0) {
                        format.setInteger(MediaFormat.KEY_BITRATE_MODE, MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_VBR);
                    }
                }

            }

            mMediaCodec = MediaCodec.createEncoderByType(MIME);
            mMediaCodec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            mMuxer = new MediaMuxer(mOutput, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);

        } catch (FileNotFoundException e) {
            releaseEncoder();
        } catch (Exception e) {
            if (sublevel >= 1) {
                sublevel--;
                Log.e("z", "sublevel down");
                prepareEncoder(level, sublevel);
            } else if (level >= 1) {
                level--;
                sublevel = 2;
                Log.e("z", "level down");
                prepareEncoder(level, sublevel);
            } else {
                releaseEncoder();
                e.printStackTrace();
            }
        }
    }

    public boolean isRecording() {
        return mMediaCodecStarted;
    }

    public void signalEndOfInputStream() {
        try {
            if (isSuccess() && mMediaCodecStarted) {
                mMediaCodec.signalEndOfInputStream();
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    public void stop() {
        try {
            if (isSuccess() && mMediaCodecStarted) {
                mMediaCodec.signalEndOfInputStream();
            }
        } catch (Throwable e) {
            e.printStackTrace();
        } finally {
            mMediaCodecStarted = false;
            releaseEncoder();
        }
    }

    public void releaseEncoder() {

        try {
            if (isSuccess() && mMediaCodec != null) {
                mMediaCodec.stop();
                mMuxer.stop();
            }
        } catch (RuntimeException e) {
            e.printStackTrace();
            Log.e(TAG, "releaseEncoder error!");
        } finally {
            if (mMediaCodec != null) {
                mMediaCodec.release();
                mMediaCodec = null;
            }

            if (mMuxer != null) {
                mMuxer.release();
            }
        }
    }


    public void drainEncoder() {
        try {
            if (!isValid()) {
                return;
            }
            ByteBuffer[] encoderOutputBuffers = mMediaCodec.getOutputBuffers();
            while (true) {
                int encoderStatus = mMediaCodec.dequeueOutputBuffer(mBufferInfo, 0);
                if (encoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                    break;
                } else if (encoderStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                    encoderOutputBuffers = mMediaCodec.getOutputBuffers();
                } else if (encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    if (mMuxerStarted) {
                        Log.e(TAG, "format changed twice");
                        mMediaCodec.releaseOutputBuffer(encoderStatus, false);
                        continue;
//					throw new RuntimeException("format changed twice");
                    }

                    mTrackIndex = mMuxer.addTrack(mMediaCodec.getOutputFormat());
                    mMuxer.start();
                    mMuxerStarted = true;
                } else {
                    ByteBuffer encodedData = encoderOutputBuffers[encoderStatus];
                    if (encodedData == null) {
                        Log.e(TAG, "encoderOutputBuffer " + encoderStatus + " was null");
                        mMediaCodec.releaseOutputBuffer(encoderStatus, false);
                        continue;
                    }

                    if (mBufferInfo.size != 0) {
                        if (!mMuxerStarted) {
                            Log.e(TAG, "muxer hasn't started");
                            mMediaCodec.releaseOutputBuffer(encoderStatus, false);
                            continue;
                        }
                        encodedData.position(mBufferInfo.offset);
                        encodedData.limit(mBufferInfo.offset + mBufferInfo.size);


                        if (l != null) {
                            int stamp = 0;
                            if (mBufferInfo.flags != MediaCodec.BUFFER_FLAG_CODEC_CONFIG) {
                                synchronized (mStamps) {
                                    if (mStamps.size() > 0)
                                        stamp = mStamps.remove(0);
                                }
                            }
                            l.onEncodecd(this, encodedData, mBufferInfo, stamp);
                        }
                        mMuxer.writeSampleData(mTrackIndex, encodedData, mBufferInfo);
                    }
                    mMediaCodec.releaseOutputBuffer(encoderStatus, false);
                    if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        mMuxer.stop();
                        break;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean isEof() {
        return mBufferInfo.flags >= MediaCodec.BUFFER_FLAG_END_OF_STREAM;
    }

    public void prepare() {
        prepare = true;
    }

    private boolean isValid() {
        return mMediaCodec != null && mBufferInfo != null && mMediaCodecStarted;
    }

    public String getOutputPath() {
        return mOutput;
    }

    public void start() {
        mMediaCodecStarted = true;
        if (isSuccess()) {
            mMediaCodec.start();
            new Thread() {
                @Override
                public void run() {
                    while (mMediaCodecStarted) {
                        drainEncoder();
                        try {
                            Thread.sleep(25);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }.start();
        }
    }

    public void startManual() {
        mMediaCodecStarted = true;
        if (isSuccess()) {
            mMediaCodec.start();
        }
    }

    public Surface getInputSurface() {
        if (mSurface == null && isSuccess()) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                    mSurface = mMediaCodec.createInputSurface();
                }
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
        return mSurface;
    }

    public boolean isSuccess() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2) {
            return false;
        }

        if (mMediaCodec == null) {
            return false;
        } else {
            return enableHardWare;
        }
    }

    public int getFrameHeight() {
        return frameHeight;
    }

    public int getFrameWidth() {
        return frameWidth;
    }
}
