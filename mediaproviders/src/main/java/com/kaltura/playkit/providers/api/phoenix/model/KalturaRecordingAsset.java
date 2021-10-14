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

public class KalturaRecordingAsset extends KalturaProgramAsset {
    
    private String recordingId;
    private KalturaRecordingType recordingType;

    public String getRecordingId() {
        return recordingId;
    }

    public KalturaRecordingType getRecordingType() {
        return recordingType;
    }
}
