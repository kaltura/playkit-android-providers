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

package com.kaltura.playkit.providers.ott;

import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.StringDef;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.kaltura.netkit.connect.executor.APIOkRequestsExecutor;
import com.kaltura.netkit.connect.executor.RequestQueue;
import com.kaltura.netkit.connect.request.MultiRequestBuilder;
import com.kaltura.netkit.connect.request.RequestBuilder;
import com.kaltura.netkit.connect.response.BaseResult;
import com.kaltura.netkit.connect.response.PrimitiveResult;
import com.kaltura.netkit.connect.response.ResponseElement;
import com.kaltura.netkit.utils.Accessories;
import com.kaltura.netkit.utils.ErrorElement;
import com.kaltura.netkit.utils.OnCompletion;
import com.kaltura.netkit.utils.SessionProvider;
import com.kaltura.playkit.PKLog;
import com.kaltura.playkit.PKMediaEntry;
import com.kaltura.playkit.PKMediaFormat;
import com.kaltura.playkit.PKMediaSource;
import com.kaltura.playkit.PKPlaylist;
import com.kaltura.playkit.PKPlaylistMedia;
import com.kaltura.playkit.PKPlaylistType;
import com.kaltura.playkit.providers.MediaProvidersUtils;
import com.kaltura.playkit.providers.api.SimpleSessionProvider;
import com.kaltura.playkit.providers.api.base.model.KalturaDrmPlaybackPluginData;
import com.kaltura.playkit.providers.api.ovp.model.KalturaMediaEntry;
import com.kaltura.playkit.providers.api.ovp.model.KalturaPlaylist;
import com.kaltura.playkit.providers.api.phoenix.APIDefines;
import com.kaltura.playkit.providers.api.phoenix.PhoenixErrorHelper;
import com.kaltura.playkit.providers.api.phoenix.PhoenixParser;
import com.kaltura.playkit.providers.api.phoenix.model.KalturaLoginSession;
import com.kaltura.playkit.providers.api.phoenix.model.KalturaMediaAsset;
import com.kaltura.playkit.providers.api.phoenix.model.KalturaPlaybackContext;
import com.kaltura.playkit.providers.api.phoenix.model.KalturaPlaybackSource;
import com.kaltura.playkit.providers.api.phoenix.model.KalturaRecordingAsset;
import com.kaltura.playkit.providers.api.phoenix.model.KalturaThumbnail;
import com.kaltura.playkit.providers.api.phoenix.services.AssetService;
import com.kaltura.playkit.providers.api.phoenix.services.OttUserService;
import com.kaltura.playkit.providers.api.phoenix.services.PhoenixService;
import com.kaltura.playkit.providers.base.BECallableLoader;
import com.kaltura.playkit.providers.base.BEMediaProvider;
import com.kaltura.playkit.providers.base.BEResponseListener;
import com.kaltura.playkit.providers.base.FormatsHelper;
import com.kaltura.playkit.providers.base.OnMediaLoadCompletion;
import com.kaltura.playkit.utils.Consts;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.kaltura.netkit.utils.ErrorElement.GeneralError;


/**
 * Created by tehilarozin on 27/10/2016.
 */

/*
 * usages:
 *
 * by formats - request will fetch all available source, filter sources response according to requested formats list
 *
 * by mediaFile ids - request include the requests file ids and will fetch sources for those files only.
 *
 * mandatory fields: assetIds, assetType, contextType
 *
 *
 * */

public class PhoenixPlaylistProvider extends BEMediaProvider {

    private static final PKLog log = PKLog.get("PhoenixMediaProvider");

    private static String LIVE_ASSET_OBJECT_TYPE = "KalturaLiveAsset"; //Might be needed to support in KalturaProgramAsset for EPG

    public static final String KALTURA_API_EXCEPTION = "KalturaAPIException";
    public static final String ERROR = "error";
    public static final String OBJECT_TYPE = "objectType";
    public static final String CODE = "code";
    public static final String MESSAGE = "message";
    public static final String RESULT = "result";

    private static final boolean EnableEmptyKs = true;

    private PKPlaylistRequest playlist;

    private BEResponseListener responseListener;

    private String referrer;

    private class PKPlaylistRequest {

        public List<String> assetIds;

