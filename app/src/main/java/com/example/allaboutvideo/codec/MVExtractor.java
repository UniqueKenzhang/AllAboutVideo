package com.example.allaboutvideo.codec;

import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMetadataRetriever;
import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
public class MVExtractor {
    public boolean isNeedAudio;
    private long start;
    private boolean enableAudio;

    public class Frame {
        public ByteBuffer buffer = null;
        public int bufferSize = 0;
        public long ptsUs;
        public int sampleFlags;
        public int index;

        Frame() {
            buffer = null;
            ptsUs = 0;
            sampleFlags = 0;
            index = -1;
        }
    }

    private String TAG = "MVExtractor";
    private static final boolean VERBOSE = false; // lots of Logging
    private MediaExtractor mExtractor = null;
    private boolean mExtractorCreating = false;

    private int mVideoMax = 1024;
    private int mAudioMax = 256;

    private Queue<Frame> mVideoQueue = new LinkedList<Frame>();
    private Queue<Frame> mAudioQueue = new LinkedList<Frame>();


    private int mVideoBufferSize = 0;
    private int mAudioBufferSize = 0;

    private boolean mReadIsFailed = false;
    private int mReadErrorCount = 0;
    private long mAudioNewReadPts = 0;
    private long mCurrReadPts = 0;
    private int mVideoIndex = -1;
    private int mAudioIndex = -1;
    private boolean mExtractorDone = false;
    private boolean mStop = false;
    private boolean mThreadIsRun = false;
    private Lock mLock = new ReentrantLock(true);
    private Condition mCondition = mLock.newCondition();

    private Lock mAudioLock = new ReentrantLock(true);
    private Condition mAudioCondition = mAudioLock.newCondition();

    private Lock mVideoLock = new ReentrantLock(true);
    private Condition mVideoCondition = mVideoLock.newCondition();

    private long mSeektimeUs = -1;
    private int mSeekMode = MediaExtractor.SEEK_TO_NEXT_SYNC;

    private boolean mVideoSeekDone = true;
    private boolean mAudioSeekDone = true;
    private boolean mAudioReadPaused = false;
    private boolean mVideoReadPaused = false;

    private ByteBuffer mVideoBuffer = null;
    private ByteBuffer mAudioBuffer = null;
    private String mFile = null;

    public MVExtractor(String sourceFile, long start) {
        this(sourceFile, -1, false);
    }

    public MVExtractor(String sourceFile, long startMs, boolean isNeedAudio) {
        if (sourceFile == null) {
            return;
        }
        this.start = startMs;
        mFile = sourceFile;
        this.isNeedAudio = isNeedAudio;
        StartThread();
    }


    public static int EXTRACTOR_NO_ERRROR = 0;
    public static int EXTRACTOR_ERRROR_NO_SUCH_FILE = 1;
    public static int EXTRACTOR_ERROR_BROKEN_FILE = 2;

    public static int EXTRACTOR_SOURCE_TYPE_NULL = 0;
    public static int EXTRACTOR_SOURCE_TYPE_PROXY = 1;
    public static int EXTRACTOR_SOURCE_TYPE_NET = 2;
    public static int EXTRACTOR_SOURCE_TYPE_LOCAL = 3;

    static public boolean fileIsExists(String strFile) {
        try {
            File f = new File(strFile);
            if (!f.exists()) {
                return false;
            }
        } catch (Exception e) {
            return false;
        }

        return true;
    }

    public int getSourcType() {
        if (mFile == null || mFile.length() == 0) {
            return EXTRACTOR_SOURCE_TYPE_NULL;
        }
        if (mFile.indexOf("http://127.0.0.1") == 0) {
            return EXTRACTOR_SOURCE_TYPE_PROXY;
        }
        if (mFile.indexOf("http://fs.mv.android.kugou.com") == 0) {
            return EXTRACTOR_SOURCE_TYPE_NET;
        }
        return EXTRACTOR_SOURCE_TYPE_LOCAL;

    }

