package com.auroratv.player;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Basic extended-M3U channel parser. Entries are not capped or silently discarded. */
public final class M3uParser {
    private static final Pattern ATTRIBUTE = Pattern.compile("([\\w-]+)\\s*=\\s*(?:\"([^\"]*)\"|'([^']*)'|([^\\s]+))");
    private M3uParser() {}

    public static List<Channel> parse(Reader reader, String baseUrl) throws IOException {
        BufferedReader lines = new BufferedReader(reader);
        List<Channel> result = new ArrayList<>();
        String line, name = null, group = "Other", logo = "", tvg = "";
        URI base = null;
        if (baseUrl != null && !baseUrl.isEmpty()) {
            try { base = URI.create(baseUrl); } catch (IllegalArgumentException ignored) { }
        }
        while ((line = lines.readLine()) != null) {
            line = line.trim();
            if (line.startsWith("\uFEFF")) line = line.substring(1);
            if (line.isEmpty()) continue;
            if (line.startsWith("#EXTINF")) {
                int comma = titleDelimiter(line);
                name = comma >= 0 ? line.substring(comma + 1).trim() : "";
                String attributes = comma >= 0 ? line.substring(0, comma) : line;
                group = attribute(attributes, "group-title");
                if (group.isEmpty()) group = "Other";
                logo = attribute(attributes, "tvg-logo");
                tvg = attribute(attributes, "tvg-id");
                if (name.isEmpty()) name = attribute(attributes, "tvg-name");
            } else if (line.startsWith("#EXTGRP:")) {
                group = line.substring("#EXTGRP:".length()).trim();
                if (group.isEmpty()) group = "Other";
            } else if (!line.startsWith("#")) {
                String url = resolve(base, line);
                if (isSupportedScheme(url)) {
                    String displayName = name == null || name.isEmpty() ? inferName(url) : name;
                    result.add(new Channel(displayName, group, url, logo, tvg));
                }
                name = null;
                group = "Other";
                logo = "";
                tvg = "";
            }
        }
        return Collections.unmodifiableList(result);
    }

    private static int titleDelimiter(String text) {
        boolean quoted = false; char quoteChar = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (quoted) { if (c == quoteChar) quoted = false; }
            else if (c == '\'' || c == '"') { quoted = true; quoteChar = c; }
            else if (c == ',') return i;
        }
        return -1;
    }

    private static String attribute(String s, String key) {
        Matcher m = ATTRIBUTE.matcher(s);
        while (m.find()) if (key.equalsIgnoreCase(m.group(1))) {
            for (int j = 2; j <= 4; j++) if (m.group(j) != null) return m.group(j).trim();
        }
        return "";
    }

    private static String resolve(URI base, String url) {
        try {
            URI uri = URI.create(url);
            return base != null ? base.resolve(uri).toString() : uri.toString();
        } catch (IllegalArgumentException e) { return ""; }
    }

    public static boolean isSupportedScheme(String url) {
        try {
            String scheme = URI.create(url).getScheme();
            if (scheme == null) return false;
            String s = scheme.toLowerCase(Locale.ROOT);
            return s.equals("https") || s.equals("http") || s.equals("rtsp");
        } catch (IllegalArgumentException e) { return false; }
    }

    private static String inferName(String url) {
        try {
            String path = URI.create(url).getPath();
            if (path != null && !path.isEmpty()) {
                String last = path.substring(path.lastIndexOf('/') + 1);
                if (!last.isEmpty()) return last;
            }
        } catch (IllegalArgumentException ignored) { }
        return "Live Stream";
    }
}
