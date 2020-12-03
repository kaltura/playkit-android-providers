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

import android.text.TextUtils;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import com.kaltura.playkit.providers.api.ovp.APIDefines;
import com.kaltura.playkit.providers.api.ovp.OvpRequestBuilder;

import static com.kaltura.playkit.utils.Consts.HTTP_METHOD_POST;


/**
 * @hide
 */

public class BaseEntryService extends OvpService {

    /*public static RequestBuilder entryInfo(String baseUrl, String ks, int partnerId, String entryId) {

        MultiRequestBuilder multiRequestBuilder = (MultiRequestBuilder) OvpService.getMultirequest(baseUrl, ks, partnerId)
                .tag("mediaAsset-multi-get");

        if(TextUtils.isEmpty(ks)){
            multiRequestBuilder.add(OvpSessionService.anonymousSession(baseUrl, partnerId));

            ks = "{1:result:ks}";
        }

        return multiRequestBuilder.add(list(baseUrl, ks, entryId),
                getPlaybackContext(baseUrl, ks, entryId),
                MetaDataService.list(baseUrl,ks,entryId));
    }*/

    public static OvpRequestBuilder list(String baseUrl, String ks, String entryId, String referenceId) {
        return new OvpRequestBuilder()
                .service("baseEntry")
                .action("list")
                .method(HTTP_METHOD_POST)
                .url(baseUrl)
                .tag("baseEntry-list")
                .params(getEntryListReqParams(ks, entryId, referenceId));
    }

    private static JsonObject getEntryListReqParams(String ks, String entryId, String referenceId) {

        BaseEntryListParams baseEntryListParams = new BaseEntryListParams(ks);
        if (!TextUtils.isEmpty(entryId)) {
            baseEntryListParams.filter.redirectFromEntryId = entryId;
        } else if (!TextUtils.isEmpty(referenceId)) {
            baseEntryListParams.filter.referenceIdEqual = referenceId;
        }
        baseEntryListParams.responseProfile.fields = "id,name,description,thumbnailUrl,dataUrl,duration,msDuration,flavorParamsIds,mediaType,type,tags,dvrStatus";
        baseEntryListParams.responseProfile.type = APIDefines.ResponseProfileType.IncludeFields;

        return new Gson().toJsonTree(baseEntryListParams).getAsJsonObject();
    }

    public static OvpRequestBuilder getContextData(String baseUrl, String ks, String entryId) {
        JsonObject params = new JsonObject();
        params.addProperty("entryId", entryId);
        params.addProperty("ks", ks);

        JsonObject contextDataParams = new JsonObject();
        contextDataParams.addProperty("objectType","KalturaContextDataParams");
        params.add("contextDataParams", contextDataParams);

        return new OvpRequestBuilder().service("baseEntry")
                .action("getContextData")
                .method(HTTP_METHOD_POST)
                .url(baseUrl)
                .tag("baseEntry-getContextData")
                .params(params);
    }

    public static OvpRequestBuilder getPlaybackContext(String baseUrl, String ks, String entryId, String referrer) {
        JsonObject params = new JsonObject();
        params.addProperty("entryId", entryId);
        params.addProperty("ks", ks);

        JsonObject contextDataParams = new JsonObject();
        contextDataParams.addProperty("objectType","KalturaContextDataParams");
        if(!TextUtils.isEmpty(referrer)) {
            contextDataParams.addProperty("referrer", referrer);
        }

        params.add("contextDataParams", contextDataParams);

        return new OvpRequestBuilder().service("baseEntry")
                .action("getPlaybackContext")
                .method(HTTP_METHOD_POST)
                .url(baseUrl)
                .tag("baseEntry-getPlaybackContext")
                .params(params);
    }




    static class BaseEntryListParams {
        String ks;
        Filter filter;
        ResponseProfile responseProfile;

        public BaseEntryListParams(String ks) {
            this.ks = ks;
            this.filter = new Filter();
            this.responseProfile = new ResponseProfile();
        }

        class Filter {
            String idEqual;
            String redirectFromEntryId;
            String referenceIdEqual;
        }
    }

}
