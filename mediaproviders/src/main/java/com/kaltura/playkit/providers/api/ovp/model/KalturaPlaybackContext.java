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

import com.kaltura.netkit.utils.ErrorElement;


import java.util.ArrayList;

import com.kaltura.playkit.providers.api.base.model.BasePlaybackContext;
import com.kaltura.playkit.providers.api.ovp.KalturaOvpErrorHelper;

/**
 * @hide
 */

public class KalturaPlaybackContext extends BasePlaybackContext {

    private ArrayList<KalturaPlaybackSource> sources;
    private ArrayList<KalturaFlavorAsset> flavorAssets;
    private ArrayList<KalturaPlaybackCaption> playbackCaptions;

    public KalturaPlaybackContext() {
    }

    public ArrayList<KalturaPlaybackSource> getSources() {
        return sources;
    }

    public ArrayList<KalturaFlavorAsset> getFlavorAssets() {
        return flavorAssets;
    }

    public ArrayList<KalturaPlaybackCaption> getPlaybackCaptions() {
        return playbackCaptions;
    }

    @Override
    protected ErrorElement getErrorElement(KalturaAccessControlMessage message) {
        return KalturaOvpErrorHelper.getErrorElement(message.getCode(), message.getMessage());
    }
}