        public APIDefines.KalturaAssetType assetType;

        public APIDefines.AssetReferenceType assetReferenceType;

        public APIDefines.PlaybackContextType contextType;

        public List<String> formats;

        public List<String> mediaFileIds;

        public String protocol;

        public PKPlaylistRequest() {
        }

        public boolean hasFormats() {
            return formats != null && formats.size() > 0;
        }

        public boolean hasFiles() {
            return mediaFileIds != null && mediaFileIds.size() > 0;
        }
    }

    public PhoenixPlaylistProvider() {
        super(log.tag);
        this.playlist = new PKPlaylistRequest();
    }

    public PhoenixPlaylistProvider(final String baseUrl, final int partnerId, final String ks) {
        this();
        setSessionProvider(new SessionProvider() {
            @Override
            public String baseUrl() {
                return baseUrl;
            }

            @Override
            public void getSessionToken(OnCompletion<PrimitiveResult> completion) {
                completion.onComplete(new PrimitiveResult(ks));
            }

            @Override
            public int partnerId() {
                return partnerId;
            }
        });
    }

    /**
     * NOT MANDATORY! The referrer url, to fetch the data for.
     *
     * @param referrer - application referrer.
     * @return - instance of PhoenixMediaProvider
     */
    public PhoenixPlaylistProvider setReferrer(String referrer) {
        this.referrer = referrer;
        return this;
    }

    /**
     * MANDATORY! provides the baseUrl and the session token(ks) for the API calls.
     *
     * @param sessionProvider - {@link SessionProvider}
     * @return - instance of PhoenixMediaProvider
     */
    public PhoenixPlaylistProvider setSessionProvider(@NonNull SessionProvider sessionProvider) {
        this.sessionProvider = sessionProvider;
        return this;
    }

    /**
     * MANDATORY! the media asset id, to fetch the data for.
     *
     * @param assetIds - assetIds of requested entry.
     * @return - instance of PhoenixMediaProvider
     */
    public PhoenixPlaylistProvider setAssetIds(@NonNull List<String> assetIds) {
        this.playlist.assetIds = assetIds;
        return this;
    }

    /**
     * ESSENTIAL in EPG!! defines the playing  AssetReferenceType especially in case of epg
     * Defaults to - {@link APIDefines.KalturaAssetType#Media}
     *
     * @param assetReferenceType - can be one of the following types {@link APIDefines.AssetReferenceType}
     * @return - instance of PhoenixMediaProvider
     */
    public PhoenixPlaylistProvider setAssetReferenceType(@NonNull APIDefines.AssetReferenceType assetReferenceType) {
        this.playlist.assetReferenceType = assetReferenceType;
        return this;
    }

    /**
     * ESSENTIAL!! defines the playing asset group type
     * Defaults to - {@link APIDefines.KalturaAssetType#Media}
     *
     * @param assetType - can be one of the following types {@link APIDefines.KalturaAssetType}
     * @return - instance of PhoenixMediaProvider
     */
    public PhoenixPlaylistProvider setAssetType(@NonNull APIDefines.KalturaAssetType assetType) {
        this.playlist.assetType = assetType;
        return this;
    }

    /**
     * ESSENTIAL!! defines the playing context: Trailer, Catchup, Playback etc
     * Defaults to - {@link APIDefines.PlaybackContextType#Playback}
     *
     * @param contextType - can be one of the following types {@link APIDefines.PlaybackContextType}
     * @return - instance of PhoenixMediaProvider
     */
    public PhoenixPlaylistProvider setContextType(@NonNull APIDefines.PlaybackContextType contextType) {
        this.playlist.contextType = contextType;
        return this;
    }

    /**
     * OPTIONAL
     *
     * @param protocol - the desired protocol (http/https) for the playback sources
     *                 The default is null, which makes the provider filter by server protocol.
     * @return - instance of PhoenixMediaProvider
     */
    public PhoenixPlaylistProvider setProtocol(@NonNull @HttpProtocol String protocol) {
        this.playlist.protocol = protocol;
        return this;
    }

