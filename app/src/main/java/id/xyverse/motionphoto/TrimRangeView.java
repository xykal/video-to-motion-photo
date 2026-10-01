package id.xyverse.motionphoto;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.MotionEvent;
import android.view.View;

/** A two-handle trim range with immediate drag feedback. */
public final class TrimRangeView extends View {
    public interface Listener { void onRangeChanged(long startMs, long endMs, long previewMs, boolean finished); }
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private long durationMs = 30000, startMs, endMs = 30000;
    private int activeHandle = -1;
    private Listener listener;

    public TrimRangeView(Context context) { super(context); setMinimumHeight(dp(76)); }
    public void setListener(Listener listener) { this.listener = listener; }
    public void setDuration(long durationMs) {
        this.durationMs = Math.max(1000, durationMs);
        startMs = 0;
        endMs = Math.min(this.durationMs, 30000);
        notifyRange(startMs, true);
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
        canvas.drawRoundRect(left(), mid - dp(14), right(), mid + dp(14), dp(10), dp(10), paint);
        paint.setColor(0xffd45c4f);
        canvas.drawRoundRect(toX(startMs), mid - dp(14), toX(endMs), mid + dp(14), dp(9), dp(9), paint);
        paint.setColor(0xff202521);
        canvas.drawRoundRect(toX(startMs) - dp(10), mid - dp(21), toX(startMs) + dp(10), mid + dp(21), dp(7), dp(7), paint);
        canvas.drawRoundRect(toX(endMs) - dp(10), mid - dp(21), toX(endMs) + dp(10), mid + dp(21), dp(7), dp(7), paint);
        paint.setColor(0xffffffff);
        canvas.drawRoundRect(toX(startMs) - dp(2), mid - dp(10), toX(startMs) + dp(2), mid + dp(10), dp(2), dp(2), paint);
        canvas.drawRoundRect(toX(endMs) - dp(2), mid - dp(10), toX(endMs) + dp(2), mid + dp(10), dp(2), dp(2), paint);
    }

    @Override public boolean onTouchEvent(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                activeHandle = Math.abs(event.getX() - toX(startMs)) <= Math.abs(event.getX() - toX(endMs)) ? 0 : 1;
                getParent().requestDisallowInterceptTouchEvent(true);
                performClick();
                moveHandle(event.getX());
                return true;
            case MotionEvent.ACTION_MOVE:
                if (activeHandle < 0) return false;
                moveHandle(event.getX());
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (activeHandle >= 0) notifyRange(activeHandle == 0 ? startMs : endMs, true);
                activeHandle = -1;
                getParent().requestDisallowInterceptTouchEvent(false);
                return true;
            default: return false;
        }
    }
    private void moveHandle(float position) {
        long chosen = toTime(position);
        if (activeHandle == 0) startMs = Math.max(0, Math.min(chosen, endMs - 1000));
        else endMs = Math.min(durationMs, Math.max(chosen, startMs + 1000));
        if (endMs - startMs > 30000) {
            if (activeHandle == 0) startMs = endMs - 30000;
            else endMs = startMs + 30000;
        }
        notifyRange(activeHandle == 0 ? startMs : endMs, false);
        invalidate();
    }
    @Override public boolean performClick() { super.performClick(); return true; }
    private void notifyRange(long previewMs, boolean finished) {
        setContentDescription("Pangkas dari " + format(startMs) + " sampai " + format(endMs)
                + ", durasi " + format(endMs - startMs));
        if (listener != null) listener.onRangeChanged(startMs, endMs, previewMs, finished);
    }
    public static String format(long ms) {
        long seconds = ms / 1000;
        return String.format(java.util.Locale.getDefault(), "%02d:%02d", seconds / 60, seconds % 60);
    }
}
