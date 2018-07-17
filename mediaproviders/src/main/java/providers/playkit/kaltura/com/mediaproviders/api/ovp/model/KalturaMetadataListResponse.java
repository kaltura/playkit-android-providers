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
import com.kaltura.playkit.api.ovp.model.KalturaMetadata;

import java.util.List;
/**
 * @hide
 */

public class KalturaMetadataListResponse extends BaseResult {

    public List<KalturaMetadata> objects;
    int totalCount;
}
