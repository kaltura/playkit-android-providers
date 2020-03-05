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

public enum KalturaMediaType {

    @SerializedName("1")
    VIDEO,
    @SerializedName("2")
    IMAGE,
    @SerializedName("5")
    AUDIO,
    @SerializedName("201")
    LIVE_STREAM_FLASH,
    @SerializedName("202")
    LIVE_STREAM_WINDOWS_MEDIA,
    @SerializedName("203")
    LIVE_STREAM_REAL_MEDIA,
    @SerializedName("204")
    LIVE_STREAM_QUICKTIME
}
