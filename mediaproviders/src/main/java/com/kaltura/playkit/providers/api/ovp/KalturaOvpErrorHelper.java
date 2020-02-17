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

package com.kaltura.playkit.providers.api.ovp;

import android.text.TextUtils;

import com.kaltura.netkit.utils.ErrorElement;
import com.kaltura.netkit.utils.RestrictionError;
import com.kaltura.playkit.providers.MediaProvidersUtils;

/**
 * @hide
 */

public class KalturaOvpErrorHelper {

    public static ErrorElement getErrorElement(String code, String message){

        final String NO_FILES_FOUND = "NoFilesFound";

        if (TextUtils.isEmpty(code)) {
            code = NO_FILES_FOUND;
        }

        if (TextUtils.isEmpty(message)) {
            message = "unknown error";
        }

        switch (code){
            /*case "SCHEDULED_RESTRICTED":
            case "COUNTRY_RESTRICTED":*/
            case NO_FILES_FOUND:
                return MediaProvidersUtils.buildNotFoundlErrorElement("Content can't be played due to lack of sources");

            default:
                String messageCode = code;
                if (!"".equals(messageCode)) {
                    messageCode += ":";
                }
                return new RestrictionError(messageCode + message, RestrictionError.Restriction.NotAllowed);
        }
    }

    public static ErrorElement getErrorElement(String code) {
        return getErrorElement(code, null);
    }
}
