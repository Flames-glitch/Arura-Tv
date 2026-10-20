package com.auroratv.player;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.UiModeManager;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Unified touch + DPAD browser. Includes the community IPTV-org M3U by default. */
public final class MainActivity extends Activity {
    private static final int PICK_FILE = 51;
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private List<Channel> all = Collections.emptyList();
    private final List<Channel> filtered = new ArrayList<Channel>();
    private Set<String> favorites = new LinkedHashSet<String>();
    private String category = "All";
    private String searchTerm = "";
    private boolean tv;
    private volatile boolean destroyed;
    private TextView status, sectionTitle, emptyView;
    private LinearLayout categoryStrip;
    private GridView grid;
    private ChannelAdapter adapter;

    @Override public void onCreate(Bundle savedInstance) {
        super.onCreate(savedInstance);
        UiModeManager mode = (UiModeManager) getSystemService(UI_MODE_SERVICE);
        tv = mode != null && mode.getCurrentModeType() == Configuration.UI_MODE_TYPE_TELEVISION;
        favorites = new LinkedHashSet<String>(getSharedPreferences("aurora", MODE_PRIVATE)
                .getStringSet("favorites", Collections.<String>emptySet()));
        makeScreen();
        renderCategories();
        renderChannels();
        status.setText("Loading saved channels…");
        io.execute(new Runnable() {
            @Override public void run() {
                try {
                    final List<Channel> stored = PlaylistImporter.restore(MainActivity.this);
                    if (!stored.isEmpty()) {
                        deliver(stored, "Saved playlist ready • Refresh for newer channels");
                        return;
                    }
                } catch (Exception ignored) { /* retry from built-in public source */ }
                download(PlaylistImporter.DEFAULT_URL);
            }
        });
    }

