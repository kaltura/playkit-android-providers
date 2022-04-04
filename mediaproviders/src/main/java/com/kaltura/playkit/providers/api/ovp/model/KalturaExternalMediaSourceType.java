package com.kaltura.playkit.providers.api.ovp.model;

import com.google.gson.annotations.SerializedName;

public enum KalturaExternalMediaSourceType {
    @SerializedName("InterCall")
    INTERCALL("InterCall"),
    @SerializedName("YouTube")
    YOUTUBE("YouTube");

    public String type;

    KalturaExternalMediaSourceType(String type) {
        this.type = type;
    }
}
