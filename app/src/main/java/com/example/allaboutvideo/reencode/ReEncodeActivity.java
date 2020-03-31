package com.example.allaboutvideo.reencode;

import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Toast;

import com.example.allaboutvideo.R;
import com.example.allaboutvideo.codec.MyRecorder;
import com.example.allaboutvideo.codec.ProcessListener;
import com.example.allaboutvideo.codec.VideoFilterMixTask;
import com.example.allaboutvideo.codec.VideoSource;
import com.example.allaboutvideo.filter.ViewPort;
import com.example.allaboutvideo.simpleplay.SimplePlayActivity;

import java.io.File;

public class ReEncodeActivity extends SimplePlayActivity {

    @Override
    public void setContentView(int layoutResID) {
        super.setContentView(R.layout.activity_re_encode);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        findViewById(R.id.btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ViewPort viewPort = new ViewPort();
                viewPort.path = mPath;

                VideoSource source = new VideoSource(viewPort);
                String dir = Environment.getExternalStorageDirectory() + "/AllAboutVideo";
                File file = new File(dir);
                if (!file.exists() || !file.isDirectory()) {
                    file.mkdir();
                }
                String out = dir + "/out.mp4";
                try {
                    file = new File(out);
                    if (!file.exists()) {
                        file.createNewFile();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }


                MyRecorder myRecorder = new MyRecorder(source.w, source.h, source.bitRate, 2, source.fps, out);
                new VideoFilterMixTask(source, myRecorder, BitmapFactory.decodeResource(getResources(), R.mipmap.smile), 0, new ProcessListener() {
                    @Override
                    public void onFinish() {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(ReEncodeActivity.this, "finish", Toast.LENGTH_SHORT).show();
                            }
                        });

                    }

                    @Override
                    public void onStart() {

                    }

                    @Override
                    public void onProcess(int progress) {

                    }

                    @Override
                    public void onError() {

                    }
                }).start();
            }
        });

    }
}