    /**
     * OPTIONAL
     * defines which of the sources to consider on {@link PKMediaEntry} creation.
     *
     * @param formats - 1 or more content format definition. can be: Hd, Sd, Download, Trailer etc
     * @return - instance of PhoenixMediaProvider
     */
    public PhoenixPlaylistProvider setFormats(@NonNull String... formats) {
        this.playlist.formats = new ArrayList<>(Arrays.asList(formats));
        return this;
    }


    /**
     * OPTIONAL - if not available all sources will be fetched
     * Provide a list of media files ids. will be used in the getPlaybackContext API request                                                                                                 .
     *
     * @param mediaFileIds - list of MediaFile ids to narrow sources fetching from API to
     *                     the specific files
     * @return - instance of PhoenixMediaProvider
     */
    public PhoenixPlaylistProvider setFileIds(@NonNull String... mediaFileIds) {
        this.playlist.mediaFileIds = new ArrayList<>(Arrays.asList(mediaFileIds));
        return this;
    }

    public PhoenixPlaylistProvider setResponseListener(BEResponseListener responseListener) {
        this.responseListener = responseListener;
        return this;
    }

    /**
     * OPTIONAL
     * Defaults to {@link APIOkRequestsExecutor} implementation.
     *
     * @param executor - executor
     * @return - instance of PhoenixMediaProvider
     */
    public PhoenixPlaylistProvider setRequestExecutor(@NonNull RequestQueue executor) {
        this.requestsExecutor = executor;
        return this;
    }


    protected Loader factorNewLoader(OnMediaLoadCompletion completion) {
        return new Loader(requestsExecutor, sessionProvider, playlist, completion);
    }

    /**
     * Checks for non empty value on the mandatory parameters.
     *
     * @return - error in case of at least 1 invalid mandatory parameter.
     */
    @Override
    protected ErrorElement validateParams() {

        if (playlist.assetIds == null || playlist.assetIds.isEmpty()) {
            return ErrorElement.BadRequestError.addMessage("Missing required parameter [assetIds]");
        }

        if (playlist.contextType == null) {
            playlist.contextType = APIDefines.PlaybackContextType.Playback;
        }

        if (playlist.assetType == null) {
            switch (playlist.contextType) {
                case Playback:
                case Trailer:
                    playlist.assetType = APIDefines.KalturaAssetType.Media;
                    break;

                case StartOver:
                case Catchup:
                    playlist.assetType = APIDefines.KalturaAssetType.Epg;
                    break;
            }
        }

        if (playlist.assetReferenceType == null) {
            switch (playlist.assetType) {
                case Media:
                    playlist.assetReferenceType = APIDefines.AssetReferenceType.Media;
                    break;
                case Epg:
                    playlist.assetReferenceType = APIDefines.AssetReferenceType.InternalEpg;
                    break;
            }
            // Or leave it as null.
        }

        return null;
    }


    class Loader extends BECallableLoader {

        private PKPlaylistRequest playlistRequest;


        public Loader(RequestQueue requestsExecutor, SessionProvider sessionProvider, PKPlaylistRequest playlistRequest, OnMediaLoadCompletion completion) {
            super(log.tag + "#Loader", requestsExecutor, sessionProvider, completion);

            this.playlistRequest = playlistRequest;

            log.v(loadId + ": construct new Loader");
        }

        @Override
        protected ErrorElement validateKs(String ks) { // enable anonymous session creation
            return EnableEmptyKs || !TextUtils.isEmpty(ks) ? null :
                    ErrorElement.BadRequestError.message(ErrorElement.BadRequestError + ": SessionProvider should provide a valid KS token");
        }


        private RequestBuilder getPlaylistRequest(String baseUrl, String ks, String assetId, APIDefines.AssetReferenceType assetReferenceType) {
            return AssetService.get(baseUrl, ks, assetId, assetReferenceType);
        }

        private RequestBuilder getRemoteRequest(String baseUrl, String ks, String referrer, PKPlaylistRequest playlistRequest) {

            String multiReqKs;

            MultiRequestBuilder builder = (MultiRequestBuilder) PhoenixService.getMultirequest(baseUrl, ks)
                    .tag("asset-play-data-multireq");

            if (TextUtils.isEmpty(ks)) {
                multiReqKs = "{1:result:ks}";
                builder.add(OttUserService.anonymousLogin(baseUrl, sessionProvider.partnerId(), null));
            } else {
                multiReqKs = ks;
            }

            if (playlistRequest.assetReferenceType != null) {
                for(String assetId : playlistRequest.assetIds)
                builder.add(getPlaylistRequest(baseUrl, multiReqKs, assetId, playlistRequest.assetReferenceType));
            }

            return builder;
        }

