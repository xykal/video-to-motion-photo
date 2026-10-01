package id.xyverse.motionphoto;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.MotionEvent;
import android.view.View;

/** A touch-accessible timeline with two large draggable endpoints. */
public final class TrimRangeView extends View {
    public interface Listener { void onRangeChanged(long startMs, long endMs); }
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private long durationMs = 30000, startMs, endMs = 30000;
    private int activeHandle = -1;
    private Listener listener;

    public TrimRangeView(Context context) { super(context); setMinimumHeight(dp(68)); }
    public void setListener(Listener listener) { this.listener = listener; }
    public void setDuration(long durationMs) {
        this.durationMs = Math.max(1000, durationMs);
        startMs = 0;
        endMs = Math.min(this.durationMs, 30000);
        setContentDescription("Pangkas video dari " + format(startMs) + " sampai " + format(endMs));
        notifyRange();
        invalidate();
    }
    public long getStartMs() { return startMs; }
    public long getEndMs() { return endMs; }
    private int dp(int dp) { return Math.round(dp * getResources().getDisplayMetrics().density); }
    private float left() { return dp(28); }
    private float right() { return Math.max(left() + 1, getWidth() - dp(28)); }
    private float toX(long ms) { return left() + (right() - left()) * ms / durationMs; }
    private long toTime(float x) { return Math.round(Math.max(0, Math.min(1, (x - left()) / (right() - left()))) * durationMs); }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float mid = getHeight() / 2f;
        paint.setColor(0xffe2dfd6);
        canvas.drawRoundRect(left(), mid - dp(12), right(), mid + dp(12), dp(10), dp(10), paint);
        paint.setColor(0xffd45c4f);
        canvas.drawRoundRect(toX(startMs), mid - dp(12), toX(endMs), mid + dp(12), dp(9), dp(9), paint);
        paint.setColor(0xff202521);
        canvas.drawCircle(toX(startMs), mid, dp(16), paint);
        canvas.drawCircle(toX(endMs), mid, dp(16), paint);
        paint.setColor(0xffffffff);
        canvas.drawRoundRect(toX(startMs) - dp(2), mid - dp(6), toX(startMs) + dp(2), mid + dp(6), dp(2), dp(2), paint);
        canvas.drawRoundRect(toX(endMs) - dp(2), mid - dp(6), toX(endMs) + dp(2), mid + dp(6), dp(2), dp(2), paint);
    }

    @Override public boolean onTouchEvent(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                activeHandle = Math.abs(event.getX() - toX(startMs)) <= Math.abs(event.getX() - toX(endMs)) ? 0 : 1;
                getParent().requestDisallowInterceptTouchEvent(true);
                performClick();
            case MotionEvent.ACTION_MOVE:
                if (activeHandle < 0) return false;
                long chosen = toTime(event.getX());
                if (activeHandle == 0) startMs = Math.max(0, Math.min(chosen, endMs - 1000));
                else endMs = Math.min(durationMs, Math.max(chosen, startMs + 1000));
                if (endMs - startMs > 30000) {
                    if (activeHandle == 0) startMs = endMs - 30000;
                    else endMs = startMs + 30000;
                }
                notifyRange();
                invalidate();
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                activeHandle = -1;
                return true;
            default: return true;
        }
    }
    @Override public boolean performClick() { super.performClick(); return true; }
    private void notifyRange() {
        setContentDescription("Pangkas video dari " + format(startMs) + " sampai " + format(endMs));
        if (listener != null) listener.onRangeChanged(startMs, endMs);
    }
    public static String format(long ms) {
        long seconds = ms / 1000;
        return String.format(java.util.Locale.getDefault(), "%02d:%02d", seconds / 60, seconds % 60);
    }
}
