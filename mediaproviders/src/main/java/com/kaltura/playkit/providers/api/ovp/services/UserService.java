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

package com.kaltura.playkit.providers.api.ovp.services;

import androidx.annotation.NonNull;
import com.google.gson.JsonObject;
import com.kaltura.playkit.providers.api.ovp.OvpRequestBuilder;

import static com.kaltura.playkit.utils.Consts.HTTP_METHOD_POST;


/**
 * @hide
 */

public class UserService extends OvpService {

    /**
     * @param baseUrl baseURL
     * @param loginId  mandatory  user's email address that identifies the user for login
     * @param password mandatory  user's password
     * @param partnerId optional  if value = 0, won't be used
     * @return OvpRequestBuilder
     */
    public static OvpRequestBuilder loginByLoginId(@NonNull String baseUrl, @NonNull String loginId, @NonNull String password, int partnerId) {
        JsonObject params = new JsonObject();
        params.addProperty("loginId", loginId);
        params.addProperty("password", password);
        if (partnerId > 0) {
            params.addProperty("partnerId", partnerId);
        }

        return new OvpRequestBuilder()
                .service("user")
                .action("loginByLoginId")
                .method(HTTP_METHOD_POST)
                .url(baseUrl)
                .tag("user-loginbyloginid")
                .params(params);
    }

}
