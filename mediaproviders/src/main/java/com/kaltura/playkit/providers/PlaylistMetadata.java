package com.kaltura.playkit.providers;

import com.kaltura.playkit.PKPlaylistType;

public class PlaylistMetadata {

    private String id = "";
    private String name = "";
    private String description = "";
    private String thumbnailUrl = "";

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
}