        /**
         * Builds and passes to the executor, the Asset info fetching request.
         *
         * @param ks - ks
         * @throws InterruptedException - {@link InterruptedException}
         */
        @Override
        protected void requestRemote(String ks) throws InterruptedException {
            final RequestBuilder requestBuilder = getRemoteRequest(getApiBaseUrl(), ks, referrer, playlistRequest)
                    .completion(response -> {
                        log.v(loadId + ": got response to [" + loadReq + "]");
                        loadReq = null;

                        try {
                            onAssetGetResponse(response, ks);

                        } catch (InterruptedException e) {
                            interrupted();
                        }
                    });

            synchronized (syncObject) {
                loadReq = requestQueue.queue(requestBuilder.build());
                log.d(loadId + ": request queued for execution [" + loadReq + "]");
            }

            if (!isCanceled()) {
                log.v(loadId + " set waitCompletion");
                waitCompletion();
            } else {
                log.v(loadId + " was canceled.");
            }
            log.v(loadId + ": requestRemote wait released");
        }

        private String getApiBaseUrl() {
            final String url = sessionProvider.baseUrl();
            return url.endsWith("/") ? url : url + "/";
        }

        /**
         * Parse and create a {@link PKMediaEntry} object from the API response.
         *
         * @param response - server response
         * @throws InterruptedException - {@link InterruptedException}
         */
        private void onAssetGetResponse(final ResponseElement response, String ks) throws InterruptedException {
            ErrorElement error = null;
            List<KalturaMediaAsset> kalturaMediaAssets = null;
            List<Map<String,String>> assetsMetadtaList = null;
            PKPlaylist pkPlaylist = null;

            if (isCanceled()) {
                log.v(loadId + ": i am canceled, exit response parsing ");
                return;
            }

            if (responseListener != null) {
                responseListener.onResponse(response);
            }

            if (!isValidResponse(response)) {
                return;
            }

            if (response.isSuccess()) {
                KalturaMediaAsset asset = null;

                try {
                    //**************************

                /* ways to parse the AssetInfo from response string:

                    1. <T> T PhoenixParser.parseObject: parse json string to a single object, according to a specific type - returns an object of the specific type
                            asset = PhoenixParser.parseObject(response.getResponse(), KalturaMediaAsset.class);

                    2. Object PhoenixParser.parse(String response, Class...types): parse json string according to 1 or more types (dynamic types array) - returns Object since can
                       be single or an array of objects. cast is needed, can be used for multiple response
                            asset = (KalturaMediaAsset) PhoenixParser.parse(response.getResponse(), KalturaMediaAsset.class);

                        in case of an error - the error will be passed over the returned object (should extend BaseResult) */

                    //*************************

                    log.d(loadId + ": parsing response  [" + Loader.this.toString() + "]");
                    /* 3. <T> T PhoenixParser.parse(String response): parse json string to an object of dynamically parsed type.
                       type defined by the value of "objectType" property provided in the response objects, if type wasn't found or in
                       case of error object in the response, will be parsed to BaseResult object (error if occurred will be accessible from this object)*/

                    BaseResult loginResult = null;
                    BaseResult playbackContextResult = null;
                    BaseResult assetGetResult = null;

                    Object parsedResponsesObject = PhoenixParser.parse(response.getResponse());
                    List<BaseResult> parsedResponses = new ArrayList<>();
                    if (parsedResponsesObject instanceof List) {
                        parsedResponses = (List<BaseResult>) parsedResponsesObject;
                    } else if (parsedResponsesObject instanceof BaseResult){
                        // Fix potential bug in BE that response will come in single object and not as List
                        parsedResponses.add((BaseResult) parsedResponsesObject);
                        loginResult = (BaseResult) parsedResponsesObject;
                    }

                    if (!parsedResponses.isEmpty() && parsedResponses.get(0) instanceof KalturaLoginSession) {
                        loginResult = parsedResponses.get(0);
                    }

                    if (loginResult != null && loginResult.error != null) {
                        error = ErrorElement.LoadError.message("failed to get responses on load requests");
                        completion.onComplete(Accessories.buildResult(null, error));
                        return;
                    }
                    if (loginResult != null) {
                        ks = ((KalturaLoginSession)loginResult).getKs();
                    }

                    int mediaAssetsStartIndex = 0;
                    if (loginResult != null && parsedResponses.size() > 1) {
                        mediaAssetsStartIndex = 1;
                    }

                    kalturaMediaAssets = new ArrayList<>();
                    assetsMetadtaList = new ArrayList<>();
                    for ( ; mediaAssetsStartIndex < parsedResponses.size()  ; mediaAssetsStartIndex++) {
                        if (parsedResponses.get(mediaAssetsStartIndex).error == null) {
                            KalturaMediaAsset kalturaMediaAsset = (KalturaMediaAsset) parsedResponses.get(mediaAssetsStartIndex);
                            Map<String, String> metadata = createOttMetadata(kalturaMediaAsset);
                            metadata.put("is360Content", String.valueOf(is360Supported(metadata)));
                            assetsMetadtaList.add(metadata);
                            kalturaMediaAssets.add(kalturaMediaAsset);
                        }
                    }

                    if (kalturaMediaAssets.isEmpty()) { // makes sure there are sources available for play
                        error = ErrorElement.LoadError.message("failed to get responses on load requests no medias available");
                        completion.onComplete(Accessories.buildResult(null, error));
                        return;
                    }

                } catch (JsonParseException | InvalidParameterException ex) {
                    error = ErrorElement.LoadError.message("failed parsing remote response: " + ex.getMessage());
                } catch (IndexOutOfBoundsException ex) {
                    error = GeneralError.message("responses list doesn't contain the expected responses number: " + ex.getMessage());
                }
            } else {
                error = response.getError() != null ? response.getError() : ErrorElement.LoadError;
            }

            log.i(loadId + ": load operation " + (isCanceled() ? "canceled" : "finished with " + (error == null ? "success" : "failure")));

            if (!isCanceled() && completion != null) {
                pkPlaylist = getPKPlaylist(ks, kalturaMediaAssets, assetsMetadtaList);
                completion.onComplete(Accessories.buildResult(pkPlaylist, error));
            }

            log.w(loadId + " media load finished, callback passed...notifyCompletion");
            notifyCompletion();

        }

