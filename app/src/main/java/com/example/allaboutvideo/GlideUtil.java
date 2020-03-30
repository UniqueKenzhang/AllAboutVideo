package com.example.allaboutvideo;

import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.view.ViewTreeObserver;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation;
import com.bumptech.glide.request.RequestListener;

/**
 * 图片加载工具，并加上.dontAnimate()，不使用glide的动画，这样能出现边框以及圆角图片第一次能显示
 */
public class GlideUtil {

    /**
     * 加载图片
     *
     * @param url
     * @param imageView
     * @param placeholder          加载中的图片
     * @param error                加载中的失败的图片
     * @param bitmapTransformation 图片效果 比如圆角
     */
    public static void loadImage(final String url, final ImageView imageView, final int placeholder, final int error, final BitmapTransformation bitmapTransformation, final RequestListener<Drawable> listener) {

        if (imageView.getWidth() <= 0) {
            imageView.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                @Override
                public boolean onPreDraw() {
                    if (imageView.getWidth() > 0) {
                        imageView.getViewTreeObserver().removeOnPreDrawListener(this);
                        excuteImageLoad(url, imageView, placeholder, error, bitmapTransformation, listener);
                    }
                    return true;
                }
            });

        } else {
            excuteImageLoad(url, imageView, placeholder, error, bitmapTransformation, listener);
        }
    }

    public static void excuteImageLoad(String url, final ImageView imageView, int placeholder, int error, BitmapTransformation bitmapTransformation, RequestListener<Drawable> listener) {
        RequestBuilder<Drawable> drawableRequestBuilder = Glide.with(imageView.getContext())
                .load(Uri.parse(url))
                .dontAnimate()
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .listener(listener);

        if (placeholder > 0) {
            drawableRequestBuilder = drawableRequestBuilder.placeholder(placeholder);
        }

        if (error > 0) {
            drawableRequestBuilder = drawableRequestBuilder.error(error);
        }

        if (bitmapTransformation != null) {
            drawableRequestBuilder = drawableRequestBuilder.transform(bitmapTransformation);
        }

        if (imageView.getWidth() > 0 && imageView.getWidth() < 500) {
            drawableRequestBuilder = drawableRequestBuilder.override(imageView.getWidth(), imageView.getHeight());
        }

        drawableRequestBuilder.into(imageView);

    }

}
