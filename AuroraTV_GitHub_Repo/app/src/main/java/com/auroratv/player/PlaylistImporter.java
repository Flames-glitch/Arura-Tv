package com.auroratv.player;

import android.content.Context;
import android.net.Uri;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collections;
import java.util.List;

/** Playlist cache, default IPTV-org import and optional user-specified M3U sources. */
public final class PlaylistImporter {
    public static final String DEFAULT_URL = "https://iptv-org.github.io/iptv/index.m3u";
    private static final int MAX_BYTES = 32 * 1024 * 1024;
    private static final String CACHED_FILE = "channels.m3u";
    private static final String PREFS = "aurora";
    private static final String SOURCE = "playlist_source";

    private PlaylistImporter() { }

    public static List<Channel> fromUrl(Context context, String address) throws IOException {
        if (!address.startsWith("https://") && !address.startsWith("http://"))
            throw new IOException("Use a valid http:// or https:// M3U playlist URL.");
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(address).openConnection();
            connection.setConnectTimeout(12000);
            connection.setReadTimeout(20000);
            connection.setInstanceFollowRedirects(true);
            connection.setRequestProperty("User-Agent", "AuroraTV/2.0 (Android)");
            int response = connection.getResponseCode();
            if (response < 200 || response >= 300)
                throw new IOException("The playlist server returned HTTP " + response);
            byte[] bytes;
            InputStream input = connection.getInputStream();
            try { bytes = readLimited(input); }
            finally { input.close(); }
            List<Channel> parsed = parse(bytes, connection.getURL().toExternalForm());
            if (parsed.isEmpty()) throw new IOException("The playlist has no supported HTTP/HTTPS/RTSP channel URLs.");
            save(context, bytes);
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                    .putString(SOURCE, address).apply();
            return parsed;
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    public static List<Channel> fromFile(Context context, Uri file) throws IOException {
        InputStream stream = context.getContentResolver().openInputStream(file);
        if (stream == null) throw new IOException("Cannot read that playlist file.");
        byte[] bytes;
        try { bytes = readLimited(stream); }
        finally { stream.close(); }
        List<Channel> parsed = parse(bytes, null);
        if (parsed.isEmpty()) throw new IOException("No supported channel URLs were found in the selected file.");
        save(context, bytes);
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove(SOURCE).apply();
        return parsed;
    }

    public static List<Channel> restore(Context context) throws IOException {
        File file = new File(context.getFilesDir(), CACHED_FILE);
        if (!file.isFile()) {
            // Preserve a previous V1 imported playlist on upgrades.
            file = new File(context.getFilesDir(), "playlist.m3u");
        }
        if (!file.isFile()) return Collections.emptyList();
        FileInputStream stream = new FileInputStream(file);
        byte[] bytes;
        try { bytes = readLimited(stream); }
        finally { stream.close(); }
        // Local files have no URL base; do not resolve their entries against the public URL.
        String source = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getString(SOURCE, null);
        return parse(bytes, source);
    }

    public static String rememberedUrl(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(SOURCE, DEFAULT_URL);
    }

    private static List<Channel> parse(byte[] bytes, String base) throws IOException {
        InputStreamReader reader = new InputStreamReader(new ByteArrayInputStream(bytes), "UTF-8");
        try { return M3uParser.parse(reader, base); }
        finally { reader.close(); }
    }

    private static byte[] readLimited(InputStream input) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] block = new byte[16384];
        int n;
        while ((n = input.read(block)) != -1) {
            if (output.size() > MAX_BYTES - n)
                throw new IOException("Playlist exceeds the 32 MB safety limit.");
            output.write(block, 0, n);
        }
        return output.toByteArray();
    }

    private static void save(Context context, byte[] bytes) throws IOException {
        File destination = new File(context.getFilesDir(), CACHED_FILE);
        File temporary = new File(context.getFilesDir(), CACHED_FILE + ".new");
        FileOutputStream output = new FileOutputStream(temporary);
        try { output.write(bytes); output.getFD().sync(); }
        finally { output.close(); }
        if (!temporary.renameTo(destination)) {
            if (destination.exists() && !destination.delete())
                throw new IOException("Could not update saved playlist.");
            if (!temporary.renameTo(destination)) throw new IOException("Could not save playlist.");
        }
    }
}
