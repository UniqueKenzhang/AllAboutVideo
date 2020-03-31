package com.example.allaboutvideo.codec;

public abstract class ProcessListener {

    public abstract void onFinish();

    public abstract void onStart();

    public abstract void onProcess(int progress);

    public abstract void onError();

//    void onOverLage() {
//        ToastUtil.show(CommonApplication.getAppContext(), "抱歉，您的视频清晰度过高，暂不支持上传");
//    }
}
