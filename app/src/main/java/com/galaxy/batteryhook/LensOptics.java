package com.galaxy.batteryhook;

import android.graphics.RuntimeShader;
import android.graphics.Shader;

// Only constructed on API 33+ with a hardware Canvas. The optical input is the
// analytically reconstructed local tile surface, not a screen capture.
final class LensOptics {
    private static final String CODE="""
        uniform shader pigment;
        uniform float4 bounds;
        uniform float3 centers;
        uniform float halfTile;
        uniform float corner;
        uniform float density;
        uniform float strength;
        uniform float motion;

        float tileSurface(float2 p) {
            float dx=min(abs(p.x-centers.x),min(abs(p.x-centers.y),abs(p.x-centers.z)));
            float cy=(bounds.y+bounds.w)*0.5;
            float hy=max(1.0,(bounds.w-bounds.y)*0.5);
            float r=min(corner,min(halfTile,hy));
            float2 q=float2(dx,abs(p.y-cy))-float2(halfTile-r,hy-r);
            float sd=length(max(q,float2(0.0)))+min(max(q.x,q.y),0.0)-r;
            return 1.0-smoothstep(-density,density,sd);
        }
        half4 main(float2 p) {
            float2 c=(bounds.xy+bounds.zw)*0.5;
            float2 size=max((bounds.zw-bounds.xy)*0.5,float2(1.0));
            float2 q=(p-c)/size;
            float across=clamp(max(abs(q.x),abs(q.y)),0.0,1.0);
            float edge=pow(across,3.5);
            float2 n=q/max(length(q),0.001);
            float2 bend=n*edge*density*(4.0+5.0*motion)*strength;
            float seam=tileSurface(p+bend)-tileSurface(p);
            half4 base=pigment.eval(p);
            float top=max(0.0,-q.y);
            float bottom=max(0.0,q.y);
            float rim=pow(across,9.0);
            float shine=pow(top,5.0)*0.11+pow(bottom,6.0)*0.038;
            float directional=clamp(0.55-0.26*q.x-0.28*q.y,0.0,1.0);
            float reflection=shine+rim*directional*0.10;
            float3 color=float3(base.rgb)+float3(seam*0.075*strength);
            color=mix(color,float3(1.0),clamp(reflection*strength,0.0,0.23));
            return half4(half3(clamp(color,0.0,1.0)),1.0);
        }
        """;
    private final RuntimeShader effect=new RuntimeShader(CODE);
    private Shader lastPigment;
    private final float[] bounds=new float[4];
    Shader shader(Shader pigment,float left,float top,float right,float bottom,float[] centers,float halfTile,float corner,float density,int style,float energy){
        if(lastPigment!=pigment){effect.setInputShader("pigment",pigment);lastPigment=pigment;}
        bounds[0]=left;bounds[1]=top;bounds[2]=right;bounds[3]=bottom;
        effect.setFloatUniform("bounds",bounds);effect.setFloatUniform("centers",centers);
        effect.setFloatUniform("halfTile",halfTile);effect.setFloatUniform("corner",corner);effect.setFloatUniform("density",Math.max(.1f,density));
        effect.setFloatUniform("strength",style==0?.6f:style==2?1.12f:.85f);effect.setFloatUniform("motion",GlassMath.clamp(energy,0,1));
        return effect;
    }
}