    public int getErrorState() {

        if (getExtractor() == null && !fileIsExists(mFile))
            return EXTRACTOR_ERRROR_NO_SUCH_FILE;
        else if (getVideoIndex() < 0 || getAudioIndex() < 0)
            return EXTRACTOR_ERROR_BROKEN_FILE;

        return EXTRACTOR_NO_ERRROR;
    }

    private final int getInteger(MediaFormat format, String name, int defaultValue) {
        try {
            if (format != null) {
                return format.getInteger(name);
            }
        } catch (NullPointerException e) { /* no such field */
            e.printStackTrace();
        } catch (ClassCastException e) { /* field of different type */
            e.printStackTrace();
        }
        return defaultValue;
    }

    private void init() {
        getExtractor();
        getVideoIndex();
        getAudioIndex();
        if (mExtractor != null && mVideoIndex >= 0) {

            MediaFormat format = mExtractor.getTrackFormat(mVideoIndex);
            if (format != null) {
                mVideoBufferSize = getInteger(format, MediaFormat.KEY_MAX_INPUT_SIZE, 0);
                if (mVideoBufferSize <= 0) {
                    int width = getInteger(format, MediaFormat.KEY_WIDTH, 0);
                    int height = getInteger(format, MediaFormat.KEY_HEIGHT, 0);
                    mVideoBufferSize = width * height * 3;
                    if (mVideoBufferSize == 0) {
                        mVideoBufferSize = 1920 * 1080 * 3;  // 1080P yuv
                    }
                }

                if (mVideoBufferSize > 0) {
                    mVideoBuffer = ByteBuffer.allocate(mVideoBufferSize);
                }
            }
        }
        if (mExtractor != null && mAudioIndex >= 0) {
            MediaFormat format = mExtractor.getTrackFormat(mAudioIndex);
            if (format != null) {
                mAudioBufferSize = getInteger(format, MediaFormat.KEY_MAX_INPUT_SIZE, 0);
                if (mAudioBufferSize <= 0) {
                    int channel = getInteger(format, MediaFormat.KEY_CHANNEL_COUNT, 0);
                    int sample_rate = getInteger(format, MediaFormat.KEY_SAMPLE_RATE, 0);
                    mAudioBufferSize = channel * sample_rate * 4 * 46 / 1000;  // > channel * sample_rate * 2 * 23 / 1000
                    if (mAudioBufferSize < 4096) {
                        mAudioBufferSize = 4096;
                    }
                }

                if (mAudioBufferSize > 0) {
                    mAudioBuffer = ByteBuffer.allocate(mAudioBufferSize);
                }
            }

            mExtractor.seekTo(start * 1000, MediaExtractor.SEEK_TO_PREVIOUS_SYNC);
            Log.d(TAG, "MVExtractor: mVideoBufferSize:" + mVideoBufferSize + " mAudioBufferSize:" + mAudioBufferSize);
        }
    }

    private MediaExtractor createExtractor() {
        MediaExtractor extractor = null;
        Log.d(TAG, "createExtractor filepath(" + mFile + ")");
        if (mFile != null) {
            extractor = new MediaExtractor();
            try {
                extractor.setDataSource(mFile);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                //Exception:java.io.IOException: Failed to instantiate extractor

                Log.e(TAG, "createExtractor Exception:" + e + " mFile:" + mFile);
            } finally {
            }
        }
        return extractor;
    }

    private MediaExtractor getExtractor() {

        mLock.lock();
        try {
            if (!mStop && mExtractor == null && mFile != null && !mExtractorCreating) {
                mExtractorCreating = true;
                mLock.unlock();

                Log.d(TAG, "getExtractor createExtractor filepath(" + mFile + ")" + " fileIsExists:" + fileIsExists(mFile));
                MediaExtractor extractor = createExtractor();

                mLock.lock();
                mExtractor = extractor;
                mExtractorCreating = false;
                mCondition.signalAll();

                Log.d(TAG, "getExtractor mExtractor:" + mExtractor + " this:" + this);
            } else {

                // Log.d(TAG,
                // "getExtractor  mExtractor:"+mExtractor+" filepath("+mFile+")");
                while (!mStop && mExtractor == null && mFile != null && mExtractorCreating) {

                    Log.d(TAG, "getExtractor wait createExtractor filepath(" + mFile + ")");
                    mCondition.await();
                }
            }
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            Log.e(TAG, "getExtractor Exception:" + e);
        } finally {
            mLock.unlock();
        }
        //Log.d(TAG, "getExtractor mExtractor:"+mExtractor);
        return mExtractor;
    }