        private PKPlaylist getPKPlaylist(String playlistKs, List<KalturaMediaAsset> entriesList, List<Map<String,String>> assetsMetadtaList) {
            List<PKPlaylistMedia> mediaArrayList = new ArrayList<>();

            int listIndex = 0;
            for (KalturaMediaAsset kalturaMediaEntry : entriesList) {
                if (kalturaMediaEntry == null || assetsMetadtaList == null) {
                    break;
                }

                if (kalturaMediaEntry.getMediaFiles() == null || kalturaMediaEntry.getMediaFiles().isEmpty() ||
                        kalturaMediaEntry.getImages() == null || kalturaMediaEntry.getImages().isEmpty())  {
                    continue;
                }
                mediaArrayList.add(new PKPlaylistMedia().
                        setId(String.valueOf(kalturaMediaEntry.getId())).
                        setName(kalturaMediaEntry.getName()).
                        setDescription(kalturaMediaEntry.getDescription()).
                        setType(PKMediaEntry.MediaEntryType.Unknown).
                        setMsDuration(kalturaMediaEntry.getMediaFiles().get(0).getDuration() * Consts.MILLISECONDS_MULTIPLIER).
                        setThumbnailUrl(kalturaMediaEntry.getImages().get(0).getUrl()).
                        setFlavorParamsIds(kalturaMediaEntry.getMediaFiles().get(0).getType()).
                        setTags(assetsMetadtaList.get(0).get("tags")).
                        setMetadata(assetsMetadtaList.get(listIndex)));
                listIndex++;
            }

            PKPlaylist playlist = new PKPlaylist().
                    setKs(playlistKs).
                    setId("1").
                    setName("Playlist").
                    setDescription("").
                    setThumbnailUrl("").
                    setDuration(0).
                    setPlaylistType(PKPlaylistType.Unknown).
                    setPlaylistMediaList(mediaArrayList);

            return playlist;
        }

