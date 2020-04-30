package com.example.allaboutvideo.reencode;

import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.example.allaboutvideo.PageUtil;
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
        final TextView btn = findViewById(R.id.btn);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mediaPlayer.stop();

                ViewPort viewPort = new ViewPort();
                viewPort.path = mPath;

                VideoSource source = new VideoSource(viewPort);
                String dir = Environment.getExternalStorageDirectory() + "/AllAboutVideo";
                File file = new File(dir);
                if (!file.exists() || !file.isDirectory()) {
                    file.mkdir();
                }
                final String out = dir + "/out.mp4";
                try {
                    file = new File(out);
                    if (!file.exists()) {
                        file.createNewFile();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }


                MyRecorder myRecorder = new MyRecorder(source, out);
                new VideoFilterMixTask(source, myRecorder, BitmapFactory.decodeResource(getResources(), R.mipmap.smile), 0, new ProcessListener() {
                    @Override
                    public void onFinish() {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                btn.setText( "100%");
                                Toast.makeText(ReEncodeActivity.this, "reEncode finish", Toast.LENGTH_SHORT).show();
                                PageUtil.toPlayer(ReEncodeActivity.this, out);
                                finish();
                            }
                        });

                    }

                    @Override
                    public void onStart() {

                    }

                    @Override
                    public void onProcess(final int progress) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                btn.setText(progress+"%");
                            }
                        });

                    }

                    @Override
                    public void onError() {

                    }
                }).start();
            }
        });

    }
}
