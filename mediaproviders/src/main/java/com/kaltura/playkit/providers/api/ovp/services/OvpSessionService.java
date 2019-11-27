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

import com.google.gson.JsonObject;
import com.kaltura.playkit.providers.api.ovp.OvpRequestBuilder;

import static com.kaltura.playkit.utils.Consts.HTTP_METHOD_POST;


/**
 * @hide
 */

public class OvpSessionService extends OvpService {

    public static OvpRequestBuilder anonymousSession(String baseUrl, String widgetId){
        JsonObject params = new JsonObject();
        params.addProperty("widgetId", widgetId);

        return new OvpRequestBuilder()
                .service("session")
                .action("startWidgetSession")
                .method(HTTP_METHOD_POST)
                .url(baseUrl)
                .tag("session-startWidget")
                .params(params);
    }

    public static OvpRequestBuilder get(String baseUrl, String ks){
        JsonObject params = new JsonObject();
        params.addProperty("ks", ks);

        return new OvpRequestBuilder()
                .service("session")
                .action("get")
                .method(HTTP_METHOD_POST)
                .url(baseUrl)
                .tag("session-get")
                .params(params);
    }

    public static OvpRequestBuilder end(String baseUrl, String ks) {
        JsonObject params = new JsonObject();
        params.addProperty("ks", ks);

        return new OvpRequestBuilder()
                .service("session")
                .action("end")
                .method(HTTP_METHOD_POST)
                .url(baseUrl)
                .tag("session-end")
                .params(params);
    }
}
