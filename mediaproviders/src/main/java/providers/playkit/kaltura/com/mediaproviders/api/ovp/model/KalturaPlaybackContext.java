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

package providers.playkit.kaltura.com.mediaproviders.api.ovp.model;

import com.kaltura.netkit.utils.ErrorElement;
import com.kaltura.playkit.api.base.model.BasePlaybackContext;
import com.kaltura.playkit.api.ovp.KalturaOvpErrorHelper;
import com.kaltura.playkit.api.ovp.model.KalturaFlavorAsset;
import com.kaltura.playkit.api.ovp.model.KalturaPlaybackSource;

import java.util.ArrayList;

/**
 * @hide
 */

public class KalturaPlaybackContext extends BasePlaybackContext{

    private ArrayList<com.kaltura.playkit.api.ovp.model.KalturaPlaybackSource> sources;
    private ArrayList<com.kaltura.playkit.api.ovp.model.KalturaFlavorAsset> flavorAssets;

    public KalturaPlaybackContext() {
    }

    public ArrayList<KalturaPlaybackSource> getSources() {
        return sources;
    }

    public ArrayList<KalturaFlavorAsset> getFlavorAssets() {
        return flavorAssets;
    }

    @Override
    protected ErrorElement getErrorElement(KalturaAccessControlMessage message) {
        return KalturaOvpErrorHelper.getErrorElement(message.getCode(), message.getMessage());
    }
}

