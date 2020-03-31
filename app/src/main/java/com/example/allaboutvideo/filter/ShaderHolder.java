package com.example.allaboutvideo.filter;

public class ShaderHolder {


    public static String vStr =
            "attribute vec4 vPosition;" +
                    "attribute vec2 vCoordinate;" +
                    "varying vec2 aCoordinate;" +
                    "uniform mat4 vMatrix;" +
                    "void main() { " +
                    "   gl_Position = vMatrix*vPosition;" +
                    "   aCoordinate=vCoordinate;" +
                    "}";
    public static String fStr =
            " precision mediump float;" +
                    "varying vec2 vTextureCo;" +
                    "uniform sampler2D uTexture;" +
//                    "uniform lowp float fAlpha;" +
                    "void main() { " +
                    "   vec4 org = texture2D(uTexture,vTextureCo);" +
//                    "   org.a = fAlpha;" +
                    "   gl_FragColor= org;" +
                    "}";

    // Y = 0.299R + 0.587G + 0.114B
    public static String fStr2 =
            " precision mediump float;" +
                    "uniform sampler2D vTexture; " +
                    "varying vec2 aCoordinate;" +
                    "uniform lowp float fAlpha;" +
                    "uniform lowp float fcr;" +
                    "uniform lowp float fcg;" +
                    "uniform lowp float fcb;" +
                    "void main() { " +
                    "   vec4 org = texture2D(vTexture,aCoordinate);" +
                    "   org.a = fcb*org.b+ fcr*org.r+ fcg*org.g;" +
                    "   gl_FragColor= org;" +
                    "}";

    public static String fStr1 =
            //step(fPercent,(1.0 - aCoordinate.x) + (1.0 - aCoordinate.y))
            " precision mediump float; " +
                    "    varying vec2 aCoordinate;" +
                    "    uniform sampler2D vTexture;" +
                    "    uniform lowp float fAlpha;" +
                    "    uniform float fPercent;" +
                    "    vec4 trans = vec4(0.0,0.0,0.0,0.0);" +
                    "    void main(){" +
                    "        vec4 c = texture2D(vTexture,aCoordinate);" +
                    "        c.a = fAlpha;" +
                    "        lowp float temp =step(1.0,step(aCoordinate.x + aCoordinate.y,fPercent)+ step((1.0 - aCoordinate.x) + (1.0 - aCoordinate.y),fPercent))    ;" +
                    "        gl_FragColor = c*temp+trans*(1.0-temp);" +
                    "    }";

    public static String fStr3 =
            //step(fPercent,(1.0 - aCoordinate.x) + (1.0 - aCoordinate.y))
            " precision mediump float; " +
                    "    varying vec2 aCoordinate;" +
                    "    uniform sampler2D vTexture;" +
                    "    uniform lowp float fAlpha;" +
                    "    uniform float fPercent;" +
                    "    vec4 trans = vec4(0.0,0.0,0.0,0.0);" +
                    "    vec2 center = vec2(0.5,0.3);" +
                    "    void main(){" +
                    "        vec4 c = texture2D(vTexture,aCoordinate);" +
                    "        c.a = fAlpha;" +
                    "        lowp float r =  0.04+(1.0-fPercent)*0.5;" +
                    "        lowp float x = aCoordinate.x-center.x;" +
                    "        lowp float y = aCoordinate.y-center.y;" +
                    "        lowp float temp =step(x*x+ y* y,r);" +
                    "        gl_FragColor = c*temp+trans*(1.0-temp);" +
                    "    }";

    public static String fStr4 =
            //step(fPercent,(1.0 - aCoordinate.x) + (1.0 - aCoordinate.y))
            " precision mediump float; " +
                    "    varying vec2 aCoordinate;" +
                    "    uniform sampler2D vTexture;" +
                    "    uniform lowp float fAlpha;" +
                    "    uniform float fPercent;" +
                    "    vec4 trans = vec4(0.0,0.0,0.0,0.0);" +
                    "    vec2 center = vec2(0.5,0.3);" +
                    "    void main(){" +
                    "        vec4 c = texture2D(vTexture,aCoordinate);" +
                    "        c.a = fAlpha;" +
                    "        lowp float r = pow((1.0-fPercent)*0.8+0.2,2.0);" +
                    "        lowp float x = aCoordinate.x-center.x;" +
                    "        lowp float y = center.y-aCoordinate.y;" +
                    "        lowp float temp =step(pow(x,2.0)+ pow(y-pow(pow(x,2.0),0.66),2.0),r);" +
                    "        gl_FragColor = c*temp+trans*(1.0-temp);" +
                    "    }";

    public static String fStr5 =
            "    precision mediump float; " +
                    "    varying vec2 aCoordinate;" +
                    "    uniform sampler2D vTexture;" +
                    "    void main(){" +
                    "       vec4 c = vec4(0.0,0.0,0.0,1.0);" +
                    "       float r = 24.0;" +
                    "       float count = 0.0;" +
                    "       for(float x = -r; x <= r; x += 2.0) { " +
                    "            float weight = (r - abs(x));" +
                    "            c += texture2D(vTexture, aCoordinate + vec2(x/1080.0 , 0.0)) * weight;" +
                    "            count += weight;" +
                    "       }" +
                    "       gl_FragColor =c/count;" +
                    "   }";

    public static String fStr6 =
            "    precision mediump float; " +
                    "    varying vec2 vTextureCo;" +
                    "    uniform sampler2D uTexture;" +
                    "    void main(){" +
                    "       vec4 c = vec4(0.0,0.0,0.0,1.0);" +
                    "       float r = 24.0;" +
                    "       float count = 0.0;" +
                    "       for(float y = -r; y <= r; y +=2.0){" +
                    "            float weight = (r - abs(y));" +
                    "            c += texture2D(uTexture, vTextureCo + vec2(0.0 , y/1080.0)) * weight;" +
                    "            count += weight;" +
                    "       }" +
                    "       gl_FragColor =c/count;" +
                    "   }";
}
