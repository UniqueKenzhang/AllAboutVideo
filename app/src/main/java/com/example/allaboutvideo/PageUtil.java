package com.example.allaboutvideo;

import android.app.Activity;
import android.content.Intent;

import com.example.allaboutvideo.base.PageParam;
import com.example.allaboutvideo.simpleplay.SimplePlayActivity;

public class PageUtil {

    public static void toPlayer(Activity act, String path) {
        Intent intent = new Intent(act, SimplePlayActivity.class);
        intent.putExtra(PageParam.VIDEO_PATH, path);
        act.startActivity(intent);
    }
}
