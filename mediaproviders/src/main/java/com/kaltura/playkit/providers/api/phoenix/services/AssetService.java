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

import android.text.TextUtils;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.List;
import java.util.Map;

import com.kaltura.playkit.providers.api.phoenix.APIDefines;
import com.kaltura.playkit.providers.api.phoenix.PhoenixRequestBuilder;

import static com.kaltura.playkit.utils.Consts.HTTP_METHOD_POST;

/**
 * @hide
 */

public class AssetService extends PhoenixService {

    public static PhoenixRequestBuilder get(String baseUrl, String ks, String assetId, APIDefines.AssetReferenceType referenceType) {
        JsonObject params = new JsonObject();
        params.addProperty("ks", ks);
        params.addProperty("id", assetId);
        params.addProperty("assetReferenceType", referenceType.value);

        return new PhoenixRequestBuilder()
                .service("asset")
                .action("get")
                .method(HTTP_METHOD_POST)
                .url(baseUrl)
                .tag("asset-get")
                .params(params);
    }

    /**
     * builds the request for detailed asset sources data, including DRM data if has any.
     * @param baseUrl - api base server url
     * @param ks - valid session token
     * @param assetId - Asset id
     * @param assetType - {@link APIDefines.KalturaAssetType}
     * @param contextOptions - list of extra details to narrow search of sources
     * @return
     */
    public static PhoenixRequestBuilder getPlaybackContext(String baseUrl, String ks, String assetId,
                                                           APIDefines.KalturaAssetType assetType, KalturaPlaybackContextOptions contextOptions){

        JsonObject params = new JsonObject();
        params.addProperty("ks", ks);
        params.addProperty("assetId", assetId);
        params.addProperty("assetType", assetType.value);
        params.add("contextDataParams", contextOptions != null ? contextOptions.toJson() : new JsonObject());

        return new PhoenixRequestBuilder()
                .service("asset")
                .action("getPlaybackContext")
                .method(HTTP_METHOD_POST)
                .url(baseUrl)
                .tag("asset-getPlaybackContext")
                .params(params);
    }


    public static class KalturaPlaybackContextOptions{

        private APIDefines.PlaybackContextType context;
        private APIDefines.KalturaUrlType urlType;
        private APIDefines.KalturaStreamerType streamerType;
        private String protocol;
        private String assetFileIds;
        private String referrer;
        private Map<String,String> adapterData;

        public KalturaPlaybackContextOptions(APIDefines.PlaybackContextType context){
            this.context = context;
        }

        public KalturaPlaybackContextOptions setMediaProtocol(String protocol){
            this.protocol = protocol;
            return this;
        }

        public KalturaPlaybackContextOptions setReferrer(String referrer){
            this.referrer = referrer;
            return this;
        }

        public KalturaPlaybackContextOptions setUrlType(APIDefines.KalturaUrlType urlType) {
            this.urlType = urlType;
            return this;
        }

        public KalturaPlaybackContextOptions setStreamerType(APIDefines.KalturaStreamerType streamerType) {
            this.streamerType = streamerType;
            return this;
        }

        public KalturaPlaybackContextOptions setMediaFileIds(String ids){
            this.assetFileIds = ids;
            return this;
        }

        public KalturaPlaybackContextOptions setMediaFileIds(List<String> ids){
            this.assetFileIds = TextUtils.join(",", ids);
            return this;
        }

        public KalturaPlaybackContextOptions setAdapterData(Map<String,String> adapterData){
            this.adapterData = adapterData;
            return this;
        }

        public JsonObject toJson(){
            JsonObject params = new JsonObject();
            if (context != null) {
                params.addProperty("context", context.value);
            }

            if (urlType != null) {
                params.addProperty("urlType", urlType.value);
            }

            if (streamerType != null) {
                params.addProperty("streamerType", streamerType.value);
            }

            if (!TextUtils.isEmpty(protocol)) {
                params.addProperty("mediaProtocol", protocol);
            }

            if (!TextUtils.isEmpty(assetFileIds)) {
                params.addProperty("assetFileIds", assetFileIds);
            }

            if (!TextUtils.isEmpty(referrer)) {
                params.addProperty("referrer", referrer);
            }

//            if (adapterData != null && !adapterData.isEmpty()) {
//                JsonObject adapterDataJson = new JsonObject();
//                for (Map.Entry<String,String> adapterDataEntry : adapterData.entrySet()) {
//                    JsonObject adapterDataItemJsonExternal = new JsonObject();
//                    JsonObject adapterDataItemJsonInternal = new JsonObject();
//                    if (adapterDataEntry == null || TextUtils.isEmpty(adapterDataEntry.getValue())) {
//                        continue;
//                    }
//                    adapterDataItemJsonInternal.addProperty("objectType", "KalturaStringValue");
//                    adapterDataItemJsonInternal.addProperty("value", adapterDataEntry.getValue());
//                    adapterDataJson.add(adapterDataEntry.getKey(), adapterDataItemJsonInternal);
//                }
//                params.add("adapterData", adapterDataJson);
//            }

//            if (adapterData != null && !adapterData.isEmpty()) {
//                JsonArray adapterDataArray = new JsonArray();
//                for (Map.Entry<String,String> adapterDataEntry : adapterData.entrySet()) {
//                    JsonObject adapterDataItemJsonExternal = new JsonObject();
//                    JsonObject adapterDataItemJsonInternal = new JsonObject();
//                    if (adapterDataEntry == null || TextUtils.isEmpty(adapterDataEntry.getValue())) {
//                        continue;
//                    }
//                    adapterDataItemJsonInternal.addProperty("objectType", "KalturaStringValue");
//                    adapterDataItemJsonInternal.addProperty("value", adapterDataEntry.getValue());
//                    adapterDataItemJsonExternal.add(adapterDataEntry.getKey(), adapterDataItemJsonInternal);
//                    adapterDataArray.add(adapterDataItemJsonExternal);
//                }
//                params.add("adapterData", adapterDataArray);
//            }

            if (adapterData != null && !adapterData.isEmpty()) {
                JsonArray adapterDataArray = new JsonArray();
                JsonObject adapterDataJson = new JsonObject();

                for (Map.Entry<String,String> adapterDataEntry : adapterData.entrySet()) {
                    JsonObject adapterDataItemJsonInternal = new JsonObject();
                    if (adapterDataEntry == null || TextUtils.isEmpty(adapterDataEntry.getValue())) {
                        continue;
                    }
                    adapterDataItemJsonInternal.addProperty("objectType", "KalturaStringValue");
                    adapterDataItemJsonInternal.addProperty("value", adapterDataEntry.getValue());
                    adapterDataJson.add(adapterDataEntry.getKey(), adapterDataItemJsonInternal);

                }
                adapterDataArray.add(adapterDataJson);
                params.add("adapterData", adapterDataArray);
            }
            return params;
        }
    }
}
