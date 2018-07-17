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

import com.kaltura.netkit.connect.response.BaseResult;
import com.kaltura.playkit.api.ovp.model.KalturaFlavorAsset;

import java.util.ArrayList;

/**
 * @hide
 */

public class KalturaEntryContextDataResult extends BaseResult {

    ArrayList<com.kaltura.playkit.api.ovp.model.KalturaFlavorAsset> flavorAssets;


    public ArrayList<KalturaFlavorAsset> getFlavorAssets() {
        return flavorAssets;
    }


}
