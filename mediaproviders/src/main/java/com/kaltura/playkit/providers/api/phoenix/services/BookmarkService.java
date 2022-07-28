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

import com.google.gson.JsonObject;
import com.kaltura.playkit.providers.api.phoenix.PhoenixRequestBuilder;

import static com.kaltura.playkit.utils.Consts.HTTP_METHOD_POST;


/**
 * Created by zivilan on 21/11/2016.
 */

public class BookmarkService extends PhoenixService {

    public static PhoenixRequestBuilder actionAdd(String baseUrl, int partnerId, String ks, String assetType, String contextType, String assetId, String epgId, String actionType, long position, String fileId) {
        return new PhoenixRequestBuilder()
                .service("bookmark")
                .action("add")
                .method(HTTP_METHOD_POST)
                .url(baseUrl)
                .tag("bookmark-action-add")
                .params(addBookmarkGetReqParams(ks, assetId, epgId, assetType, contextType, actionType, position, fileId));
    }

    private static JsonObject addBookmarkGetReqParams(String ks, String assetId, String epgId, String assetType, String contextType, String actionType, long position, String fileId) {
        JsonObject playerData = new JsonObject();
        playerData.addProperty("objectType", "KalturaBookmarkPlayerData");
        playerData.addProperty("action", actionType);
        playerData.addProperty("fileId", fileId);

        JsonObject bookmark = new JsonObject();
        bookmark.addProperty("objectType", "KalturaBookmark");
        bookmark.addProperty("id", assetId);
        if (!TextUtils.isEmpty(epgId)) {
            bookmark.addProperty("programId", epgId);
        }
        bookmark.addProperty("type", assetType);
        bookmark.addProperty("context", contextType);
        bookmark.addProperty("position", position);
        bookmark.add("playerData", playerData);

        JsonObject getParams = new JsonObject();
        getParams.addProperty("ks", ks);
        getParams.add("bookmark", bookmark);

        return getParams;
    }
}
