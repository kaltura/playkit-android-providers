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

import com.kaltura.netkit.connect.response.BaseResult;

/**
 * @hide
 */

public class KalturaStartWidgetSessionResponse extends BaseResult {
    private int partnerId;
    private String ks;
    private String userId;

    public int getPartnerId() {
        return partnerId;
    }

    public String getKs() {
        return ks;
    }

    public String getUserId() {
        return userId;
    }
}
