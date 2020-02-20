package com.kaltura.playkit.providers.api.ovp.model;

import com.google.gson.annotations.SerializedName;

public enum KalturaPlaylistType {
    @SerializedName("3")
    STATIC_LIST,
    @SerializedName("10")
    DYNAMIC,
    @SerializedName("101")
    EXTERNAL
}
