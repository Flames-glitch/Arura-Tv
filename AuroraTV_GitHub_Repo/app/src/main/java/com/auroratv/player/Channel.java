package com.auroratv.player;

/** Immutable representation of a channel supplied by the user's playlist. */
public final class Channel {
    public final String name;
    public final String group;
    public final String url;
    public final String logoUrl;
    public final String tvgId;

    public Channel(String name, String group, String url, String logoUrl, String tvgId) {
        this.name = name;
        this.group = group;
        this.url = url;
        this.logoUrl = logoUrl;
        this.tvgId = tvgId;
    }
}
