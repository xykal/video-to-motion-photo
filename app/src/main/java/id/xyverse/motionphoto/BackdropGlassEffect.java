package id.xyverse.motionphoto;

import android.content.Context;
import android.opengl.GLES20;
import androidx.media3.common.util.Size;
import androidx.media3.common.VideoFrameProcessingException;
import androidx.media3.common.util.GlProgram;
import androidx.media3.common.util.GlUtil;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.effect.BaseGlShaderProgram;
import androidx.media3.effect.GlEffect;
import androidx.media3.effect.GlShaderProgram;

/** Blurs only the underlying video pixels inside the lower-left watermark panel. */
@UnstableApi
public final class BackdropGlassEffect implements GlEffect {
    @Override public GlShaderProgram toGlShaderProgram(Context context, boolean useHdr) {
        return new Shader();
    }

    private static final class Shader extends BaseGlShaderProgram {
        private static final String VERTEX = "attribute vec2 aPosition; varying vec2 vUv;"
                + "void main(){ gl_Position=vec4(aPosition,0.0,1.0); vUv=(aPosition+1.0)*0.5; }";
        private static final String FRAGMENT = "precision highp float; varying vec2 vUv;"
                + "uniform sampler2D uInput; uniform vec2 uTexel; uniform vec2 uPanel;"
                + "void main(){ vec4 src=texture2D(uInput,vUv);"
                + "vec2 p=(vUv-vec2(0.03,0.05))/uPanel;"
                + "vec2 q=abs(p-0.5)-vec2(0.46,0.38);"
                + "float edge=length(max(q,0.0))+min(max(q.x,q.y),0.0)-0.08;"
                + "float mask=1.0-smoothstep(-0.005,0.005,edge);"
                + "if(mask<0.001){gl_FragColor=src;return;}"
                + "vec4 blur=vec4(0.0);"
                + "blur+=texture2D(uInput,vUv+uTexel*vec2(-9.0,-9.0));"
                + "blur+=texture2D(uInput,vUv+uTexel*vec2(0.0,-9.0));"
                + "blur+=texture2D(uInput,vUv+uTexel*vec2(9.0,-9.0));"
                + "blur+=texture2D(uInput,vUv+uTexel*vec2(-9.0,0.0));"
                + "blur+=texture2D(uInput,vUv)*2.0;"
                + "blur+=texture2D(uInput,vUv+uTexel*vec2(9.0,0.0));"
                + "blur+=texture2D(uInput,vUv+uTexel*vec2(-9.0,9.0));"
                + "blur+=texture2D(uInput,vUv+uTexel*vec2(0.0,9.0));"
                + "blur+=texture2D(uInput,vUv+uTexel*vec2(9.0,9.0));"
                + "blur/=10.0; gl_FragColor=mix(src,vec4(blur.rgb,src.a),mask*0.83); }";
        private static final float[] QUAD = {-1f, -1f, 1f, -1f, -1f, 1f, 1f, 1f};
        private GlProgram program;
        private int width, height;

        Shader() { super(false, 1); }

        @Override public Size configure(int width, int height) throws VideoFrameProcessingException {
            this.width = width;
            this.height = height;
            try {
                if (program == null) program = new GlProgram(VERTEX, FRAGMENT);
            } catch (GlUtil.GlException error) {
                throw new VideoFrameProcessingException(error);
            }
            return new Size(width, height);
        }

        @Override public void drawFrame(int inputTexId, long presentationTimeUs) throws VideoFrameProcessingException {
            try {
                program.use();
                program.setBufferAttribute("aPosition", QUAD, 2);
                program.setSamplerTexIdUniform("uInput", inputTexId, 0);
                program.setFloatsUniform("uTexel", new float[]{1f / width, 1f / height});
                program.setFloatsUniform("uPanel", new float[]{Math.min(800f, width - 24f) / width,
                        Math.min(800f, width - 24f) * .22f / height});
                program.bindAttributesAndUniforms();
                GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
            } catch (GlUtil.GlException error) {
                throw new VideoFrameProcessingException(error, presentationTimeUs);
            }
        }

        @Override public void release() throws VideoFrameProcessingException {
            super.release();
            try { if (program != null) program.delete(); }
            catch (GlUtil.GlException error) { throw new VideoFrameProcessingException(error); }
        }
    }
}
