package com.example.allaboutvideo.entity;

public class VideoInfoEntity {
    public final int id;
    public final String path;
    public final String thumb;
    public final long duration;
    public final long size;

    public VideoInfoEntity(int id, String path, String thumb, long duration, long size) {
        this.id = id;
        this.path = path;
        this.thumb = thumb;
        this.duration = duration;
        this.size = size;
    }
}
