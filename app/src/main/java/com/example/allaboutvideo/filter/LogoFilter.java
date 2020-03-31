package com.example.allaboutvideo.filter;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class LogoFilter extends BaseFilter {


    private int tex;

    public void initBuffer() {
        ByteBuffer vertex = ByteBuffer.allocateDirect(32);
        vertex.order(ByteOrder.nativeOrder());
        mVertexBuffer = vertex.asFloatBuffer();
        mVertexBuffer.put(new float[]{
                -1.0f, -1.0f,
                -1.0f, 1.0f,
                1.0f, -1.0f,
                1.0f, 1.0f
        });
        mVertexBuffer.position(0);
        ByteBuffer texture = ByteBuffer.allocateDirect(32);
        texture.order(ByteOrder.nativeOrder());
        mTextureBuffer = texture.asFloatBuffer();
        mTextureBuffer.put(MatrixUtils.getOriginalTextureCo());
        mTextureBuffer.position(0);
    }

    public LogoFilter() {
        super(null, BaseFilter.BASE_VERT,
                "precision mediump float;\n" +
                        "varying vec2 vTextureCo;\n" +
                        "uniform sampler2D uTexture;\n" +
                        "void main() {\n" +
                        "    gl_FragColor = texture2D(uTexture, vTextureCo);\n" +
                        "}");
    }

    @Override
    protected void onClear() {
    }

    public void setLogoTex(int tex) {
        this.tex = tex;
    }

    public int getTex() {
        return tex;
    }

    public void draw() {
        draw(tex);
    }
}
