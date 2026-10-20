package com.auroratv.player;

import android.app.Activity;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** API14+ native VideoView/MediaPlayer player. Codec/stream support is platform-dependent. */
public final class PlayerActivity extends Activity {
    private VideoView video;
    private TextView title, feedback, playPause, favorite;
    private LinearLayout panel;
    private ProgressBar spinner;
    private List<Channel> channels;
    private int index;
    private String fallbackUrl, fallbackName;
    private boolean ready;
    private boolean pausedByUser;
    private boolean resumeOnForeground;
    private final Handler handler = new Handler();
    private final Runnable playbackTimeout = new Runnable() {
        @Override public void run() {
            if (!ready && !isFinishing()) showError("Stream timed out. It may be offline or incompatible with this device.");
        }
    };

    @Override public void onCreate(Bundle saved) {
        super.onCreate(saved);
        getWindow().setFlags(android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN,
                android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN);
        channels = ChannelStore.all();
        index = getIntent().getIntExtra("index", -1);
        fallbackUrl = getIntent().getStringExtra("url");
        fallbackName = getIntent().getStringExtra("name");
        makeUi();
        if (currentUrl() == null) showError("No stream URL was supplied.");
        else openChannel();
    }

    private void makeUi() {
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);
        setContentView(root);
        video = new VideoView(this);
        root.addView(video, new FrameLayout.LayoutParams(-1, -1));
        video.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) { panel.setVisibility(panel.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE); }
        });
        video.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override public void onPrepared(MediaPlayer player) {
                ready = true;
                handler.removeCallbacks(playbackTimeout);
                spinner.setVisibility(View.GONE);
                feedback.setText("");
                player.setOnInfoListener(new MediaPlayer.OnInfoListener() {
                    @Override public boolean onInfo(MediaPlayer mp, int what, int extra) {
                        if (what == MediaPlayer.MEDIA_INFO_BUFFERING_START) {
                            spinner.setVisibility(View.VISIBLE);
                            feedback.setText("Buffering…");
                        } else if (what == MediaPlayer.MEDIA_INFO_BUFFERING_END) {
                            spinner.setVisibility(View.GONE);
                            feedback.setText("");
                        }
                        return false;
                    }
                });
                video.start();
                playPause.setText("Pause");
            }
        });
        video.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override public boolean onError(MediaPlayer player, int what, int extra) {
                showError("Cannot play this channel (error " + what + "/" + extra
                        + "). Stream may be offline, unsupported, or geo-restricted.");
                return true;
            }
        });
        video.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override public void onCompletion(MediaPlayer player) {
                feedback.setText("Stream ended. Try another channel.");
                playPause.setText("Play");
                panel.setVisibility(View.VISIBLE);
            }
        });
        FrameLayout indicatorArea = new FrameLayout(this);
        FrameLayout.LayoutParams center = new FrameLayout.LayoutParams(-1, -1);
        root.addView(indicatorArea, center);
        indicatorArea.setClickable(false);
        spinner = new ProgressBar(this);
        spinner.setIndeterminate(true);
        FrameLayout.LayoutParams spinParams = new FrameLayout.LayoutParams(Ui.dp(this, 52), Ui.dp(this, 52), Gravity.CENTER);
        indicatorArea.addView(spinner, spinParams);

        panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(Ui.dp(this, 17), Ui.dp(this, 10), Ui.dp(this, 17), Ui.dp(this, 13));
        GradientDrawable panelBg = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{Color.rgb(39, 47, 67), Color.rgb(10, 14, 26)});
        Ui.background(panel, panelBg);
        FrameLayout.LayoutParams controls = new FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM);
        root.addView(panel, controls);

        title = Ui.text(this, "", 18, Ui.WHITE, true);
        title.setSingleLine(true);
        title.setEllipsize(android.text.TextUtils.TruncateAt.END);
        panel.addView(title, new LinearLayout.LayoutParams(-1, Ui.dp(this, 28)));
        feedback = Ui.text(this, "Loading stream…", 12, Ui.MUTED, false);
        feedback.setMaxLines(2);
        panel.addView(feedback, new LinearLayout.LayoutParams(-1, -2));
        LinearLayout actions = new LinearLayout(this);
        actions.setGravity(Gravity.CENTER_VERTICAL);
        panel.addView(actions, new LinearLayout.LayoutParams(-1, Ui.dp(this, 49)));
        addControl(actions, "◀ Previous", new View.OnClickListener() {
            @Override public void onClick(View v) { switchChannel(-1); }
        }, 1);
        playPause = addControl(actions, "Pause", new View.OnClickListener() {
            @Override public void onClick(View v) { togglePlayback(); }
        }, 1);
        addControl(actions, "Next ▶", new View.OnClickListener() {
            @Override public void onClick(View v) { switchChannel(1); }
        }, 1);
        favorite = addControl(actions, "☆", new View.OnClickListener() {
            @Override public void onClick(View v) { toggleFavorite(); }
        }, 0);
        addControl(actions, "Back", new View.OnClickListener() {
            @Override public void onClick(View v) { finish(); }
        }, 0);
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            title.setTextSize(16);
            playPause.setTextSize(12);
        }
    }

    private TextView addControl(LinearLayout area, String text, View.OnClickListener click, int weight) {
        TextView control = Ui.text(this, text, 12, Ui.WHITE, true);
        control.setGravity(Gravity.CENTER);
        Ui.focusable(control, Ui.CARD, 9);
        control.setOnClickListener(click);
        LinearLayout.LayoutParams size = weight == 0
                ? new LinearLayout.LayoutParams(Ui.dp(this, text.equals("Back") ? 55 : 39), Ui.dp(this, 41))
                : new LinearLayout.LayoutParams(0, Ui.dp(this, 41), weight);
        size.rightMargin = Ui.dp(this, 5);
        area.addView(control, size);
        return control;
    }

    private String currentUrl() {
        if (index >= 0 && index < channels.size()) return channels.get(index).url;
        return fallbackUrl;
    }

    private String currentName() {
        if (index >= 0 && index < channels.size()) return channels.get(index).name;
        return fallbackName == null ? "Live channel" : fallbackName;
    }

    private void openChannel() {
        String url = currentUrl();
        if (url == null) return;
        handler.removeCallbacks(playbackTimeout);
        ready = false;
        pausedByUser = false;
        playPause.setText("Pause");
        spinner.setVisibility(View.VISIBLE);
        feedback.setText("Connecting to live stream…");
        title.setText(currentName());
        panel.setVisibility(View.VISIBLE);
        updateFavorite();
        if (url.toLowerCase(java.util.Locale.ROOT).contains(".mpd")) {
            showError("MPEG-DASH playback is not supported by the Android 4 compatibility player.");
            return;
        }
        // Native MediaPlayer supports some HLS and RTSP formats; capabilities vary by device.
        try {
            video.stopPlayback();
            video.setVideoURI(Uri.parse(url));
            video.start();
            handler.postDelayed(playbackTimeout, 25000);
        } catch (Exception e) {
            showError("Playback failed: " + e.getMessage());
        }
    }

    private void switchChannel(int direction) {
        if (channels.isEmpty()) {
            Toast.makeText(this, "Channel list unavailable. Return to browser.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (index < 0 || index >= channels.size()) index = 0;
        else index = (index + direction + channels.size()) % channels.size();
        openChannel();
    }

    private void togglePlayback() {
        try {
            if (video.isPlaying()) {
                video.pause();
                pausedByUser = true;
                playPause.setText("Play");
            } else {
                video.start();
                pausedByUser = false;
                playPause.setText("Pause");
            }
        } catch (Exception e) { showError("Could not change playback state."); }
    }

    private void updateFavorite() {
        String url = currentUrl();
        Set<String> liked = getSharedPreferences("aurora", MODE_PRIVATE)
                .getStringSet("favorites", Collections.<String>emptySet());
        favorite.setText(liked.contains(url) ? "★" : "☆");
    }

    private void toggleFavorite() {
        String url = currentUrl();
        if (url == null) return;
        Set<String> liked = new LinkedHashSet<String>(getSharedPreferences("aurora", MODE_PRIVATE)
                .getStringSet("favorites", Collections.<String>emptySet()));
        if (!liked.add(url)) liked.remove(url);
        getSharedPreferences("aurora", MODE_PRIVATE).edit().putStringSet("favorites", liked).apply();
        updateFavorite();
    }

    private void showError(String reason) {
        ready = false;
        handler.removeCallbacks(playbackTimeout);
        spinner.setVisibility(View.GONE);
        feedback.setText(reason);
        playPause.setText("Play");
        panel.setVisibility(View.VISIBLE);
    }

    @Override public boolean onKeyDown(int key, KeyEvent event) {
        if (key == KeyEvent.KEYCODE_CHANNEL_UP || key == KeyEvent.KEYCODE_DPAD_UP) {
            switchChannel(-1);
            return true;
        }
        if (key == KeyEvent.KEYCODE_CHANNEL_DOWN || key == KeyEvent.KEYCODE_DPAD_DOWN) {
            switchChannel(1);
            return true;
        }
        if (key == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE) {
            togglePlayback();
            return true;
        }
        return super.onKeyDown(key, event);
    }

    @Override protected void onPause() {
        super.onPause();
        resumeOnForeground = ready && !pausedByUser && video.isPlaying();
        if (resumeOnForeground) video.pause();
    }

    @Override protected void onResume() {
        super.onResume();
        if (resumeOnForeground && ready && !pausedByUser) video.start();
        resumeOnForeground = false;
    }

    @Override protected void onDestroy() {
        handler.removeCallbacks(playbackTimeout);
        video.stopPlayback();
        super.onDestroy();
    }
}
