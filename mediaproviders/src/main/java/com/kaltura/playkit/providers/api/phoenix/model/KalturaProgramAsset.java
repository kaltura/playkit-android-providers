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

package com.kaltura.playkit.providers.api.phoenix.model;

public class KalturaProgramAsset extends KalturaMediaAsset {
    
    private String epgId;
    private long epgChannelId;
    private long relatedMediaId;
    private long linearAssetId;
    private boolean enableCatchUp;
    private boolean enableCdvr;
    private boolean enableStartOver;
    private boolean enableTrickPlay;
    private String crid;
    
    public String getEpgId() {
        return epgId;
    }

    public long getRelatedMediaId() {
        return relatedMediaId;
    }

    public long getEpgChannelId() {
        return epgChannelId;
    }

    public long getLinearAssetId() {
        return linearAssetId;
    }

    public boolean isEnableCatchUp() {
        return enableCatchUp;
    }

    public boolean isEnableCdvr() {
        return enableCdvr;
    }

    public boolean isEnableStartOver() {
        return enableStartOver;
    }

    public boolean isEnableTrickPlay() {
        return enableTrickPlay;
    }

    public String getCrid() {
        return crid;
    }
}
