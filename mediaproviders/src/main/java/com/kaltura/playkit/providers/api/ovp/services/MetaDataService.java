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
public class MetaDataService extends OvpService {

    public static OvpRequestBuilder list(String baseUrl, String ks, String entryId) {
        JsonObject filter = new JsonObject();
        filter.addProperty("objectType", "KalturaMetadataFilter");
        filter.addProperty("objectIdEqual", entryId);
        filter.addProperty("metadataObjectTypeEqual", "1");

        JsonObject params = new JsonObject();
        params.add("filter", filter);
        params.addProperty("ks", ks);

        return new OvpRequestBuilder().service("metadata_metadata")
                .action("list")
                .method(HTTP_METHOD_POST)
                .url(baseUrl)
                .tag("metadata_metadata-list")
                .params(params);
    }
}
