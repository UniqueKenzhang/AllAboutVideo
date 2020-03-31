package com.example.allaboutvideo.home;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.allaboutvideo.R;
import com.example.allaboutvideo.choosevideo.VideoChooseActivity;
import com.example.allaboutvideo.gl.GLDiceActivity;
import com.example.allaboutvideo.reencode.ReEncodeActivity;

public class MainActivity extends AppCompatActivity {

    String[] funs = {
            "opengl小栗子",
            "简单利用opengl渲染播放",
            "重编码",
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);
        RecyclerView content = findViewById(R.id.content);
        content.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        content.setAdapter(new RecyclerView.Adapter<ViewHolder>() {
            @NonNull
            @Override
            public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View inflate = LayoutInflater.from(MainActivity.this).inflate(R.layout.item_main_list, parent, false);
                return new ViewHolder(inflate);
            }

            @Override
            public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
                holder.txt.setText(funs[position]);
            }

            @Override
            public int getItemCount() {
                return funs.length;
            }
        });

    }


    private class ViewHolder extends RecyclerView.ViewHolder {

        final TextView txt;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            txt = itemView.findViewById(R.id.txt);
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    switch (getAdapterPosition()) {
                        case 0:
                            startActivity(new Intent(MainActivity.this, GLDiceActivity.class));
                            break;
                        case 1: {
                            Intent intent = new Intent(MainActivity.this, VideoChooseActivity.class);
                            intent.putExtra(VideoChooseActivity.JUMP_TYEP, 0);
                            startActivity(intent);
                            break;
                        }
                        case 2: {
                            Intent intent = new Intent(MainActivity.this, VideoChooseActivity.class);
                            intent.putExtra(VideoChooseActivity.JUMP_TYEP, 1);
                            startActivity(intent);
                            break;
                        }
                    }
                }
            });
        }
    }
}
