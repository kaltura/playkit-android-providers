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

package com.kaltura.playkit.providers.api.phoenix.services;

import com.google.gson.JsonObject;

import com.kaltura.playkit.providers.api.phoenix.PhoenixRequestBuilder;

import static com.kaltura.playkit.utils.Consts.HTTP_METHOD_POST;

/**
 * Created by tehilarozin on 17/11/2016.
 */

public class LicensedUrlService extends PhoenixService {

    /**
     * both VOD and live content uses the media license request
     * @param baseUrl String
     * @param ks String
     * @param assetId String
     * @param mediaId String
     * @param mediaBaseUrl String
     * @return PhoenixRequestBuilder
     */
    public static PhoenixRequestBuilder getForMedia(String baseUrl, String ks, String assetId, String mediaId, String mediaBaseUrl) {
        JsonObject requestProperty = new JsonObject();
        requestProperty.addProperty("objectType", "KalturaLicensedUrlMediaRequest");
        requestProperty.addProperty("contentId", mediaId);
        requestProperty.addProperty("baseUrl", mediaBaseUrl);
        requestProperty.addProperty("assetId", assetId);

        return getLicensedLinksRequestBuilder(baseUrl, ks, "licensedlink-media-get", requestProperty);
    }

    /**
     * @param baseUrl String
     * @param ks String
     * @param streamType catchup / start_over / trick_play
     * @param startDate long
     * @return PhoenixRequestBuilder
     */
    public static PhoenixRequestBuilder getForShiftedLive(String baseUrl, String ks, String assetId, String streamType, long startDate) {

        JsonObject requestProperty = new JsonObject();
        requestProperty.addProperty("objectType", "KalturaLicensedUrlEpgRequest");
        requestProperty.addProperty("streamType", streamType);
        requestProperty.addProperty("startDate", startDate);
        requestProperty.addProperty("assetId", assetId);

        return getLicensedLinksRequestBuilder(baseUrl, ks, "licensedlink-epg-get", requestProperty);
    }

    public static PhoenixRequestBuilder getForRecording(String baseUrl, String ks, String assetId, String fileType) {

        JsonObject requestProperty = new JsonObject();
        requestProperty.addProperty("objectType", "KalturaLicensedUrlEpgRequest");
        requestProperty.addProperty("fileType", fileType); //file format (HD,SD...)
        requestProperty.addProperty("assetId", assetId);

        return getLicensedLinksRequestBuilder(baseUrl, ks, "licensedlink-rec-get", requestProperty);
    }

    private static PhoenixRequestBuilder getLicensedLinksRequestBuilder(String baseUrl, String ks, String tag, JsonObject requestProperty) {
        JsonObject reqParams = new JsonObject();
        if(!ks.equals("")) {
            reqParams.addProperty("ks", ks);
        }
        reqParams.add("request", requestProperty);

        return new PhoenixRequestBuilder()
                .service("licensedUrl")
                .action("get")
                .method(HTTP_METHOD_POST)
                .url(baseUrl)
                .tag(tag)
                .params(reqParams);
    }

    //TODO: check if assetId is needed
}
