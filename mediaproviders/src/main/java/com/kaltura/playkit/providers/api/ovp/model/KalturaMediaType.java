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

import com.google.gson.annotations.SerializedName;

/**
 * Created by tehilarozin on 21/12/2016.
 */

public enum KalturaMediaType {

    @SerializedName("1")
    VIDEO("1"),
    @SerializedName("2")
    IMAGE("2"),
    @SerializedName("5")
    AUDIO("5"),
    @SerializedName("201")
    LIVE_STREAM_FLASH("201"),
    @SerializedName("202")
    LIVE_STREAM_WINDOWS_MEDIA("202"),
    @SerializedName("203")
    LIVE_STREAM_REAL_MEDIA("203"),
    @SerializedName("204")
    LIVE_STREAM_QUICKTIME("204");

    public String type;

    KalturaMediaType(String type) {
        this.type = type;
    }
}
