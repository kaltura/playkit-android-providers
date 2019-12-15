/*
 * ============================================================================
 * Copyright (C) 2017 Kaltura Inc.
 * 
 * Licensed under the AGPLv3 license, unless a different license for a
 * particular library is specified in the applicable library path.
 * 
 * You may obtain a copy of the License at
 * https://www.gnu.org/licenses/agpl-3.0.html
 * ============================================================================
 */

package com.kaltura.playkit.providers.api.ovp.model;


import com.kaltura.netkit.connect.response.BaseResult;

import java.util.Arrays;
import java.util.List;

/**
 * @hide
 */

public class KalturaPlaylist extends BaseResult {
    private String id;
    private String name;
    private String description;
    private String thumbnailUrl;
    private KalturaEntryType type;
    private KalturaPlaylistType playlistType;
    private Integer duration;

//    private String playlistContent;
//    private Integer plays;
//    private Integer views;
    //private Integer totalResults;

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public KalturaEntryType getType() { return type; }

    public KalturaPlaylistType getPlaylistType() { return playlistType; }

    public Integer getDuration() { return duration; }

    public void setId(String id) {
        this.id = id;
    }

    public boolean hasId() {
        return id != null;
    }

    public boolean hasName() {
        return name != null;
    }

    public boolean hasDescription() {
        return description != null;
    }

    public boolean hasThumbnail() {
        return thumbnailUrl != null;
    }

    public boolean hasPlaylistType() {
        return playlistType != null;
    }

    public boolean hasType() {
        return type != null;
    }

    public boolean hasDuration() { return this.duration != null; }

//    public String getPlaylistContent() { return playlistContent; }
//
//    public Integer getPlays() { return plays; }
//
//    public Integer getViews() { return views; }
//
//    public Integer getTotalResults() { return totalResults; }

//    public boolean hasPlaylistContent() { return playlistContent != null; }
//
//    public boolean hasPlays() { return this.plays != null; }
//
//    public boolean hasViews(Integer views) { return this.views != null; }
//
//    public boolean hasTotalResults() { return this.totalResults != null; }

}



