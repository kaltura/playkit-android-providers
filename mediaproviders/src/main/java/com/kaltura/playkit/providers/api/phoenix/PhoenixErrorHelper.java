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

package com.kaltura.playkit.providers.api.phoenix;

import android.text.TextUtils;

import com.kaltura.netkit.utils.ErrorElement;
import com.kaltura.netkit.utils.RestrictionError;

/**
 * @hide
 */

public class PhoenixErrorHelper {

    public static final String ERROR_CODE_UNAVILABLE = "Unavailable";
    public static final String ERROR_MESSAGE_UNAVILABLE = "unknown error";

    /**
     * in case specific error codes should be parsed to predefined errors.
     * @param code
     * @param message
     * @return
     */
    public static ErrorElement getErrorElement(String code, String message){
        ErrorElement errorElement = getDefinedErrorElement(code, message);
        if(errorElement == null){
            errorElement = new ErrorElement(code, message);
        }
        return errorElement;
    }

    public static ErrorElement getErrorElement(ErrorElement error){
        ErrorElement errorElement = getDefinedErrorElement(error.getCode(), error.getMessage());
        if(errorElement == null){
            return error;
        }
        return errorElement;
    }

    /**
     * parse phoenix specific errors to playkit errors.
     * errors with text code are messages that my be retrieved from the "getPlaybackContext" API.
     *
     * @param code
     * @param message
     * @return
     */
    private static ErrorElement getDefinedErrorElement(String code, String message) {

        if (code == null) {
            code = ERROR_CODE_UNAVILABLE;
        }

        if (TextUtils.isEmpty(message)) {
            message = ERROR_MESSAGE_UNAVILABLE;
        }
        return new ErrorElement(message, code).setName("OTTError");
    }
}
