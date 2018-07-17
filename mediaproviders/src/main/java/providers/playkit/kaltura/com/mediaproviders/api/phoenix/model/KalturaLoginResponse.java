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

package providers.playkit.kaltura.com.mediaproviders.api.phoenix.model;

import com.kaltura.netkit.connect.response.BaseResult;
import com.kaltura.netkit.utils.ErrorElement;
import com.kaltura.playkit.api.phoenix.model.KalturaLoginSession;
import com.kaltura.playkit.api.phoenix.model.KalturaOTTUser;

/**
 * @hide
 */

public class KalturaLoginResponse extends BaseResult {

    private com.kaltura.playkit.api.phoenix.model.KalturaLoginSession loginSession;
    private com.kaltura.playkit.api.phoenix.model.KalturaOTTUser user;

    public KalturaLoginResponse(ErrorElement error) {
        super(error);
    }

    public KalturaLoginResponse() {
        super();
    }

    public KalturaLoginSession getLoginSession() {
        return loginSession;
    }

    public KalturaOTTUser getUser() {
        return user;
    }
}
