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

/**
 * @hide
 */

public class KalturaPlaylist extends BaseResult {

    private String id;
    private String name;
    private String description;
    private String thumbnailUrl;
    private KalturaEntryType type;

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

    public boolean hasType() {
        return type != null;
    }
}