    public boolean frameQueueIsEmpty() {
        boolean isEmpty = true;
        mAudioLock.lock();
        if (mAudioQueue != null && mAudioQueue.size() > 0) {
            isEmpty = false;
        }
        mAudioLock.unlock();
        return isEmpty;
    }

    public boolean resetExtractor() {
        return resetExtractor(0);
    }

    public void setSource(String sourceFile) {
        mFile = sourceFile;
    }

    private boolean resetExtractor(long seekUs) {
        try {
            Log.d(TAG, "resetExtractor createExtractor filepath(" + mFile + ")");
            MediaExtractor extractor = createExtractor();
            getAndSelectVideoTrackIndex(extractor);

            if (extractor == null) {
                Log.e(TAG, "resetExtractor createExtractor failed");
                return false;
            }

            mLock.lock();
            MediaExtractor tmp = mExtractor;
            mExtractor = extractor;
            clearAll();
            mLock.unlock();
            tmp.release();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            Log.e(TAG, "resetExtractor Exception:" + e);
        } finally {

            Log.d(TAG, "resetExtractor end,mExtractor:" + mExtractor + " this:" + this);
        }
        return true;

    }

    private Frame readFrameFromExtractor() {
        MediaExtractor extractor = getExtractor();
        if (mExtractorDone || extractor == null) {

            Log.e(TAG, "readFrameFromExtractor error extractorDone:" + mExtractorDone + " extractor:" + extractor);
            return null;
        }

        Frame frame = new Frame();
        frame.index = extractor.getSampleTrackIndex();
        if (frame.index >= 0) {
            frame.ptsUs = extractor.getSampleTime();
            frame.sampleFlags = extractor.getSampleFlags();
            if (frame.ptsUs > 0) {
                if (frame.index == mAudioIndex) {
                    mAudioNewReadPts = frame.ptsUs;
                }
                mCurrReadPts = frame.ptsUs;

            }
            mReadErrorCount = 0;
        } else {
            mReadIsFailed = true;
            mReadErrorCount++;

            Log.e(TAG, "readFrameFromExtractor failed index:" + frame.index + " ptsUs:" + frame.ptsUs + " flags:" + frame.sampleFlags + "videoindex:" + mVideoIndex + " mAudioIndex:" + mAudioIndex + "mReadErrorCount:" + mReadErrorCount);
        }
        //int ret = -1;
        if (VERBOSE)
            Log.d(TAG, "readFrameFromExtractor  ptsUs:" + frame.ptsUs + " index:" + frame.index + " flags:" + frame.sampleFlags + "videoindex:" + mVideoIndex + " mAudioIndex:" + mAudioIndex);
        if (mVideoIndex == frame.index && mVideoBuffer != null && mVideoBufferSize > 0) {
            mVideoBuffer.clear();
            frame.bufferSize = extractor.readSampleData(mVideoBuffer, 0);
            if (VERBOSE)
                Log.d(TAG, "readFrameFromExtractor mVideoIndex bufferSize:" + frame.bufferSize);
            if (frame.bufferSize > 0 && frame.bufferSize <= mVideoBufferSize) {
                frame.buffer = ByteBuffer.allocate(frame.bufferSize);
                if (frame.buffer == null) {

                    Log.e(TAG, "readFrameFromExtractor video allocate failed,bufferSize:" + frame.bufferSize);
                    return null;
                }
                mVideoBuffer.position(0);
                mVideoBuffer.limit(frame.bufferSize);

                frame.buffer.clear();
                frame.buffer.put(mVideoBuffer);

                frame.buffer.position(0);
                frame.buffer.limit(frame.bufferSize);
            } else {

                Log.e(TAG, "readFrameFromExtractor video bufferSize(" + frame.bufferSize + ")invalid, maxbuffersize:" + mVideoBufferSize);
                return null;
            }

        } else if (mAudioIndex == frame.index && mAudioBuffer != null && mAudioBufferSize > 0) {
            mAudioBuffer.clear();
            frame.bufferSize = extractor.readSampleData(mAudioBuffer, 0);
            if (VERBOSE)
                Log.d(TAG, "readFrameFromExtractor mAudioIndex bufferSize:" + frame.bufferSize);
            if (frame.bufferSize > 0 && frame.bufferSize <= mAudioBufferSize) {
                frame.buffer = ByteBuffer.allocate(frame.bufferSize);
                if (frame.buffer == null) {

                    Log.e(TAG, "readFrameFromExtractor audio allocate failed,bufferSize:" + frame.bufferSize);
                    return null;
                }
                mAudioBuffer.position(0);
                mAudioBuffer.limit(frame.bufferSize);

                frame.buffer.clear();
                frame.buffer.put(mAudioBuffer);

                frame.buffer.position(0);
                frame.buffer.limit(frame.bufferSize);
            } else {

                Log.e(TAG, "readFrameFromExtractor audio bufferSize(" + frame.bufferSize + ")invalid, maxbuffersize:" + mAudioBufferSize);
                return null;
            }

        }
		/*if( mReadIsFailed )
		{
			long duration = getDuration();
			Log.d(TAG, "readFrameFromExtractor: read failed  duration:" + duration + " readpts:" + mAudioNewReadPts );
			if( duration - 500000 < mAudioNewReadPts)
			{
				mExtractorDone = !extractor.advance();
				Log.iLF(TAG,"readFrameFromExtractor: readSampleData size:"+frame.bufferSize+ " is EOF:"+(mExtractorDone?"yes":"no" ));
			}
		}else
		*/
        {
            mExtractorDone = !extractor.advance();
        }

        if (frame.bufferSize <= 0) {

            Log.e(TAG, "readFrameFromExtractor: readSampleData size:" + frame.bufferSize + " is EOF:" + (mExtractorDone ? "yes" : "no"));
            frame = null;
        } else {
            if (VERBOSE)
                Log.d(TAG, "readFrameFromExtractor: readSampleData size:" + frame.bufferSize + " is EOF:" + (mExtractorDone ? "yes" : "no"));
        }
        if (mReadErrorCount >= 3) {
            //reset extractor
            //resetExtractor( mCurrReadPts );
        }
        return frame;
    }

