package com.example.allaboutvideo.codec;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.Build;
import android.util.Log;
import android.view.Surface;

import androidx.annotation.RequiresApi;

import java.nio.ByteBuffer;

@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
public class MediaDecodeTask {


    private final MVExtractor mMVExtractor;
    private final long mEnd;
    private int mVideoIndex;
    private int mAudioIndex;
    private Surface mSurface;
    private MediaCodec mVideoDecoder;
    private boolean mVideoExtractorDone;
    private int videoExtractedFrameCount;
    private MediaCodec.BufferInfo videoDecoderOutputBufferInfo;
    private ByteBuffer[] videoDecoderInputBuffers;
    private boolean mVideoDecodeEof = false;

    public MVExtractor getExtractor() {
        return mMVExtractor;
    }

    public MediaDecodeTask(MVExtractor extractor, long end) {
        mMVExtractor = extractor;
        this.mEnd = end * 1000;
    }

    public void setSurface(Surface surface) throws Exception {
        if (mSurface == null) {
            mSurface = surface;
        } else {
            Log.e("z", "Surface already bind" + surface.toString());
        }

        mVideoIndex = mMVExtractor.getVideoIndex();
        mAudioIndex = mMVExtractor.getAudioIndex();

        MediaFormat inputFormat = mMVExtractor.getTrackFormat(mVideoIndex);
        if (inputFormat == null) {
            Log.e("z", "inputFormat NULL!!");
            return;
        }

        mVideoDecoder = createVideoDecoder(inputFormat, mSurface);
        videoDecoderInputBuffers = mVideoDecoder.getInputBuffers();
        videoDecoderOutputBufferInfo = new MediaCodec.BufferInfo();
    }

    public boolean decode(long ptsMs) {
        if (videoDecoderOutputBufferInfo.presentationTimeUs - ptsMs * 1000 < 50 * 1000) {
            setVideoDecoderInput(videoDecoderInputBuffers);
        } else {
            Log.e("z", "throw frame");
            return true;
        }

        int decoderOutputBufferIndex = MediaCodec.INFO_TRY_AGAIN_LATER;
        try {
            decoderOutputBufferIndex = mVideoDecoder.dequeueOutputBuffer(videoDecoderOutputBufferInfo, 50 * 10);
        } catch (Exception e) {
            sendErrorReport(10002, 4, e);
            return false;
        }
        if (videoDecoderOutputBufferInfo.flags == MediaCodec.BUFFER_FLAG_END_OF_STREAM || videoDecoderOutputBufferInfo.presentationTimeUs < 0) {//eof优先级高于一切
            mVideoDecoder.releaseOutputBuffer(decoderOutputBufferIndex, false);
            mVideoDecodeEof = true;
            return false;
        } else if (decoderOutputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
            return false;
        } else if (decoderOutputBufferIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
            try {
                mVideoDecoder.getOutputBuffers();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return false;
        } else if (decoderOutputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
            try {
                mVideoDecoder.getOutputFormat();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return false;
        } else if ((videoDecoderOutputBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
            mVideoDecoder.releaseOutputBuffer(decoderOutputBufferIndex, false);
            return false;
        } else if (videoDecoderOutputBufferInfo.presentationTimeUs < ptsMs * 1000) {
            mVideoDecoder.releaseOutputBuffer(decoderOutputBufferIndex, false);
            Log.e("z", "throw frame " + videoDecoderOutputBufferInfo.presentationTimeUs + "  ," + ptsMs);
            return false;
        } else if (videoDecoderOutputBufferInfo.presentationTimeUs > mEnd) {
            mVideoDecoder.releaseOutputBuffer(decoderOutputBufferIndex, false);
            mVideoDecodeEof = true;
            return false;
        }


        try {
            mVideoDecoder.releaseOutputBuffer(decoderOutputBufferIndex, videoDecoderOutputBufferInfo.size != 0);
            return true;
        } catch (Exception e) {
            sendErrorReport(10006, 4, e);
            return false;
        }
    }


    private void setVideoDecoderInput(ByteBuffer[] videoDecoderInputBuffers) {

        if (mVideoDecoder == null || videoDecoderInputBuffers == null) {
            return;
        }

        while (!mVideoExtractorDone) {
            int videoInputBufferIndex = mVideoDecoder.dequeueInputBuffer(videoExtractedFrameCount > 3 ? 0 : 50);
            if (videoInputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                if (videoExtractedFrameCount == 0) {
                    continue;
                }
                break;
            } else if (videoInputBufferIndex < 0 || videoInputBufferIndex >= videoDecoderInputBuffers.length) {
                break;
            }

            ByteBuffer decoderInputBuffer = videoDecoderInputBuffers[videoInputBufferIndex];
            if (decoderInputBuffer == null) {
                break;
            }

            if (!mMVExtractor.isNeedAudio) {
                mMVExtractor.clear(mAudioIndex);
            }
            MVExtractor.Frame frame = mMVExtractor.readFrame(mVideoIndex);

            if (frame == null) {
                if (mMVExtractor.isEof()) {
                    mVideoExtractorDone = true;
                    try {
                        mVideoDecoder.queueInputBuffer(
                                videoInputBufferIndex,
                                0,
                                0,
                                -1,
                                MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                    } catch (Exception e) {
                        sendErrorReport(100020, 4, e);
                        return;
                    }
                }
                break;
            }
            decoderInputBuffer.position(0);
            decoderInputBuffer.put(frame.buffer);
            int size = frame.bufferSize;
            long presentationTime = frame.ptsUs;
            int flags = frame.sampleFlags;

            if (size >= 0) {
                try {
                    mVideoDecoder.queueInputBuffer(
                            videoInputBufferIndex,
                            0,
                            size,
                            presentationTime,
                            flags);

                } catch (Exception e) {
                    sendErrorReport(100021, 4, e);
                    return;
                }
            }
            videoExtractedFrameCount++;
        }
    }

    private void sendErrorReport(int errorNo, int what, Exception e) {
        Log.e("z", "sendErrorReport errorNo" + errorNo);
        if (e != null) {
            e.printStackTrace();
        }
    }


    private String getMimeTypeFor(MediaFormat format) {
        return format.getString(MediaFormat.KEY_MIME);
    }

    private MediaCodec createVideoDecoder(MediaFormat inputFormat, Surface surface) throws Exception {
        MediaCodec decoder = MediaCodec.createDecoderByType(getMimeTypeFor(inputFormat));

        Exception exception = null;
        try {
            decoder.configure(inputFormat, surface, null, 0);
        } catch (Exception e) {
            e.printStackTrace();
            decoder.release();
            exception = e;
        } finally {
            if (exception != null) {
                throw exception;
            }
        }
        decoder.start();
        return decoder;
    }

    public boolean isEof() {
        return mVideoDecodeEof;
    }


    public void releaseDecoder() {

        try {
            if (mVideoDecoder != null) {
                mVideoDecoder.stop();
                mVideoDecoder.release();
            }
            mMVExtractor.release();
            mSurface.release();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
