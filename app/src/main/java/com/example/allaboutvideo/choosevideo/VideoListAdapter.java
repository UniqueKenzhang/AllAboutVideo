package com.example.allaboutvideo.choosevideo;

import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.request.RequestOptions;
import com.example.allaboutvideo.GlideUtil;
import com.example.allaboutvideo.R;
import com.example.allaboutvideo.base.PageParam;
import com.example.allaboutvideo.entity.VideoInfoEntity;
import com.example.allaboutvideo.reencode.ReEncodeActivity;
import com.example.allaboutvideo.simpleplay.SimplePlayActivity;
import com.example.allaboutvideo.videofilter.VideoFilterActivity;

import java.io.File;
import java.util.List;

public class VideoListAdapter extends RecyclerView.Adapter<VideoListAdapter.ViewHolder> {

    private final List<VideoInfoEntity> data;
    private final int type;

    public VideoListAdapter(List<VideoInfoEntity> data, int type) {
        this.data = data;
        this.type = type;
    }

    @NonNull
    @Override
    public VideoListAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View inflate = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_video_list, parent, false);
        return new ViewHolder(inflate);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        VideoInfoEntity entity = data.get(position);
        if (!TextUtils.isEmpty(entity.thumb)) {
            GlideUtil.loadImage(entity.thumb, holder.img, 0, 0, new CenterCrop(), null);
        } else {
            Glide.with(holder.img.getContext())
                    .load(Uri.fromFile(new File(entity.path)))
                    .apply(new RequestOptions().frame(0)).into(holder.img);
        }

    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    public void setData(List<VideoInfoEntity> list) {
        data.clear();
        data.addAll(list);
        notifyDataSetChanged();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        ImageView img;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            img = itemView.findViewById(R.id.image);
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = null;
                    VideoInfoEntity videoInfoEntity = data.get(getAdapterPosition());
                    switch (type) {
                        case 0: {
                            intent = new Intent(view.getContext(), SimplePlayActivity.class);
                            break;
                        }
                        case 1: {
                            intent = new Intent(view.getContext(), ReEncodeActivity.class);
                            break;
                        }
                        case 2: {
                            intent = new Intent(view.getContext(), VideoFilterActivity.class);
                            break;
                        }

                    }

                    if (intent != null) {
                        intent.putExtra(PageParam.VIDEO_PATH, videoInfoEntity.path);
                        view.getContext().startActivity(intent);
                    }
                }
            });
        }
    }
}
