package com.kaltura.playkit.providers.api.ovp.model;

import com.google.gson.annotations.SerializedName;

public enum KalturaPlaylistType {
    @SerializedName("3")
    STATIC_LIST("3"),
    @SerializedName("10")
    DYNAMIC("10"),
    @SerializedName("101")
    EXTERNAL("101");

    public String type;

    KalturaPlaylistType(String type) {
        this.type = type;
    }
}
