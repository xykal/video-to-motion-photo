package id.xyverse.motionphoto;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.VideoView;

public final class TrimActivity extends Activity {
    private static final int INK = 0xff202521, CREAM = 0xfff7f4ed;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private VideoView video;
    private TrimRangeView range;
    private TextView times, play, continueButton;
    private boolean prepared;
    private final Runnable stopAtEnd = new Runnable() {
        @Override public void run() {
            if (!prepared || video == null || !video.isPlaying()) return;
            if (video.getCurrentPosition() >= range.getEndMs()) {
                video.pause(); play.setText("Putar potongan  ▶");
            } else handler.postDelayed(this, 80);
        }
    };

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        Uri source = getIntent().getData();
        if (source == null) { finish(); return; }
        getWindow().setStatusBarColor(CREAM);
        getWindow().setNavigationBarColor(CREAM);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(CREAM);
        root.setPadding(dp(24), dp(38), dp(24), dp(24));
        TextView back = button("←  Kembali", false);
        back.setOnClickListener(v -> finish());
        root.addView(back, new LinearLayout.LayoutParams(-1, dp(48)));
        TextView title = text("Pangkas momenmu", 28, INK, true);
        root.addView(title, margin(24));
        root.addView(text("Geser kedua ujung garis untuk memilih bagian video. Maksimal 30 detik untuk stabilisasi.", 14, 0xff656d67, false), margin(10));
        video = new VideoView(this);
        video.setBackgroundColor(0xff202521);
        LinearLayout.LayoutParams frame = new LinearLayout.LayoutParams(-1, 0, 1);
        frame.topMargin = dp(18);
        root.addView(video, frame);
        play = button("Putar potongan  ▶", false);
        play.setOnClickListener(v -> {
            if (!prepared) return;
            if (video.isPlaying()) {
                video.pause(); play.setText("Putar potongan  ▶");
            } else {
                video.seekTo((int) range.getStartMs());
                video.start();
                play.setText("Jeda  Ⅱ");
                handler.removeCallbacks(stopAtEnd);
                handler.postDelayed(stopAtEnd, 80);
            }
        });
        root.addView(play, margin(16));
        times = text("Memuat durasi…", 16, INK, true);
        root.addView(times, margin(22));
        range = new TrimRangeView(this);
        range.setListener((start, end) -> {
            times.setText(TrimRangeView.format(start) + "  —  " + TrimRangeView.format(end)
                    + "    ·    " + TrimRangeView.format(end - start));
            if (prepared) {
                video.pause();
                play.setText("Putar potongan  ▶");
                video.seekTo((int) start);
            }
        });
        root.addView(range, margin(8));
        continueButton = button("Lanjut atur sampul  →", true);
        continueButton.setAlpha(.45f);
        continueButton.setOnClickListener(v -> {
            if (!prepared) return;
            Intent result = new Intent();
            result.putExtra("startMs", range.getStartMs());
            result.putExtra("endMs", range.getEndMs());
            setResult(RESULT_OK, result);
            finish();
        });
        root.addView(continueButton, margin(16));
        setContentView(root);
        video.setOnPreparedListener(player -> {
            int duration = player.getDuration();
            if (duration <= 0) { times.setText("Durasi tidak terbaca. Pilih video lain."); return; }
            prepared = true;
            range.setDuration(duration);
            continueButton.setAlpha(1f);
            video.seekTo(0);
        });
        video.setOnErrorListener((player, what, extra) -> {
            prepared = false;
            times.setText("Video tidak dapat dipratinjau di perangkat ini.");
            return true;
        });
        video.setVideoURI(source);
    }

    private TextView button(String label, boolean primary) {
        TextView view = text(label, 15, primary ? Color.WHITE : INK, true);
        view.setGravity(Gravity.CENTER);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(primary ? 0xffd45c4f : 0xffffffff);
        bg.setCornerRadius(dp(14));
        view.setBackground(bg);
        view.setFocusable(true);
        view.setClickable(true);
        return view;
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
        if (video != null) video.stopPlayback();
        super.onDestroy();
    }
}
