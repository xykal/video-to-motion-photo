package id.xyverse.motionphoto;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.View;

/** A small custom progress meter with no platform progress-bar chrome. */
public final class ProgressMeter extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private float fraction;

    public ProgressMeter(Context context) { super(context); setContentDescription("Progres ekspor 0 persen"); }

    public void setPercent(int percent) {
        fraction = Math.max(0, Math.min(100, percent)) / 100f;
        setContentDescription("Progres ekspor " + Math.round(fraction * 100) + " persen");
        invalidate();
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float height = getHeight(), radius = height / 2f;
        paint.setColor(0xffe3dfd7);
        canvas.drawRoundRect(0, 0, getWidth(), height, radius, radius, paint);
        if (fraction > 0) {
            paint.setColor(0xffd45c4f);
            canvas.drawRoundRect(0, 0, getWidth() * fraction, height, radius, radius, paint);
        }
    }
}
