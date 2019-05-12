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

import android.text.TextUtils;

import com.kaltura.netkit.utils.ErrorElement;


import java.util.ArrayList;

import com.kaltura.playkit.providers.api.base.model.BasePlaybackContext;
import com.kaltura.playkit.providers.api.phoenix.PhoenixErrorHelper;

/**
 * Created by tehilarozin on 02/11/2016.
 */

public class KalturaPlaybackContext extends BasePlaybackContext {

    private ArrayList<KalturaPlaybackSource> sources;
    private ArrayList<KalturaPlaybackCaption> playbackCaptions;

    public ArrayList<KalturaPlaybackSource> getSources() {
        return sources;
    }

    public ArrayList<KalturaPlaybackCaption> getPlaybackCaptions() {
        return playbackCaptions;
    }
    @Override
    protected ErrorElement getErrorElement(KalturaAccessControlMessage message) {

        if (message != null) {
            if (message.getCode() != null && TextUtils.equals("OK", message.getCode())) {
                return null;
            } else {
                return PhoenixErrorHelper.getErrorElement(message.getCode(), message.getMessage());
            }
        } else {
            return PhoenixErrorHelper.getErrorElement(PhoenixErrorHelper.ERROR_CODE_UNAVILABLE, PhoenixErrorHelper.ERROR_MESSAGE_UNAVILABLE);
        }
    }
}
