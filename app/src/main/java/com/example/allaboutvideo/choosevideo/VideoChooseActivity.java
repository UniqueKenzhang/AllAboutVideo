package com.example.allaboutvideo.choosevideo;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.allaboutvideo.R;
import com.example.allaboutvideo.entity.VideoInfoEntity;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class VideoChooseActivity extends AppCompatActivity {

    public static final String VIDEO_SEL = MediaStore.Video.Media.MIME_TYPE + "=? and "
            + MediaStore.Video.Media.DURATION + ">? AND "
            + MediaStore.Video.Media.DURATION + "<?";

    private final String[] VIDEO_PROJ = new String[]{
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DATA,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DATE_MODIFIED
    };
    private RecyclerView mContent;
    private VideoListAdapter mAdapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);
        mContent = findViewById(R.id.content);

        mContent.setLayoutManager(new GridLayoutManager(this, 3));
        ArrayList<VideoInfoEntity> data = new ArrayList<>();
        mAdapter = new VideoListAdapter(data);
        mContent.setAdapter(mAdapter);

        getLocalVideoData();
    }

    private void getLocalVideoData() {

        new Thread(new Runnable() {
            @Override
            public void run() {
                Uri mVideoUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                ContentResolver mContentResolver = getContentResolver();
                String[] SEL_ARGS_VIDEO = new String[]{
                        "video/mp4",
                        String.valueOf(5 * 1000),
                        String.valueOf(6000 * 1000)
                };
                Cursor mCursor = mContentResolver.query(mVideoUri, VIDEO_PROJ, VIDEO_SEL, SEL_ARGS_VIDEO, MediaStore.Video.Media.DATE_MODIFIED + " desc");

                if (mCursor == null) {
                    return;
                }

                final List<VideoInfoEntity> list = new ArrayList<>();
                while (mCursor.moveToNext()) {

                    //获取 视频信息
                    long duration = mCursor.getInt(mCursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)); // 时长
                    //if(duration<=601000){
                    int id = mCursor.getInt(mCursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)); // id
                    String path = mCursor.getString(mCursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)); // 路径
                    long size = mCursor.getLong(mCursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)); // 大小

                    if (size < 0) {
                        //某些设备获取size<0，直接计算
                        size = new File(path).length() / 1024;
                    }
                    String thumbPath = getThumbPath(id);
                    if (!TextUtils.isEmpty(thumbPath)) {
                        File thumbFile = new File(thumbPath);//会存在thumbPath不为空，但是并没有对应图片文件的情况
                        if (thumbFile.exists()) {
                            Uri uri = Uri.fromFile(new File(thumbPath));
                            thumbPath = uri == null ? "" : uri.toString();
                        } else {
                            thumbPath = null;
                        }
                    }
                    VideoInfoEntity entity = new VideoInfoEntity(id, path, thumbPath, duration, size);
                    list.add(entity);
                }

                mCursor.close();

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mAdapter.setData(list);
                    }
                });
            }
        }).start();
    }

    private String getThumbPath(int id) {
        String[] projection = {MediaStore.Video.Thumbnails._ID, MediaStore.Video.Thumbnails.DATA};
        Cursor cursor = getContentResolver().query(MediaStore.Video.Thumbnails.EXTERNAL_CONTENT_URI
                , projection
                , MediaStore.Video.Thumbnails.VIDEO_ID + "=?"
                , new String[]{id + ""}
                , null);
        String thumbPath = "";
        int thumbId = 0;
        while (cursor != null && cursor.moveToNext()) {
            thumbId = cursor.getInt(cursor.getColumnIndex(MediaStore.Video.Thumbnails._ID));
            thumbPath = cursor.getString(cursor.getColumnIndex(MediaStore.Video.Thumbnails.DATA));
        }
        if (cursor != null) {
            cursor.close();
        }
        return thumbPath;
    }
}
