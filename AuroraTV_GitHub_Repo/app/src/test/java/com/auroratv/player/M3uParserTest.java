package com.auroratv.player;

import org.junit.Test;
import java.io.StringReader;
import java.util.List;
import static org.junit.Assert.*;

public final class M3uParserTest {
    @Test public void parsesIptvOrgFormat() throws Exception {
        String m3u = "#EXTM3U\n"
                + "#EXTINF:-1 tvg-id=\"news.example\" tvg-logo=\"https://img.example/logo.png\" group-title=\"News\",Live News\n"
                + "https://streams.example/news.m3u8\n"
                + "#EXTINF:-1 group-title=\"Music\",Live Music\n"
                + "http://streams.example/music.mp4\n";
        List<Channel> result = M3uParser.parse(new StringReader(m3u),
                "https://iptv-org.github.io/iptv/index.m3u");
        assertEquals(2, result.size());
        assertEquals("Live News", result.get(0).name);
        assertEquals("News", result.get(0).group);
        assertEquals("news.example", result.get(0).tvgId);
        assertEquals("https://img.example/logo.png", result.get(0).logoUrl);
        assertEquals("http://streams.example/music.mp4", result.get(1).url);
    }

    @Test public void rejectsDangerousSchemesAndResolvesRelativeLinks() throws Exception {
        List<Channel> channels = M3uParser.parse(new StringReader(
                "#EXTM3U\n#EXTINF:-1,One\n../live/stream.m3u8\n"
                + "#EXTINF:-1,Two\nfile:///sdcard/private\n"),
                "https://example.org/tv/all/index.m3u");
        assertEquals(1, channels.size());
        assertEquals("https://example.org/tv/live/stream.m3u8", channels.get(0).url);
    }
}
