package com.auroratv.player;

import java.util.Collections;
import java.util.List;

/** In-memory snapshot shared between channel browser and playback activity. */
public final class ChannelStore {
    private static volatile List<Channel> channels = Collections.emptyList();
    private ChannelStore() { }
    public static void replace(List<Channel> value) { channels = value; }
    public static List<Channel> all() { return channels; }
}
