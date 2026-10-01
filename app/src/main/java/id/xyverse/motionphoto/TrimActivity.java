package id.xyverse.motionphoto;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public final class TrimActivity extends Activity {
    private static final int INK = 0xff202521, CREAM = 0xfff7f4ed;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final ThreadPoolExecutor previewWorker = new ThreadPoolExecutor(1, 1, 0,
            TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>());
    private long lastStillAt;
    private ExoPlayer player;
    private ImageView thumbnail;
    private TrimRangeView range;
    private TextView times, play, finishButton, previewStatus;
    private Uri source;
    private boolean ready, canPlay = true;
    private int thumbnailGeneration;
    private final Runnable stopAtEnd = new Runnable() {
        @Override public void run() {
            if (player == null || !player.isPlaying()) return;
            if (player.getCurrentPosition() >= range.getEndMs()) {
                player.pause();
                play.setText("Putar potongan");
                setIcon(play, R.drawable.ic_play, INK);
                showStill(range.getEndMs() - 100);
            } else handler.postDelayed(this, 80);
        }
    };

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        source = getIntent().getData();
        if (source == null) { finish(); return; }
        getWindow().setStatusBarColor(CREAM);
        getWindow().setNavigationBarColor(CREAM);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        LinearLayout screen = new LinearLayout(this);
        screen.setOrientation(LinearLayout.VERTICAL);
        screen.setBackgroundColor(CREAM);
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        screen.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(24), dp(28), dp(24), dp(34));
        scroll.addView(root);
        root.addView(text("Pangkas videomu", 28, INK, true), margin(8));
        root.addView(text("Geser ujung garis. Potongan 1–30 detik bisa diambil dari bagian mana pun.", 14, 0xff656d67, false), margin(8));
        FrameLayout previewFrame = new FrameLayout(this);
        GradientDrawable surface = new GradientDrawable();
        surface.setColor(INK); surface.setCornerRadius(dp(18));
        previewFrame.setBackground(surface);
        previewFrame.setClipToOutline(true);
        PlayerView playerView = (PlayerView) getLayoutInflater().inflate(R.layout.trim_player, previewFrame, false);
        previewFrame.addView(playerView, new FrameLayout.LayoutParams(-1, -1));
        thumbnail = new ImageView(this);
        thumbnail.setScaleType(ImageView.ScaleType.FIT_CENTER);
        thumbnail.setBackgroundColor(INK);
        previewFrame.addView(thumbnail, new FrameLayout.LayoutParams(-1, -1));
        previewStatus = text("Memuat pratinjau video…", 14, Color.WHITE, false);
        previewStatus.setGravity(Gravity.CENTER);
        previewFrame.addView(previewStatus, new FrameLayout.LayoutParams(-1, -1));
        LinearLayout.LayoutParams frame = new LinearLayout.LayoutParams(-1, dp(244));
        frame.topMargin = dp(22);
        root.addView(previewFrame, frame);
        play = button("Putar potongan", false, R.drawable.ic_play);
        play.setOnClickListener(v -> {
            if (!ready || !canPlay || player == null) return;
            if (player.isPlaying()) {
                player.pause();
                play.setText("Putar potongan");
                setIcon(play, R.drawable.ic_play, INK);
                showStill(player.getCurrentPosition());
            } else {
                thumbnailGeneration++;
                player.seekTo(range.getStartMs());
                player.play();
                play.setText("Jeda potongan");
                setIcon(play, R.drawable.ic_pause, INK);
                handler.removeCallbacks(stopAtEnd);
                handler.postDelayed(stopAtEnd, 80);
            }
        });
        root.addView(play, margin(14));
        times = text("Membaca durasi…", 16, INK, true);
        root.addView(times, margin(20));
        range = new TrimRangeView(this);
        range.setListener((start, end, previewMs, finished) -> {
            times.setText("MULAI " + TrimRangeView.format(start) + "   ·   AKHIR " + TrimRangeView.format(end)
                    + "   ·   DURASI " + TrimRangeView.format(end - start));
            if (!ready) return;
            if (player != null) {
                player.pause();
                if (finished) player.seekTo(start);
            }
            play.setText("Putar potongan");
            setIcon(play, R.drawable.ic_play, INK);
            previewStatus.setText("Memuat frame " + TrimRangeView.format(previewMs) + "…");
            previewStatus.setVisibility(View.VISIBLE);
            if (finished || SystemClock.uptimeMillis() - lastStillAt > 140) {
                lastStillAt = SystemClock.uptimeMillis();
                showStill(previewMs);
            }
        });
        root.addView(range, margin(6));
        root.addView(text("Pratinjau dapat bergeser ke frame kunci; hasil ekspor memakai rentang yang dipilih.", 12, 0xff656d67, false), margin(6));
        finishButton = button("Selesai · pakai ini", true, R.drawable.ic_done);
        finishButton.setEnabled(false);
        finishButton.setAlpha(.45f);
        finishButton.setOnClickListener(v -> {
            if (!ready) return;
            Intent result = new Intent();
            result.putExtra("startMs", range.getStartMs());
            result.putExtra("endMs", range.getEndMs());
            setResult(RESULT_OK, result);
            finish();
        });
        root.addView(text("Powered by " + Brand.NAME, 11, 0xff656d67, false), margin(18));
        LinearLayout footer = new LinearLayout(this);
        footer.setPadding(dp(24), dp(10), dp(24), dp(16));
        footer.setBackgroundColor(CREAM);
        TextView cancel = button("Batal", false, R.drawable.ic_close);
        cancel.setOnClickListener(v -> { setResult(RESULT_CANCELED); finish(); });
        LinearLayout.LayoutParams cancelWidth = new LinearLayout.LayoutParams(0, dp(58), 1);
        cancelWidth.rightMargin = dp(8);
        footer.addView(cancel, cancelWidth);
        footer.addView(finishButton, new LinearLayout.LayoutParams(0, dp(58), 2));
        screen.addView(footer, new LinearLayout.LayoutParams(-1, -2));
        setContentView(screen);
        player = new ExoPlayer.Builder(this).build();
        playerView.setPlayer(player);
        player.addListener(new Player.Listener() {
            @Override public void onPlaybackStateChanged(int state) {
                if (state == Player.STATE_READY && !ready && player.getDuration() > 0) enableRange(player.getDuration());
            }
            @Override public void onRenderedFirstFrame() {
                if (player.isPlaying()) { thumbnail.setVisibility(View.GONE); previewStatus.setVisibility(View.GONE); }
            }
            @Override public void onIsPlayingChanged(boolean playing) {
                if (playing) { thumbnail.setVisibility(View.GONE); previewStatus.setVisibility(View.GONE); }
            }
            @Override public void onPlayerError(PlaybackException error) {
                canPlay = false;
                player.pause();
                play.setEnabled(false);
                play.setAlpha(.45f);
                previewStatus.setText("Pemutar tidak mendukung klip ini; periksa frame pratinjau.");
                previewStatus.setVisibility(View.VISIBLE);
            }
        });
        player.setMediaItem(MediaItem.fromUri(source));
        player.prepare();
        scheduleStill(0);
        previewWorker.execute(() -> {
            try (MediaMetadataRetriever retriever = new MediaMetadataRetriever()) {
                retriever.setDataSource(this, source);
                String duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
                if (duration != null) {
                    long ms = Long.parseLong(duration);
                    runOnUiThread(() -> { if (!ready && ms > 0) enableRange(ms); });
                }
            } catch (Exception ignored) {
                runOnUiThread(() -> { if (!ready) times.setText("Durasi tidak terbaca. Coba MP4 lain."); });
            }
        });
    }

    private void enableRange(long duration) {
        ready = true;
        range.setDuration(duration);
        finishButton.setEnabled(true);
        finishButton.setAlpha(1f);
        if (player != null) player.seekTo(0);
    }
    private void scheduleStill(long positionMs) {
        handler.removeCallbacksAndMessages(null);
        handler.postDelayed(() -> showStill(positionMs), 180);
    }
    private void showStill(long positionMs) {
        int generation = ++thumbnailGeneration;
        if (ready) previewWorker.getQueue().clear();
        previewWorker.execute(() -> {
            try (MediaMetadataRetriever retriever = new MediaMetadataRetriever()) {
                retriever.setDataSource(this, source);
                Bitmap frame = retriever.getScaledFrameAtTime(Math.max(0, positionMs) * 1000,
                        MediaMetadataRetriever.OPTION_CLOSEST, 480, 480);
                if (frame == null) throw new IllegalStateException("Frame tidak tersedia");
                runOnUiThread(() -> {
                    if (generation != thumbnailGeneration || isDestroyed() || (player != null && player.isPlaying())) {
                        frame.recycle(); return;
                    }
                    thumbnail.setImageBitmap(frame);
                    thumbnail.setVisibility(View.VISIBLE);
                    if (canPlay) previewStatus.setVisibility(View.GONE);
                });
            } catch (Exception ignored) {
                runOnUiThread(() -> {
                    if (generation == thumbnailGeneration && !isDestroyed()) {
                        previewStatus.setText("Frame pratinjau tidak tersedia di perangkat ini.");
                        previewStatus.setVisibility(View.VISIBLE);
                    }
                });
            }
        });
    }
    private TextView button(String label, boolean primary, int iconId) {
        TextView view = text(label, 15, primary ? Color.WHITE : INK, true);
        view.setGravity(Gravity.CENTER);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(primary ? 0xffd45c4f : Color.WHITE);
        bg.setCornerRadius(dp(14));
        view.setBackground(bg);
        view.setMinHeight(dp(50));
        view.setFocusable(true);
        view.setClickable(true);
        setIcon(view, iconId, primary ? Color.WHITE : INK);
        return view;
    }
    private void setIcon(TextView view, int iconId, int tint) {
        Drawable icon = getDrawable(iconId).mutate();
        icon.setTint(tint);
        view.setCompoundDrawablesWithIntrinsicBounds(icon, null, null, null);
        view.setCompoundDrawablePadding(dp(8));
    }
    private TextView text(String label, int size, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(label); view.setTextSize(size); view.setTextColor(color);
        if (bold) view.setTypeface(null, 1);
        return view;
    }
    private LinearLayout.LayoutParams margin(int top) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.topMargin = dp(top);
        return params;
    }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
    @Override protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        thumbnailGeneration++;
        previewWorker.shutdownNow();
        if (player != null) player.release();
        super.onDestroy();
    }
}