        private boolean isValidResponse(ResponseElement response) {

            if (isErrorResponse(response)) {
                ErrorElement errorResponse = parseErrorRersponse(response);
                if (errorResponse == null) {
                    errorResponse = GeneralError;
                }
                if (!isCanceled() && completion != null) {
                    completion.onComplete(Accessories.buildResult(null, errorResponse));
                    notifyCompletion();
                }
                return false;
            }

            if (isAPIExceptionResponse(response)) {
                ErrorElement apiExceptionError = parseAPIExceptionError(response);
                if (apiExceptionError == null) {
                    apiExceptionError = GeneralError;
                }
                if (!isCanceled() && completion != null) {
                    completion.onComplete(Accessories.buildResult(null, apiExceptionError));
                    notifyCompletion();
                }
                return false;
            }
            return true;
        }


        private boolean isAPIExceptionResponse(ResponseElement response) {
            return response == null || (response.isSuccess() && response.getError() == null && response.getResponse() != null && response.getResponse().contains(KALTURA_API_EXCEPTION));
        }

        private boolean isErrorResponse(ResponseElement response) {
            return response == null || (!response.isSuccess() && response.getError() != null);
        }

        private ErrorElement parseErrorRersponse(ResponseElement response) {
            if (response != null) {
                return response.getError();
            }
            return null;
        }

        private ErrorElement parseAPIExceptionError(ResponseElement response) {

            if (response != null) {

                try {
                    JSONObject apiException = new JSONObject(response.getResponse());

                    if (apiException.has(RESULT)) {
                        String ottError = "OTTError";
                        if (apiException.get(RESULT) instanceof JSONObject) {

                            JSONObject result = (JSONObject) apiException.get(RESULT);
                            if (result != null && result.has(ERROR)) {
                                JSONObject error = (JSONObject) result.get(ERROR);
                                Map<String, String> errorMap = getAPIExceptionData(error);
                                if (errorMap != null) {
                                    return new ErrorElement(errorMap.get(MESSAGE), errorMap.get(CODE), errorMap.get(OBJECT_TYPE)).setName(ottError);
                                }
                            }
                        } else if (apiException.get(RESULT) instanceof JSONArray) {

                            JSONArray result = (JSONArray) apiException.get(RESULT);
                            for(int idx = 0 ; idx < result.length() ; idx++) {
                                JSONObject error = (JSONObject) result.get(idx);
                                if (error != null && error.has(ERROR)) {
                                    JSONObject resultIndexJsonObjectError = (JSONObject) error.get(ERROR);
                                    Map<String, String> errorMap = getAPIExceptionData(resultIndexJsonObjectError);
                                    if (errorMap != null) {
                                        return new ErrorElement(errorMap.get(MESSAGE), errorMap.get(CODE), errorMap.get(OBJECT_TYPE)).setName(ottError);
                                    }
                                }
                            }
                        }

                    }
                } catch (JSONException | NumberFormatException ex) {
                    log.e("parseAPIExceptionError Exception = " + ex.getMessage());
                }
            }

            return null;
        }

        private Map<String,String> getAPIExceptionData(JSONObject error) {

            try {

                if (error != null) {

                    Map<String,String> errorMap = new HashMap<>();

                    if (error.has(OBJECT_TYPE)) {
                        String objectType = error.getString(OBJECT_TYPE);
                        errorMap.put(OBJECT_TYPE, objectType);
                    }

                    if (error.has(CODE)) {
                        String errorCode = error.getString(CODE);
                        errorMap.put(CODE, errorCode);
                    }

                    if (error.has(MESSAGE)) {
                        String errorMessage = error.getString(MESSAGE);
                        errorMap.put(MESSAGE, errorMessage);
                    }

                    //log.d("Error objectType = " + objectType + " errorCode = " + errorCode + "errorMessage = " + errorMessage);
                    return errorMap;
                }
            } catch (JSONException | NumberFormatException ex) {
                log.e("getAPIExceptionData Exception = " + ex.getMessage());
            }

            return null;
        }