    private void makeScreen() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable bg = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{Color.rgb(17, 29, 57), Color.rgb(7, 11, 23)});
        Ui.background(root, bg);
        setContentView(root);

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        header.setPadding(Ui.dp(this, tv ? 26 : 14), Ui.dp(this, tv ? 16 : 9),
                Ui.dp(this, tv ? 26 : 14), Ui.dp(this, 7));
        root.addView(header, new LinearLayout.LayoutParams(-1, -2));

        LinearLayout headRow = new LinearLayout(this);
        headRow.setGravity(Gravity.CENTER_VERTICAL);
        header.addView(headRow, new LinearLayout.LayoutParams(-1, Ui.dp(this, tv ? 49 : 43)));
        TextView brand = Ui.text(this, "✦  AURORA TV", tv ? 25 : 21, Ui.WHITE, true);
        headRow.addView(brand, new LinearLayout.LayoutParams(0, -1, 1));
        if (tv) {
            addAction(headRow, "+ Playlist URL", new View.OnClickListener() {
                @Override public void onClick(View v) { showUrlDialog(); }
            });
            addAction(headRow, "Import M3U", new View.OnClickListener() {
                @Override public void onClick(View v) { pickFile(); }
            });
            addAction(headRow, "Refresh", new View.OnClickListener() {
                @Override public void onClick(View v) { refresh(); }
            });
            addAction(headRow, "Public TV", new View.OnClickListener() {
                @Override public void onClick(View v) { startLoading(PlaylistImporter.DEFAULT_URL); }
            });
        } else {
            HorizontalScrollView actionsScroll = new HorizontalScrollView(this);
            actionsScroll.setHorizontalScrollBarEnabled(false);
            header.addView(actionsScroll, new LinearLayout.LayoutParams(-1, Ui.dp(this, 51)));
            LinearLayout actions = new LinearLayout(this);
            actions.setGravity(Gravity.CENTER_VERTICAL);
            actionsScroll.addView(actions);
            addAction(actions, "+ Playlist URL", new View.OnClickListener() {
                @Override public void onClick(View v) { showUrlDialog(); }
            });
            addAction(actions, "Import M3U", new View.OnClickListener() {
                @Override public void onClick(View v) { pickFile(); }
            });
            addAction(actions, "Refresh", new View.OnClickListener() {
                @Override public void onClick(View v) { refresh(); }
            });
            addAction(actions, "Public TV", new View.OnClickListener() {
                @Override public void onClick(View v) { startLoading(PlaylistImporter.DEFAULT_URL); }
            });
        }

        LinearLayout hero = new LinearLayout(this);
        hero.setOrientation(LinearLayout.VERTICAL);
        hero.setGravity(Gravity.CENTER_VERTICAL);
        hero.setPadding(Ui.dp(this, 17), Ui.dp(this, 9), Ui.dp(this, 12), Ui.dp(this, 9));
        Ui.background(hero, Ui.rounded(this, Color.rgb(35, 51, 87), 14));
        LinearLayout.LayoutParams hp = new LinearLayout.LayoutParams(-1, Ui.dp(this, tv ? 77 : 70));
        hp.setMargins(Ui.dp(this, tv ? 26 : 14), Ui.dp(this, 5), Ui.dp(this, tv ? 26 : 14), Ui.dp(this, 10));
        root.addView(hero, hp);
        hero.addView(Ui.text(this, "LIVE CHANNELS  •  IPTV-org public playlist", tv ? 17 : 14,
                Ui.WHITE, true), new LinearLayout.LayoutParams(-1, Ui.dp(this, 27)));
        status = Ui.text(this, "Fetching public channels…", 12, Ui.MUTED, false);
        status.setSingleLine(true);
        status.setEllipsize(android.text.TextUtils.TruncateAt.END);
        hero.addView(status, new LinearLayout.LayoutParams(-1, Ui.dp(this, 23)));

        LinearLayout browse = new LinearLayout(this);
        browse.setGravity(Gravity.CENTER_VERTICAL);
        browse.setPadding(Ui.dp(this, tv ? 26 : 14), 0, Ui.dp(this, tv ? 26 : 14), 0);
        root.addView(browse, new LinearLayout.LayoutParams(-1, Ui.dp(this, tv ? 60 : 56)));
        sectionTitle = Ui.text(this, "All channels", tv ? 24 : 18, Ui.WHITE, true);
        browse.addView(sectionTitle, new LinearLayout.LayoutParams(0, -1, 1));
        EditText search = new EditText(this);
        search.setSingleLine(true);
        search.setTextSize(tv ? 14 : 13);
        search.setTextColor(Ui.WHITE);
        search.setHintTextColor(Ui.MUTED);
        search.setHint("Search channels");
        search.setPadding(Ui.dp(this, 12), 0, Ui.dp(this, 10), 0);
        search.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
        Ui.background(search, Ui.rounded(this, Ui.PANE, 10));
        browse.addView(search, new LinearLayout.LayoutParams(Ui.dp(this, tv ? 258 : 155), Ui.dp(this, 43)));
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchTerm = s.toString().toLowerCase(Locale.ROOT).trim();
                renderChannels();
            }
            @Override public void afterTextChanged(Editable s) { }
        });

        HorizontalScrollView categories = new HorizontalScrollView(this);
        categories.setHorizontalScrollBarEnabled(false);
        categories.setFillViewport(false);
        categories.setPadding(Ui.dp(this, tv ? 26 : 14), 0, Ui.dp(this, 5), 0);
        root.addView(categories, new LinearLayout.LayoutParams(-1, Ui.dp(this, 55)));
        categoryStrip = new LinearLayout(this);
        categoryStrip.setGravity(Gravity.CENTER_VERTICAL);
        categories.addView(categoryStrip);

        FrameLayout content = new FrameLayout(this);
        root.addView(content, new LinearLayout.LayoutParams(-1, 0, 1));
        grid = new GridView(this);
        grid.setNumColumns(columnCount());
        grid.setVerticalSpacing(Ui.dp(this, 8));
        grid.setHorizontalSpacing(Ui.dp(this, 8));
        grid.setPadding(Ui.dp(this, tv ? 26 : 14), Ui.dp(this, 5),
                Ui.dp(this, tv ? 26 : 14), Ui.dp(this, 18));
        grid.setClipToPadding(false);
        grid.setVerticalScrollBarEnabled(false);
        grid.setSelector(android.R.color.transparent);
        adapter = new ChannelAdapter();
        grid.setAdapter(adapter);
        content.addView(grid, new FrameLayout.LayoutParams(-1, -1));
        emptyView = Ui.text(this, "No channels yet. Check your connection or import an M3U playlist.",
                15, Ui.MUTED, false);
        emptyView.setGravity(Gravity.CENTER);
        emptyView.setPadding(Ui.dp(this, 25), Ui.dp(this, 10), Ui.dp(this, 25), Ui.dp(this, 10));
        content.addView(emptyView, new FrameLayout.LayoutParams(-1, -1));
        grid.setEmptyView(emptyView);
    }

    private int columnCount() {
        int width = getResources().getConfiguration().screenWidthDp;
        if (width <= 0) width = (int)(getResources().getDisplayMetrics().widthPixels /
                getResources().getDisplayMetrics().density);
        if (tv) return width >= 1050 ? 5 : width >= 750 ? 4 : width >= 500 ? 3 : 2;
        return width >= 850 ? 4 : width >= 600 ? 3 : width >= 440 ? 2 : 1;
    }

    private void addAction(LinearLayout parent, String title, View.OnClickListener click) {
        TextView action = Ui.text(this, title, tv ? 12 : 13, Ui.WHITE, true);
        action.setGravity(Gravity.CENTER);
        action.setPadding(Ui.dp(this, 15), 0, Ui.dp(this, 15), 0);
        Ui.focusable(action, Ui.PANE, 9);
        action.setOnClickListener(click);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-2, Ui.dp(this, 40));
        params.leftMargin = Ui.dp(this, 6);
        parent.addView(action, params);
    }

    private void renderCategories() {
        categoryStrip.removeAllViews();
        LinkedHashSet<String> categories = new LinkedHashSet<String>();
        categories.add("All");
        categories.add("Favorites");
        for (Channel ch : all) categories.add(ch.group);
        for (final String key : categories) {
            TextView chip = Ui.text(this, key, tv ? 13 : 12, Ui.WHITE, key.equals(category));
            chip.setGravity(Gravity.CENTER);
            chip.setPadding(Ui.dp(this, 16), 0, Ui.dp(this, 16), 0);
            Ui.focusable(chip, key.equals(category) ? Color.rgb(60, 79, 120) : Ui.PANE, 12);
            chip.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    category = key;
                    renderCategories();
                    renderChannels();
                }
            });
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-2, Ui.dp(this, 37));
            params.rightMargin = Ui.dp(this, 8);
            categoryStrip.addView(chip, params);
        }
    }

    private void renderChannels() {
        if (adapter == null) return;
        filtered.clear();
        for (Channel ch : all) {
            if ("Favorites".equals(category) && !favorites.contains(ch.url)) continue;
            if (!"Favorites".equals(category) && !"All".equals(category) && !category.equals(ch.group)) continue;
            if (searchTerm.length() > 0 && !ch.name.toLowerCase(Locale.ROOT).contains(searchTerm)
                    && !ch.group.toLowerCase(Locale.ROOT).contains(searchTerm)) continue;
            filtered.add(ch);
        }
        sectionTitle.setText("All".equals(category) ? "All channels" : category);
        adapter.notifyDataSetChanged();
    }

    private void deliver(final List<Channel> channels, final String message) {
        runOnUiThread(new Runnable() {
            @Override public void run() {
                if (destroyed) return;
                all = channels;
                ChannelStore.replace(channels);
                category = "All";
                status.setText(channels.size() + " channels • " + message);
                renderCategories();
                renderChannels();
                if (tv && !channels.isEmpty()) grid.requestFocus();
            }
        });
    }

    private void startLoading(final String url) {
        status.setText("Downloading M3U playlist…");
        io.execute(new Runnable() { @Override public void run() { download(url); } });
    }

    private void download(String url) {
        try {
            deliver(PlaylistImporter.fromUrl(this, url), "Loaded from " + sourceTitle(url));
        } catch (final Exception exception) {
            error("Playlist download failed: " + exception.getMessage());
        }
    }

    private String sourceTitle(String source) {
        return source.equals(PlaylistImporter.DEFAULT_URL) ? "IPTV-org" : "custom URL";
    }

    private void refresh() { startLoading(PlaylistImporter.rememberedUrl(this)); }

    private void error(final String text) {
        runOnUiThread(new Runnable() {
            @Override public void run() {
                if (destroyed) return;
                status.setText(text);
                Toast.makeText(MainActivity.this, text, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void showUrlDialog() {
        final EditText field = new EditText(this);
        field.setSingleLine(true);
        field.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_URI);
        field.setText(PlaylistImporter.rememberedUrl(this));
        field.setSelectAllOnFocus(true);
        new AlertDialog.Builder(this).setTitle("Open M3U playlist URL")
                .setView(field).setNegativeButton("Cancel", null)
                .setPositiveButton("Import", new android.content.DialogInterface.OnClickListener() {
                    @Override public void onClick(android.content.DialogInterface dialog, int which) {
                        String url = field.getText().toString().trim();
                        if (url.length() == 0) error("Enter a playlist URL.");
                        else startLoading(url);
                    }
                }).show();
    }

    private void pickFile() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        try { startActivityForResult(Intent.createChooser(intent, "Choose M3U playlist"), PICK_FILE); }
        catch (ActivityNotFoundException e) { error("No file picker is installed on this device."); }
    }

    @Override protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (request == PICK_FILE && result == RESULT_OK && data != null && data.getData() != null) {
            final Uri file = data.getData();
            status.setText("Reading selected playlist…");
            io.execute(new Runnable() {
                @Override public void run() {
                    try { deliver(PlaylistImporter.fromFile(MainActivity.this, file), "Imported local M3U file"); }
                    catch (Exception e) { error("File import failed: " + e.getMessage()); }
                }
            });
        }
    }

    private void play(Channel channel) {
        int index = all.indexOf(channel);
        Intent intent = new Intent(this, PlayerActivity.class);
        intent.putExtra("index", index);
        intent.putExtra("url", channel.url);
        intent.putExtra("name", channel.name);
        startActivity(intent);
    }

    private void toggleFavorite(Channel channel) {
        if (!favorites.add(channel.url)) favorites.remove(channel.url);
        getSharedPreferences("aurora", MODE_PRIVATE).edit()
                .putStringSet("favorites", new LinkedHashSet<String>(favorites)).apply();
        renderChannels();
        Toast.makeText(this, favorites.contains(channel.url) ? "Added favorite" : "Removed favorite",
                Toast.LENGTH_SHORT).show();
    }

    @Override protected void onResume() {
        super.onResume();
        // Favorites may have changed in the full-screen player activity.
        favorites = new LinkedHashSet<String>(getSharedPreferences("aurora", MODE_PRIVATE)
                .getStringSet("favorites", Collections.<String>emptySet()));
        if (adapter != null) renderChannels();
    }

    @Override protected void onDestroy() {
        destroyed = true;
        io.shutdownNow();
        super.onDestroy();
    }

    private final class ChannelAdapter extends BaseAdapter {
        @Override public int getCount() { return filtered.size(); }
        @Override public Object getItem(int index) { return filtered.get(index); }
        @Override public long getItemId(int index) { return index; }

        @Override public View getView(int position, View convertView, ViewGroup parent) {
            final Card card;
            if (convertView == null) {
                card = new Card();
                LinearLayout box = new LinearLayout(MainActivity.this);
                box.setOrientation(LinearLayout.HORIZONTAL);
                box.setGravity(Gravity.CENTER_VERTICAL);
                box.setPadding(Ui.dp(MainActivity.this, 10), Ui.dp(MainActivity.this, 8),
                        Ui.dp(MainActivity.this, 10), Ui.dp(MainActivity.this, 8));
                Ui.focusable(box, Ui.CARD, 12);
                box.setLongClickable(true);
                box.setLayoutParams(new AbsListView.LayoutParams(-1, Ui.dp(MainActivity.this, tv ? 105 : 80)));
                card.initials = Ui.text(MainActivity.this, "TV", tv ? 14 : 12, Ui.WHITE, true);
                card.initials.setGravity(Gravity.CENTER);
                Ui.background(card.initials, Ui.rounded(MainActivity.this, Color.rgb(63, 80, 126), 9));
                box.addView(card.initials, new LinearLayout.LayoutParams(Ui.dp(MainActivity.this, 42), Ui.dp(MainActivity.this, 42)));
                LinearLayout words = new LinearLayout(MainActivity.this);
                words.setOrientation(LinearLayout.VERTICAL);
                words.setGravity(Gravity.CENTER_VERTICAL);
                words.setPadding(Ui.dp(MainActivity.this, 10), 0, 0, 0);
                box.addView(words, new LinearLayout.LayoutParams(0, -1, 1));
                card.name = Ui.text(MainActivity.this, "", tv ? 14 : 14, Ui.WHITE, true);
                card.name.setSingleLine(true);
                card.name.setEllipsize(android.text.TextUtils.TruncateAt.END);
                words.addView(card.name, new LinearLayout.LayoutParams(-1, Ui.dp(MainActivity.this, 29)));
                card.group = Ui.text(MainActivity.this, "", 11, Ui.MUTED, false);
                card.group.setSingleLine(true);
                card.group.setEllipsize(android.text.TextUtils.TruncateAt.END);
                words.addView(card.group, new LinearLayout.LayoutParams(-1, Ui.dp(MainActivity.this, 22)));
                card.star = Ui.text(MainActivity.this, "☆", 21, Ui.ACCENT, false);
                box.addView(card.star, new LinearLayout.LayoutParams(Ui.dp(MainActivity.this, 22), -1));
                card.root = box;
                box.setTag(card);
                convertView = box;
            } else card = (Card) convertView.getTag();
            final Channel ch = filtered.get(position);
            card.name.setText(ch.name);
            card.group.setText(ch.group);
            card.initials.setText(ch.name.length() >= 2 ? ch.name.substring(0, 2).toUpperCase(Locale.ROOT) : "TV");
            card.star.setText(favorites.contains(ch.url) ? "★" : "☆");
            card.root.setContentDescription(ch.name + ", " + ch.group + ". Press to watch; long press to favorite.");
            card.root.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) { play(ch); }
            });
            card.root.setOnLongClickListener(new View.OnLongClickListener() {
                @Override public boolean onLongClick(View v) { toggleFavorite(ch); return true; }
            });
            return convertView;
        }
    }

    private static final class Card {
        LinearLayout root;
        TextView initials, name, group, star;
    }
}
