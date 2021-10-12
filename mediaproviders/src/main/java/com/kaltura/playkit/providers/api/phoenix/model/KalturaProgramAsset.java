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
    private long linearAssetId;

    public String getEpgId() {
        return epgId;
    }

    public long getEpgChannelId() {
        return epgChannelId;
    }

    public long getLinearAssetId() {
        return linearAssetId;
    }
}