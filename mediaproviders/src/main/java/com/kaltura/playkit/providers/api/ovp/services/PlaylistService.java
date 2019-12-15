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

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.kaltura.playkit.providers.api.ovp.APIDefines;
import com.kaltura.playkit.providers.api.ovp.OvpRequestBuilder;

import static com.kaltura.playkit.utils.Consts.HTTP_METHOD_POST;


/**
 * @hide
 */
public class PlaylistService extends OvpService {

    public static OvpRequestBuilder get(String baseUrl, String ks, String playlistId) {

        return new OvpRequestBuilder().service("playlist")
                .action("get")
                .method(HTTP_METHOD_POST)
                .url(baseUrl)
                .tag("playlist-get")
                .params(getPlaylistGetParams(ks, playlistId));
    }

    public static OvpRequestBuilder execute(String baseUrl, String ks, String playlistId, Integer pageSize, Integer pageIndex) {

        return new OvpRequestBuilder().service("playlist")
                .action("execute")
                .method(HTTP_METHOD_POST)
                .url(baseUrl)
                .tag("playlist-execute")
                .params(getPlaylistExecuteParams(ks, playlistId, pageSize, pageIndex));
    }

    private static JsonObject getPlaylistGetParams(String ks, String playlistId) {

        PlaylistService.PlaylistParams playlistParams = new PlaylistService.PlaylistParams(ks, playlistId);
        playlistParams.ks = ks;
        playlistParams.id = playlistId;
        playlistParams.responseProfile.fields = "id,name,playlistType,type,description,thumbnailUrl,duration";// ,totalResults,plays,views,playlistContent";
        playlistParams.responseProfile.type = APIDefines.ResponseProfileType.IncludeFields;

        return new Gson().toJsonTree(playlistParams).getAsJsonObject();
    }

    private static JsonObject getPlaylistExecuteParams(String ks, String playlistId, Integer pageSize, Integer pageIndex) {

        PlaylistService.PlaylistParams playlistParams = new PlaylistService.PlaylistParams(ks, playlistId);
        playlistParams.responseProfile.fields = "id,referenceId,name,description,thumbnailUrl,dataUrl,msDuration,flavorParamsIds,mediaType,type,tags,externalSourceType,searchText";
        playlistParams.responseProfile.type = APIDefines.ResponseProfileType.IncludeFields;
        KalturaFilterPager pager = null;
        if (pageSize != null && pageIndex != null && pageIndex <= pageSize) {
            playlistParams.pager = new KalturaFilterPager(pageSize,pageIndex);
        }
        return new Gson().toJsonTree(playlistParams).getAsJsonObject();
    }

    static class PlaylistParams {
        String ks;
        String id;
        ResponseProfile responseProfile;
        KalturaFilterPager pager;

        public PlaylistParams(String ks, String playlistId) {
            this.ks = ks;
            this.id = playlistId;
            this.responseProfile = new ResponseProfile();
        }
    }

    static class KalturaFilterPager {
        int pageSize;
        int pageIndex;

        public KalturaFilterPager(int pageSize, int pageIndex) {
            this.pageSize = pageSize;
            this.pageIndex = pageIndex;
        }
    }
}
