package com.kaltura.playkit.providers;

import com.kaltura.playkit.PKPlaylistType;

public class PlaylistMetadata {

    private String id = "";
    private String name = "";
    private String description = "";
    private String thumbnailUrl = "";
    private PKPlaylistType playlistType = PKPlaylistType.Unknown;
    private Integer duration = 0;

    public String getId() {
        return id;
    }

    public PlaylistMetadata setId(String id) {
        this.id = id;
        return this;
    }

    public String getName() {
        return name;
    }

    public PlaylistMetadata setName(String name) {
        this.name = name;
        return this;
    }

    public String getDescription() {
        return description;
    }

    public PlaylistMetadata setDescription(String description) {
        this.description = description;
        return this;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public PlaylistMetadata setThumbnailUrl(String thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
        return this;
    }

    public PKPlaylistType getPlaylistType() {
        return playlistType;
    }

    public PlaylistMetadata setPlaylistType(PKPlaylistType playlistType) {
        this.playlistType = playlistType;
        return this;
    }

    public Integer getDuration() {
        return duration;
    }

    public PlaylistMetadata setDuration(Integer duration) {
        this.duration = duration;
        return this;
    }
}
