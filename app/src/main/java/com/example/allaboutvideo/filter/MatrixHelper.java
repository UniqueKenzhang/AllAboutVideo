package com.example.allaboutvideo.filter;

import android.opengl.Matrix;
import android.widget.ImageView;

/**
 * Created by kenzhang on 2017/10/11.
 */

public class MatrixHelper {
    public static final int TYPE_FITXY = 0;
    public static final int TYPE_CENTERCROP = 1;
    public static final int TYPE_CENTERINSIDE = 2;
    public static final int TYPE_FITSTART = 3;
    public static final int TYPE_FITEND = 4;
    public static final float EYEZ = 2;


    private static float[] mProjMatrix = new float[16];// 4x4矩阵 存储投影矩阵
    private static float[] mVMatrix = new float[16];// 摄像机位置朝向9参数矩阵

    /*
     * 第一步 ：新建平移变换矩阵
     */
    private static float[] mtMatrix = new float[16];// 平移变换矩阵

    /*
     * 第二步: 初始化为单位矩阵
     */
    static {
        //初始化为单位矩阵
        Matrix.setIdentityM(mtMatrix, 0);
        setCamera(0, 0, EYEZ, 0, 0, 0, 0, 1, 0);
    }

    public static void setIdentityM() {
        Matrix.setIdentityM(mtMatrix, 0);
    }

    public static float[] getIdentityM() {
        Matrix.setIdentityM(mtMatrix, 0);
        return mtMatrix;
    }

    public static float[] getMatrix() {
        return mtMatrix;
    }

    /*
     * 第三步 : 平移变换方法共外部使用
     */
    public static void translate(float x, float y, float z)//设置沿xyz轴移动
    {
        //Matrix.setIdentityM(mtMatrix, 0);
        Matrix.translateM(mtMatrix, 0, x, y, z);
    }

    public static void rotate(float x, float y, float z, float a)//设置沿xyz轴移动
    {
//        Matrix.setIdentityM(mtMatrix, 0);
        Matrix.rotateM(mtMatrix, 0, a, x, y, z);
    }

    // 设置摄像机
    public static void setCamera(float cx, // 摄像机位置x
                                 float cy, // 摄像机位置y
                                 float cz, // 摄像机位置z
                                 float tx, // 摄像机目标点x
                                 float ty, // 摄像机目标点y
                                 float tz, // 摄像机目标点z
                                 float upx, // 摄像机UP向量X分量
                                 float upy, // 摄像机UP向量Y分量
                                 float upz // 摄像机UP向量Z分量
    ) {
        Matrix.setLookAtM(mVMatrix, 0, cx, cy, cz, tx, ty, tz, upx, upy, upz);
    }

    // 设置透视投影参数
    public static void setProjectFrustum(float left, // near面的left
                                         float right, // near面的right
                                         float bottom, // near面的bottom
                                         float top, // near面的top
                                         float near, // near面距离
                                         float far // far面距离
    ) {
        Matrix.frustumM(mProjMatrix, 0,
                left, right, bottom, top,
                near, far);
    }

    // 获取具体物体的总变换矩阵
    static float[] mMVPMatrix = new float[16];

    public static float[] getFinalMatrix() {

        /*
         * 第四步  : 乘以平移变换矩阵
         */
        Matrix.multiplyMM(mMVPMatrix, 0, mVMatrix, 0, mtMatrix, 0);
        Matrix.multiplyMM(mMVPMatrix, 0, mProjMatrix, 0, mMVPMatrix, 0);
        return mMVPMatrix;
    }

    /**
     * 根据预览的大小和图像的大小，计算合适的变换矩阵
     *
     * @param type       变换的类型，参考{@link #TYPE_CENTERCROP}、{@link #TYPE_FITEND}、{@link #TYPE_CENTERINSIDE}、{@link #TYPE_FITSTART}、{@link #TYPE_FITXY}，对应{@link ImageView}的{@link ImageView#setScaleType(ImageView.ScaleType)}
     * @param imgWidth   图像的宽度
     * @param imgHeight  图像的高度
     * @param viewWidth  视图的宽度
     * @param viewHeight 视图的高度
     */
    public static void getMatrix(int type, int imgWidth, int imgHeight, int viewWidth, int viewHeight, float angle) {
        if (imgHeight > 0 && imgWidth > 0 && viewWidth > 0 && viewHeight > 0) {
            float[] projection = mProjMatrix;
            float[] camera = mVMatrix;
            if (type == TYPE_FITXY) {
                Matrix.orthoM(projection, 0, -1, 1, -1, 1, 1, 3);
                Matrix.setLookAtM(camera, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0);
                Matrix.multiplyMM(mMVPMatrix, 0, projection, 0, camera, 0);
                return;
            }
            float sWhView = (float) viewWidth / viewHeight;
            float sWhImg = (float) imgWidth / imgHeight;
            if (sWhImg > sWhView) {
                switch (type) {
                    case TYPE_CENTERCROP:
                        Matrix.frustumM(projection, 0, -sWhView / sWhImg, sWhView / sWhImg, -1, 1, 1, 10);
                        break;
                    case TYPE_CENTERINSIDE:
                        Matrix.orthoM(projection, 0, -1, 1, -sWhImg / sWhView, sWhImg / sWhView, 1, 10);
                        break;
                    case TYPE_FITSTART:
                        Matrix.orthoM(projection, 0, -1, 1, 1 - 2 * sWhImg / sWhView, 1, 1, 10);
                        break;
                    case TYPE_FITEND:
                        Matrix.orthoM(projection, 0, -1, 1, -1, 2 * sWhImg / sWhView - 1, 1, 10);
                        break;
                    default:
                        break;
                }
            } else {
                switch (type) {
                    case TYPE_CENTERCROP:
                        Matrix.frustumM(projection, 0, -1, 1, -sWhImg / sWhView, sWhImg / sWhView, 1, 10);
                        break;
                    case TYPE_CENTERINSIDE:
                        Matrix.orthoM(projection, 0, -sWhView / sWhImg, sWhView / sWhImg, -1, 1, 1, 10);
                        break;
                    case TYPE_FITSTART:
                        Matrix.orthoM(projection, 0, -1, 2 * sWhView / sWhImg - 1, -1, 1, 1, 10);
                        break;
                    case TYPE_FITEND:
                        Matrix.orthoM(projection, 0, 1 - 2 * sWhView / sWhImg, 1, -1, 1, 1, 10);
                        break;
                    default:
                        break;
                }
            }
            Matrix.setLookAtM(camera, 0, 0, 0, EYEZ, 0, 0, 0, (float) Math.sin(angle), (float) Math.cos(angle), 0);

            Matrix.multiplyMM(mMVPMatrix, 0, mVMatrix, 0, mtMatrix, 0);
            Matrix.multiplyMM(mMVPMatrix, 0, mProjMatrix, 0, mMVPMatrix, 0);
        }
    }
}
