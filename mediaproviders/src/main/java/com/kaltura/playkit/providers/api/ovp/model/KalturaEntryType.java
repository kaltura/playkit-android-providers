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

public enum KalturaEntryType {

    @SerializedName("-1")
    AUTOMATIC("-1"),
    @SerializedName("1")
    MEDIA_CLIP("1"),
    @SerializedName("2")
    MIX("2"),
    @SerializedName("5")
    PLAYLIST("5"),
    @SerializedName("6")
    DATA("6"),
    @SerializedName("7")
    LIVE_STREAM("7"),
    @SerializedName("8")
    LIVE_CHANNEL("8"),
    @SerializedName("10")
    DOCUMENT("10"),
    @SerializedName("conference.CONFERENCE_ENTRY_SERVER")
    CONFERENCE_ENTRY_SERVER("conference.CONFERENCE_ENTRY_SERVER"),
    @SerializedName("externalMedia.externalMedia")
    EXTERNAL_MEDIA("externalMedia.externalMedia"),
    @SerializedName("sip.SIP_ENTRY_SERVER")
    SIP_ENTRY_SERVER("sip.SIP_ENTRY_SERVER");

    public String type;

    KalturaEntryType(String type) {
        this.type = type;
    }
}