        private boolean isDvrLiveMedia() {
            return playlistRequest.assetType == APIDefines.KalturaAssetType.Epg && playlistRequest.contextType == APIDefines.PlaybackContextType.StartOver;
        }
    }

    private boolean is360Supported(Map<String, String> metadata) {
        return ("360".equals(metadata.get("tags")));
    }

    @NonNull
    private Map<String, String> createOttMetadata(KalturaMediaAsset kalturaMediaAsset) {
        Map<String, String> metadata = new HashMap<>();
        JsonObject tags = kalturaMediaAsset.getTags();
        for (Map.Entry<String, JsonElement> entry : tags.entrySet()) {
            for (Map.Entry<String, JsonElement> object : entry.getValue().getAsJsonObject().entrySet()) {
                if (object.getValue().isJsonArray()) {
                    JsonArray objectsArray = object.getValue().getAsJsonArray();
                    for (int i = 0; i < objectsArray.size(); i++) {
                        metadata.put(entry.getKey(), safeGetValue(objectsArray.get(i)));
                    }
                }
            }
        }

        JsonObject metas = kalturaMediaAsset.getMetas();
        if (metas != null) {
            for (Map.Entry<String, JsonElement> entry : metas.entrySet()) {
                metadata.put(entry.getKey(), safeGetValue(entry.getValue()));
            }
        }

        for (KalturaThumbnail image : kalturaMediaAsset.getImages()) {
            metadata.put(image.getWidth() + "X" + image.getHeight(), image.getUrl());
        }

        metadata.put("assetIds", String.valueOf(kalturaMediaAsset.getId()));
        if (!TextUtils.isEmpty(kalturaMediaAsset.getEntryId())) {
            metadata.put("entryId", kalturaMediaAsset.getEntryId());
        }
        if (kalturaMediaAsset.getName() != null) {
            metadata.put("name", kalturaMediaAsset.getName());
        }
        if (kalturaMediaAsset.getDescription() != null) {
            metadata.put("description", kalturaMediaAsset.getDescription());
        }

        metadata.put("assetType", playlist.assetType.value);
        if (isRecordingMediaEntry(kalturaMediaAsset)) {
            metadata.put("recordingId", ((KalturaRecordingAsset)kalturaMediaAsset).getRecordingId());
            metadata.put("recordingType", ((KalturaRecordingAsset)kalturaMediaAsset).getRecordingType().name());
        }
        return metadata;
    }

    private String safeGetValue(JsonElement value) {
        if (value == null || value.isJsonNull() || !value.isJsonObject()) {
            return null;
        }

        final JsonElement valueElement = value.getAsJsonObject().get("value");
        return (valueElement != null && !valueElement.isJsonNull()) ? valueElement.getAsString() : null;
    }

    private ErrorElement updateErrorElement(ResponseElement response, BaseResult loginResult, BaseResult playbackContextResult, BaseResult assetGetResult) {
        //error = ErrorElement.LoadError.message("failed to get multirequest responses on load request for asset "+playlist.assetIds);
        ErrorElement error;
        if (loginResult != null && loginResult.error != null) {
            error = PhoenixErrorHelper.getErrorElement(loginResult.error); // get predefined error if exists for this error code
        } else if (playbackContextResult != null && playbackContextResult.error != null) {
            error = PhoenixErrorHelper.getErrorElement(playbackContextResult.error); // get predefined error if exists for this error code
        } else if (assetGetResult != null && assetGetResult.error != null) {
            error = PhoenixErrorHelper.getErrorElement(assetGetResult.error); // get predefined error if exists for this error code
        } else {
            error = response != null && response.getError() != null ? response.getError() : ErrorElement.LoadError;
        }
        return error;
    }

    private boolean isLiveMediaEntry(KalturaMediaAsset kalturaMediaAsset) {
        if (kalturaMediaAsset == null) {
            return false;
        }

        String externalIdsStr = kalturaMediaAsset.getExternalIds();
        return (LIVE_ASSET_OBJECT_TYPE.equals(kalturaMediaAsset.getObjectType()) ||
                !TextUtils.isEmpty(externalIdsStr) && TextUtils.isDigitsOnly(externalIdsStr) && Long.valueOf(externalIdsStr) != 0) ||
                (playlist.assetType == APIDefines.KalturaAssetType.Epg && playlist.contextType == APIDefines.PlaybackContextType.StartOver);
    }