    public int getVideoIndex() {
        getExtractor();

        mLock.lock();
        mVideoLock.lock();
        try {
            if (mVideoIndex < 0) {
                mVideoIndex = getAndSelectVideoTrackIndex(mExtractor);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            mVideoLock.unlock();
            mLock.unlock();
        }
        return mVideoIndex;
    }

    public int getAudioIndex() {
        getExtractor();
        mLock.lock();
        mAudioLock.lock();
        try {
            if (mAudioIndex < 0) {
                mAudioIndex = getAndSelectAudioTrackIndex(mExtractor);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            mAudioLock.unlock();
            mLock.unlock();
        }
        return mAudioIndex;
    }

    public MediaFormat getAudioFormat() {

        try {
            if (mAudioIndex >= 0) {
                return mExtractor.getTrackFormat(mAudioIndex);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public void seekTo(long timeUs, int mode) {
        Log.d(TAG, "seekTo " + timeUs + "us");
        if (timeUs < 0) {
            Log.e(TAG, "seekTo " + timeUs + "us invalid");
            return;
        }
        mLock.lock();
        try {
            mSeektimeUs = timeUs;
            mSeekMode = mode;
            mExtractorDone = false;
            mReadIsFailed = false;

            mAudioLock.lock();
            try {
                mAudioSeekDone = false;
                mAudioReadPaused = true;
                mAudioCondition.signalAll();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                mAudioLock.unlock();
            }

            mVideoLock.lock();
            try {
                mVideoSeekDone = false;
                mVideoReadPaused = true;
                mVideoCondition.signalAll();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                mVideoLock.unlock();
            }
            mCondition.signalAll();

            if (!mThreadIsRun) {
                StartThread();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            mLock.unlock();
        }

        Log.d(TAG, "seekTo end");
    }

    public void setReadState(int index) {
        Log.d(TAG, "setReadState start");
        if (index == mVideoIndex || index == -1) {
            Log.d(TAG, "setReadState video...");
            mVideoLock.lock();
            mVideoReadPaused = false;
            mVideoLock.unlock();
        }
        if (index == mAudioIndex || index == -1) {
            Log.d(TAG, "setReadState audio...");
            mAudioLock.lock();
            mAudioReadPaused = false;
            mAudioLock.unlock();

        }
        Log.d(TAG, "setReadState end");
    }

    public boolean isEof() {
        mLock.lock();
        boolean eof = mExtractorDone && mSeektimeUs < 0;
        mLock.unlock();
        return eof;
    }

    public void eof() {
        mLock.lock();
        mExtractorDone = true;
        mLock.unlock();
    }

    public Frame readFrame(int index) {
        Frame frame = null;
        if (VERBOSE) Log.d(TAG, "readFrame index:" + index);

        if (index == mVideoIndex) {
            mVideoLock.lock();
            try {
                while (!mStop && !mVideoReadPaused && !mExtractorDone && mVideoQueue != null && (!mVideoSeekDone || mVideoQueue.size() == 0)) {
                    mVideoCondition.await();
                }

                if (!mVideoReadPaused && mVideoSeekDone && mVideoQueue != null && mVideoQueue.size() > 0) {
                    frame = mVideoQueue.poll();
                }
            } catch (Exception e) {
                Log.e(TAG, "getVideoFrame Exception:" + e);
                e.printStackTrace();
            } finally {
                mVideoLock.unlock();
            }

        } else if (index == mAudioIndex) {
            mAudioLock.lock();
            try {
                while (!mStop && !mAudioReadPaused && !mExtractorDone && mAudioQueue != null && (!mAudioSeekDone || mAudioQueue.size() == 0)) {
                    mAudioCondition.await();
                }
                if (!mAudioReadPaused && mAudioSeekDone && mAudioQueue != null && mAudioQueue.size() > 0) {
                    frame = mAudioQueue.poll();
                }
            } catch (Exception e) {
                Log.e(TAG, "get audio frame Exception:" + e);
                e.printStackTrace();

            } finally {
                mAudioLock.unlock();
            }
        } else {
            Log.e(TAG, "invalid index");
        }

        mLock.lock();
        try {
            mCondition.signalAll();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            mLock.unlock();
        }
        if (VERBOSE) Log.d(TAG, "readFrame index:" + index + " end frame:" + frame);
        return frame;
    }

    public void clearAll() {
        mLock.lock();
        mExtractorDone = false;
        mLock.unlock();

        clear(mVideoIndex);
        clear(mVideoIndex);
    }

    public void clear(int index) {
        if (mVideoIndex == index) {
            mVideoLock.lock();
            try {
                if (!mVideoSeekDone && mVideoQueue != null) {
                    mVideoQueue.clear();
                    mVideoCondition.signalAll();
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                mVideoLock.unlock();
            }
        } else if (mAudioIndex == index) {
            mAudioLock.lock();
            try {
                if (!mAudioSeekDone && mAudioQueue != null) {
                    mAudioQueue.clear();
                    mAudioCondition.signalAll();
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                mAudioLock.unlock();
            }
        }
    }

    public void release() {
        Log.d(TAG, " release");
        mLock.lock();
        try {
            mStop = true;
            mCondition.signalAll();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            mLock.unlock();
        }
        Log.d(TAG, " release end");
    }

    public void stop() {
        Log.d(TAG, " stop entry");
        mLock.lock();
        try {
            mStop = true;

            mCondition.signalAll();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            mLock.unlock();
        }
        Log.d(TAG, " stop 1");
        mAudioLock.lock();
        try {
            mAudioReadPaused = true;
            mAudioCondition.signalAll();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            mAudioLock.unlock();
        }

        mVideoLock.lock();
        try {
            mVideoReadPaused = true;
            mVideoCondition.signalAll();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            mVideoLock.unlock();
        }
        Log.d(TAG, " stop end");
    }

    /*protected void finalize(){
         Log.d(TAG, " finalize");
        release();
    }*/

    private void ExtratorThread() {
        Log.d(TAG, "ExtratorThread start mExtractor:" + mExtractor);
        long timeout = 500 * 1000000;

        init();
        if (mExtractor == null && mFile == null) {

            Log.e(TAG, "ExtratorThread parameters invalid filepath(" + mFile + ")");
            return;
        }
        Log.d(TAG, "ExtratorThread mExtractor(" + mExtractor + ")");

        while (!mStop) {
            if (VERBOSE) Log.d(TAG, "ExtratorThread loop");


            try {
                mLock.lock();
                while (!mStop && (mExtractorDone || (mVideoQueue.size() > 16 && mAudioQueue.size() > 64)) && mSeektimeUs < 0) {
                    if (VERBOSE) Log.d(TAG, "ExtratorThread wait..");
                    mCondition.awaitNanos(timeout);
                    //mCondition.await();
                }
            } catch (Exception e) {
                Log.e(TAG, "ExtratorThread Exception:" + e);
                e.printStackTrace();

            } finally {
                //Log.d(TAG,"ExtratorThread unlock");
                mLock.unlock();
            }
            mLock.lock();
            if (mStop) {
                if (VERBOSE) Log.d(TAG, "ExtratorThread is stop...");
                mLock.unlock();
                break;
            }
            long seektime = -1;
            int seekmode = MediaExtractor.SEEK_TO_NEXT_SYNC;
            if (mSeektimeUs >= 0) {
                seektime = mSeektimeUs;
                seekmode = mSeekMode;
                mCurrReadPts = mSeektimeUs;
                mAudioNewReadPts = mSeektimeUs;
                mSeektimeUs = -1;
                mReadIsFailed = false;
            }
            mLock.unlock();

            if (seektime >= 0) {
                Log.d(TAG, " ExtratorThread seek:" + seektime);
                mExtractor.seekTo(seektime, seekmode);
                mAudioLock.lock();
                try {
                    if (!mAudioSeekDone) {
                        mAudioQueue.clear();
                        mAudioSeekDone = true;
                        mAudioCondition.signalAll();
                    }
                } catch (Exception e) {
                    Log.e(TAG, "ExtratorThread Audio Exception:" + e);
                    e.printStackTrace();
                } finally {
                    mAudioLock.unlock();
                }
                mVideoLock.lock();
                try {
                    if (!mVideoSeekDone) {
                        mVideoQueue.clear();
                        mVideoSeekDone = true;
                        mVideoCondition.signalAll();
                    }
                } catch (Exception e) {
                    Log.e(TAG, "ExtratorThread Video Exception:" + e);
                    e.printStackTrace();
                } finally {
                    mVideoLock.unlock();
                }


                Log.d(TAG, "ExtratorThread seek:" + seektime + " end.contiue...");
                continue;
            }

            if (VERBOSE) Log.d(TAG, "ExtratorThread loop 2");

            if (VERBOSE)
                Log.d(TAG, "ExtratorThread audiosize:" + mAudioQueue.size() + " videosize:" + mVideoQueue.size());
            Frame frame = readFrameFromExtractor();
            if (frame == null) {

                Log.e(TAG, "readFrameFromExtractor return null extractorDone:" + mExtractorDone);

                mAudioLock.lock();
                try {
                    mAudioCondition.signalAll();
                } catch (Exception e) {
                    Log.e(TAG, "ExtratorThread Exception:" + e);
                    e.printStackTrace();
                } finally {
                    mAudioLock.unlock();
                }

                mVideoLock.lock();
                try {
                    mVideoCondition.signalAll();
                } catch (Exception e) {
                    Log.e(TAG, "ExtratorThread Exception:" + e);
                    e.printStackTrace();
                } finally {
                    mVideoLock.unlock();
                }

                continue;
            }

            if (frame.index == mAudioIndex) {
                //TODO
                mAudioLock.lock();
                try {
                    if (mAudioQueue.size() >= mAudioMax) {
                        if (enableAudio)
                            Log.e(TAG, "mAudioQueue is full FFFFF,size:" + mAudioQueue.size());
                    } else {       //Log.d(TAG,"mAudioQueue EEEEE,size:"+mAudioQueue.size());
                        mAudioQueue.offer(frame);
                    }
                    mAudioCondition.signalAll();
                } catch (Exception e) {
                    Log.e(TAG, "ExtratorThread Exception:" + e);
                    e.printStackTrace();
                } finally {
                    mAudioLock.unlock();
                }
            } else if (frame.index == mVideoIndex) {
                //TODO
                mVideoLock.lock();
                try {
                    if (mVideoQueue.size() >= mVideoMax) {

                        Log.e(TAG, "mVideoQueue is full FFFFF,size:" + mVideoQueue.size());
                        //break;
                    } else {
                        //Log.d(TAG,"mVideoQueue EEEEE,size:"+mVideoQueue.size());
                        mVideoQueue.offer(frame);
                    }
                    mVideoCondition.signalAll();
                } catch (Exception e) {
                    Log.e(TAG, "ExtratorThread Exception:" + e);
                    e.printStackTrace();
                } finally {
                    mVideoLock.unlock();
                }
            }
        }
        Log.e(TAG, "ExtratorThread end");
    }

    private void StartThread() {
        Log.i(TAG, "====StartThread entry");
        Runnable runnable = new Runnable() {

            @Override
            public void run() {
                Log.i(TAG, "====run: ExtratorThread start");
                mLock.lock();
                if (mThreadIsRun) {
                    mLock.unlock();
                    Log.d(TAG, "run:ExtratorThread start faile,already started");
                    return;
                }
                mThreadIsRun = true;
                mLock.unlock();
                //Looper.prepare();
                try {
                    Log.i(TAG, "====call ExtratorThread");
                    mStop = false;
                    ExtratorThread();
                    Log.i(TAG, "====call ExtratorThread end");
                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    Log.e(TAG, "ExtratorThread Exception:" + e);
                    e.printStackTrace();
                } finally {

                    mVideoLock.lock();
                    mVideoQueue.clear();
                    //mVideoQueue = null;
                    mVideoLock.unlock();

                    mAudioLock.lock();
                    mAudioQueue.clear();
                    //mAudioQueue  = null;
                    mAudioLock.unlock();

                    mLock.lock();
                    MediaExtractor extractor = mExtractor;
                    mExtractor = null;
                    mThreadIsRun = false;
                    mLock.unlock();

                    if (extractor != null) {
                        extractor.release();
                        extractor = null;
                    }
                    Log.i(TAG, "ExtratorThread finally");
                }
            }
        };
        new Thread(runnable).start();
//        KGThreadPool.getInstance().execute(runnable); // 使用线程池

        //mVideoThread = new Thread(runnable);
        //mVideoThread.start();
        Log.i(TAG, "====StartThread end");
    }

    public long getDuration() {
        MediaExtractor extractor = getExtractor();
        long duration = -1;
        try {
            if (extractor != null) {
                int trackIndex = getAudioIndex();
                if (trackIndex < 0) {
                    trackIndex = getVideoIndex();
                }
                MediaFormat inputFormat = extractor.getTrackFormat(trackIndex);
                duration = inputFormat.getLong(MediaFormat.KEY_DURATION);
            }
            Log.d(TAG, "duration:" + duration);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return duration;
    }

    public int getHeight() {
        MediaExtractor extractor = getExtractor();
        int height = 0;

        try {
            if (extractor != null) {
                MediaFormat inputFormat = extractor.getTrackFormat(getVideoIndex());
                height = inputFormat.getInteger(MediaFormat.KEY_HEIGHT);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return height;
    }

    public int getFrameRate() {
        MediaExtractor extractor = getExtractor();
        int rate = 0;

        try {
            if (extractor != null) {
                MediaFormat inputFormat = extractor.getTrackFormat(getVideoIndex());
                rate = inputFormat.getInteger(MediaFormat.KEY_FRAME_RATE);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return rate;
    }

    public int getBitRate() {
        MediaExtractor extractor = getExtractor();
        int rate = 0;

        try {
            if (extractor != null) {
                MediaFormat inputFormat = extractor.getTrackFormat(getVideoIndex());
                rate = inputFormat.getInteger(MediaFormat.KEY_BIT_RATE);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return rate;
    }

    public int getRotate() {
        MediaExtractor extractor = getExtractor();
        int rotationDegrees = 0;
        try {
            if (extractor != null) {
                MediaFormat inputFormat = extractor.getTrackFormat(getVideoIndex());
                if (inputFormat.containsKey("rotation-degrees")) {
                    rotationDegrees = inputFormat.getInteger("rotation-degrees");
                } else {
                    MediaMetadataRetriever mmr = new MediaMetadataRetriever();
                    mmr.setDataSource(mFile);
                    rotationDegrees = Integer.valueOf(mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return rotationDegrees;
    }


    public int getWidth() {
        MediaExtractor extractor = getExtractor();
        int width = 0;
        try {
            if (extractor != null) {
                MediaFormat inputFormat = extractor.getTrackFormat(getVideoIndex());
                width = inputFormat.getInteger(MediaFormat.KEY_WIDTH);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return width;
    }


    private static boolean isVideoFormat(MediaFormat format) {
        return getMimeTypeFor(format).startsWith("video/");
    }

    private static boolean isAudioFormat(MediaFormat format) {
        return getMimeTypeFor(format).startsWith("audio/");
    }

    private static String getMimeTypeFor(MediaFormat format) {
        return format.getString(MediaFormat.KEY_MIME);
    }

    public MediaFormat getTrackFormat(int index) {
        MediaExtractor extractor = getExtractor();
        if (extractor != null && index >= 0) {
            return extractor.getTrackFormat(index);
        } else {
            return null;
        }
    }

    public int getAndSelectVideoTrackIndex(MediaExtractor extractor) {

        //MediaExtractor extractor =  getExtractor();
        if (extractor == null) {
            Log.e(TAG, "getAndSelectVideoTrackIndex extractor is null");
            return -1;
        }

        Log.d(TAG, "getAndSelectVideoTrackIndex getTrackCount" + extractor.getTrackCount());
        for (int index = 0; index < extractor.getTrackCount(); ++index) {

            Log.d(TAG, "format for track " + index + " is "
                    + getMimeTypeFor(extractor.getTrackFormat(index)));

            if (isVideoFormat(extractor.getTrackFormat(index))) {
                extractor.selectTrack(index);
                return index;
            }
        }
        return -1;
    }

    public int getAndSelectAudioTrackIndex(MediaExtractor extractor) {
        if (extractor == null) {
            Log.e(TAG, "getAndSelectAudioTrackIndex extractor is null");
            return -1;
        }


        Log.d(TAG, "getAndSelectAudioTrackIndex getTrackCount" + extractor.getTrackCount());
        for (int index = 0; index < extractor.getTrackCount(); ++index) {

            Log.d(TAG, "format for track " + index + " is "
                    + getMimeTypeFor(extractor.getTrackFormat(index)));

            if (isAudioFormat(extractor.getTrackFormat(index))) {
                extractor.selectTrack(index);
                return index;
            }
        }
        return -1;
    }

    public void setEableAudioTrack(boolean enable) {
        enableAudio = enable;
//		if(!enable){
//			mAudioMax=0;
//		}
    }
}
