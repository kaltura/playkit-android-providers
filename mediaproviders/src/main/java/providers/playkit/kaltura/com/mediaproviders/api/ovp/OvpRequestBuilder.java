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

package providers.playkit.kaltura.com.mediaproviders.api.ovp;

import com.kaltura.netkit.connect.request.RequestBuilder;
import com.kaltura.netkit.connect.request.RequestElement;
import com.kaltura.playkit.api.ovp.services.OvpService;

/**
 * @hide
 */

public class OvpRequestBuilder extends RequestBuilder<OvpRequestBuilder> {

    @Override
    public RequestElement build() {
        addParams(OvpService.getOvpConfigParams());
        return super.build();
    }
}
