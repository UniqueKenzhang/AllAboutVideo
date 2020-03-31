package com.example.allaboutvideo.filter;

import android.os.Parcel;
import android.os.Parcelable;

import java.io.Serializable;

public class ViewPort implements Parcelable, Serializable {
    public float pos_x;
    public float pos_y;
    public float size_y;
    public float size_x;
    public int grid_id;
    public int texture;
    //public FilterInfo filter;
    public String path;
    public int voice = -12;
    public int grid_video_id;//视频id


    //public PortUser shooter;
    //录制文件片段信息
    //public ArrayList<RecordFileInfo> recordFileInfos;


    //视频类型，标识下载，本地，录制
    public int videoType;
    public transient String md5;//上传成功后的Md5值，不需要要序列化

    public ViewPort() {
        this(0, 0, 1, 1);
    }

    public ViewPort(String path) {
        this(0, 0, 1, 1);
        this.path = path;
    }

    public ViewPort(float x, float y, float height, float width) {
        this.pos_x = x;
        this.pos_y = y;
        this.size_x = width;
        this.size_y = height;
    }

    protected ViewPort(Parcel in) {
        pos_x = in.readFloat();
        pos_y = in.readFloat();
        size_y = in.readFloat();
        size_x = in.readFloat();
        grid_id = in.readInt();
        texture = in.readInt();
        path = in.readString();
        voice = in.readInt();
        grid_video_id = in.readInt();
//        shooter = in.readParcelable(PortUser.class.getClassLoader());
//        recordFileInfos = in.createTypedArrayList(RecordFileInfo.CREATOR);
        videoType = in.readInt();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeFloat(pos_x);
        dest.writeFloat(pos_y);
        dest.writeFloat(size_y);
        dest.writeFloat(size_x);
        dest.writeInt(grid_id);
        dest.writeInt(texture);
        dest.writeString(path);
        dest.writeInt(voice);
        dest.writeInt(grid_video_id);
//        dest.writeParcelable(shooter, flags);
//        dest.writeTypedList(recordFileInfos);
        dest.writeInt(videoType);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<ViewPort> CREATOR = new Creator<ViewPort>() {
        @Override
        public ViewPort createFromParcel(Parcel in) {
            return new ViewPort(in);
        }

        @Override
        public ViewPort[] newArray(int size) {
            return new ViewPort[size];
        }
    };

    public void setPort(ViewPort p) {
        pos_x = p.pos_x;
        pos_y = p.pos_y;
        size_x = p.size_x;
        size_y = p.size_y;
        grid_id = p.grid_id;
        //voice = p.voice;
        grid_video_id = p.grid_video_id;
    }

    public void delete() {
        grid_video_id = 0;
    }

    public void setVoice(float vol) {
        this.voice = (int) (vol * 25 - 25);
    }

    public float getVoicePercent() {
        return (voice + 25.f) / 25.f;
    }
}