    private boolean isRecordingMediaEntry(KalturaMediaAsset kalturaMediaAsset) {
        return kalturaMediaAsset instanceof KalturaRecordingAsset;
    }

    static class ProviderParser {

        public static PKMediaEntry getMedia(String assetId, final List<String> sourcesFilter, ArrayList<KalturaPlaybackSource> playbackSources, boolean is360Content) {
            PKMediaEntry mediaEntry = new PKMediaEntry();
            if (is360Content) {
                mediaEntry.setIsVRMediaType(true);
            }

            mediaEntry.setId("" + assetId);
            mediaEntry.setName(null);

            // until the response will be delivered in the right order:
            playbackSourcesSort(sourcesFilter, playbackSources);

            ArrayList<PKMediaSource> sources = new ArrayList<>();

            long maxDuration = 0;

            if (playbackSources != null) {

                // if provided, only the "formats" matching MediaFiles should be parsed and added to the PKMediaEntry media sources
                for (KalturaPlaybackSource playbackSource : playbackSources) {

                    boolean inSourceFilter = sourcesFilter != null &&
                            (sourcesFilter.contains(playbackSource.getType()) ||
                                    sourcesFilter.contains(playbackSource.getId() + ""));

                    if (sourcesFilter != null && !inSourceFilter) { // if specific formats/fileIds were requested, only those will be added to the sources.
                        continue;
                    }

                    PKMediaFormat mediaFormat = FormatsHelper.getPKMediaFormat(playbackSource.getFormat(), playbackSource.hasDrmData());

                    if (mediaFormat == null) {
                        continue;
                    }

                    PKMediaSource pkMediaSource = new PKMediaSource()
                            .setId(playbackSource.getId() + "")
                            .setUrl(playbackSource.getUrl())
                            .setMediaFormat(mediaFormat);

                    List<KalturaDrmPlaybackPluginData> drmData = playbackSource.getDrmData();
                    if (drmData != null && !drmData.isEmpty()) {
                        if (!MediaProvidersUtils.isDRMSchemeValid(pkMediaSource, drmData)) {
                            continue;
                        }
                        MediaProvidersUtils.updateDrmParams(pkMediaSource, drmData);
                    }

                    sources.add(pkMediaSource);
                    maxDuration = Math.max(playbackSource.getDuration(), maxDuration);
                }
            }
            return mediaEntry.setDuration(maxDuration * Consts.MILLISECONDS_MULTIPLIER).setSources(sources).setMediaType(MediaTypeConverter.toMediaEntryType(""));
        }

        //TODO: check why we get all sources while we asked for 4 specific formats

        // needed to sort the playback source result to be in the same order as in the requested list.
        private static void playbackSourcesSort(final List<String> sourcesFilter, ArrayList<KalturaPlaybackSource> playbackSources) {
            Collections.sort(playbackSources, new Comparator<KalturaPlaybackSource>() {
                @Override
                public int compare(KalturaPlaybackSource o1, KalturaPlaybackSource o2) {

                    int valueIndex1 = -1;
                    int valueIndex2 = -1;
                    if (sourcesFilter != null) {
                        valueIndex1 = sourcesFilter.indexOf(o1.getType());
                        if (valueIndex1 == -1) {
                            valueIndex1 = sourcesFilter.indexOf(o1.getId() + "");
                            valueIndex2 = sourcesFilter.indexOf(o2.getId() + "");
                        } else {
                            valueIndex2 = sourcesFilter.indexOf(o2.getType());
                        }
                    }
                    return valueIndex1 - valueIndex2;
                }
            });
        }
    }

    static class MediaTypeConverter {

        public static PKMediaEntry.MediaEntryType toMediaEntryType(String mediaType) {
            switch (mediaType) {
                default:
                    return PKMediaEntry.MediaEntryType.Unknown;
            }
        }
    }

    @StringDef({HttpProtocol.Http, HttpProtocol.Https, HttpProtocol.All})
    @Retention(RetentionPolicy.SOURCE)
    public @interface HttpProtocol {
        String Http = "http";       // only http sources
        String Https = "https";     // only https sources
        String All = "all";         // do not filter by protocol
    }

}
