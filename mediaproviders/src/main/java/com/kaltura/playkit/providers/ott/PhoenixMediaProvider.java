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

import android.net.Uri;
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
import com.kaltura.playkit.providers.MediaProvidersUtils;
import com.kaltura.playkit.providers.api.base.model.KalturaDrmPlaybackPluginData;
import com.kaltura.playkit.providers.api.phoenix.APIDefines;
import com.kaltura.playkit.providers.api.phoenix.PhoenixErrorHelper;
import com.kaltura.playkit.providers.api.phoenix.PhoenixParser;
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
 * mandatory fields: assetId, assetType, contextType
 *
 *
 * */

public class PhoenixMediaProvider extends BEMediaProvider {

    private static final PKLog log = PKLog.get("PhoenixMediaProvider");

    private static String LIVE_ASSET_OBJECT_TYPE = "KalturaLiveAsset"; //Might be needed to support in KalturaProgramAsset for EPG

    public static final String KALTURA_API_EXCEPTION = "KalturaAPIException";
    public static final String ERROR = "error";
    public static final String OBJECT_TYPE = "objectType";
    public static final String CODE = "code";
    public static final String MESSAGE = "message";
    public static final String RESULT = "result";

    private static final boolean EnableEmptyKs = true;

    private MediaAsset mediaAsset;

    private BEResponseListener responseListener;

    private String referrer;

    private class MediaAsset {

        public String assetId;

        public APIDefines.KalturaAssetType assetType;

        public APIDefines.AssetReferenceType assetReferenceType;

        public APIDefines.PlaybackContextType contextType;

        public APIDefines.PKUrlType urlType;

        public List<String> formats;

        public List<String> mediaFileIds;

        public String protocol;

        public MediaAsset() {
        }

        public boolean hasFormats() {
            return formats != null && formats.size() > 0;
        }

        public boolean hasFiles() {
            return mediaFileIds != null && mediaFileIds.size() > 0;
        }
    }

    public PhoenixMediaProvider() {
        super(log.tag);
        this.mediaAsset = new MediaAsset();
    }

    public PhoenixMediaProvider(final String baseUrl, final int partnerId, final String ks) {
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
    public PhoenixMediaProvider setReferrer(String referrer) {
        this.referrer = referrer;
        return this;
    }

    /**
     * MANDATORY! provides the baseUrl and the session token(ks) for the API calls.
     *
     * @param sessionProvider - {@link SessionProvider}
     * @return - instance of PhoenixMediaProvider
     */
    public PhoenixMediaProvider setSessionProvider(@NonNull SessionProvider sessionProvider) {
        this.sessionProvider = sessionProvider;
        return this;
    }

    /**
     * MANDATORY! the media asset id, to fetch the data for.
     *
     * @param assetId - assetId of requested entry.
     * @return - instance of PhoenixMediaProvider
     */
    public PhoenixMediaProvider setAssetId(@NonNull String assetId) {
        this.mediaAsset.assetId = assetId;
        return this;
    }

    /**
     * ESSENTIAL in EPG!! defines the playing  AssetReferenceType especially in case of epg
     * Defaults to - {@link APIDefines.KalturaAssetType#Media}
     *
     * @param assetReferenceType - can be one of the following types {@link APIDefines.AssetReferenceType}
     * @return - instance of PhoenixMediaProvider
     */
    public PhoenixMediaProvider setAssetReferenceType(@NonNull APIDefines.AssetReferenceType assetReferenceType) {
        this.mediaAsset.assetReferenceType = assetReferenceType;
        return this;
    }

    /**
     * ESSENTIAL!! defines the playing asset group type
     * Defaults to - {@link APIDefines.KalturaAssetType#Media}
     *
     * @param assetType - can be one of the following types {@link APIDefines.KalturaAssetType}
     * @return - instance of PhoenixMediaProvider
     */
    public PhoenixMediaProvider setAssetType(@NonNull APIDefines.KalturaAssetType assetType) {
        this.mediaAsset.assetType = assetType;
        return this;
    }

    /**
     * ESSENTIAL!! defines the playing context: Trailer, Catchup, Playback etc
     * Defaults to - {@link APIDefines.PlaybackContextType#Playback}
     *
     * @param contextType - can be one of the following types {@link APIDefines.PlaybackContextType}
     * @return - instance of PhoenixMediaProvider
     */
    public PhoenixMediaProvider setContextType(@NonNull APIDefines.PlaybackContextType contextType) {
        this.mediaAsset.contextType = contextType;
        return this;
    }

    /**
     * OPTIONAL
     *
     * @param urlType - can be one of the following types {@link APIDefines.PKUrlType}
     * @return - instance of PhoenixMediaProvider
     */
    public PhoenixMediaProvider setPKUrlType(@NonNull APIDefines.PKUrlType urlType) {
        this.mediaAsset.urlType = urlType;
        return this;
    }

    /**
     * OPTIONAL
     *
     * @param protocol - the desired protocol (http/https) for the playback sources
     *                 The default is null, which makes the provider filter by server protocol.
     * @return - instance of PhoenixMediaProvider
     */
    public PhoenixMediaProvider setProtocol(@NonNull @HttpProtocol String protocol) {
        this.mediaAsset.protocol = protocol;
        return this;
    }

    /**
     * OPTIONAL
     * defines which of the sources to consider on {@link PKMediaEntry} creation.
     *
     * @param formats - 1 or more content format definition. can be: Hd, Sd, Download, Trailer etc
     * @return - instance of PhoenixMediaProvider
     */
    public PhoenixMediaProvider setFormats(@NonNull String... formats) {
        this.mediaAsset.formats = new ArrayList<>(Arrays.asList(formats));
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
    public PhoenixMediaProvider setFileIds(@NonNull String... mediaFileIds) {
        this.mediaAsset.mediaFileIds = new ArrayList<>(Arrays.asList(mediaFileIds));
        return this;
    }

    public PhoenixMediaProvider setResponseListener(BEResponseListener responseListener) {
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
    public PhoenixMediaProvider setRequestExecutor(@NonNull RequestQueue executor) {
        this.requestsExecutor = executor;
        return this;
    }

    protected Loader factorNewLoader(OnMediaLoadCompletion completion) {
        return new Loader(requestsExecutor, sessionProvider, mediaAsset, completion);
    }

    /**
     * Checks for non empty value on the mandatory parameters.
     *
     * @return - error in case of at least 1 invalid mandatory parameter.
     */
    @Override
    protected ErrorElement validateParams() {

        if (TextUtils.isEmpty(this.mediaAsset.assetId)) {
            return new ErrorElement(ErrorElement.BadRequestError.getName(), "Missing required parameter [assetId]", ErrorElement.ErrorCode.BadRequestErrorCode);
        }

        if (mediaAsset.contextType == null) {
            mediaAsset.contextType = APIDefines.PlaybackContextType.Playback;
        }

        if (mediaAsset.urlType == null) {
            mediaAsset.urlType = APIDefines.PKUrlType.PlayManifest;
        }

        if (mediaAsset.assetType == null) {
            switch (mediaAsset.contextType) {
                case Playback:
                case Trailer:
                    mediaAsset.assetType = APIDefines.KalturaAssetType.Media;
                    break;

                case StartOver:
                case Catchup:
                    mediaAsset.assetType = APIDefines.KalturaAssetType.Epg;
                    break;
            }
        }

        if (mediaAsset.assetReferenceType == null) {
            switch (mediaAsset.assetType) {
                case Media:
                    mediaAsset.assetReferenceType = APIDefines.AssetReferenceType.Media;
                    break;
                case Epg:
                    mediaAsset.assetReferenceType = APIDefines.AssetReferenceType.InternalEpg;
                    break;
            }
            // Or leave it as null.
        }

        return null;
    }


    class Loader extends BECallableLoader {

        private MediaAsset mediaAsset;


        public Loader(RequestQueue requestsExecutor, SessionProvider sessionProvider, MediaAsset mediaAsset, OnMediaLoadCompletion completion) {
            super(log.tag + "#Loader", requestsExecutor, sessionProvider, completion);

            this.mediaAsset = mediaAsset;

            log.v(loadId + ": construct new Loader");
        }

        @Override
        protected ErrorElement validateKs(String ks) { // enable anonymous session creation
            return EnableEmptyKs || !TextUtils.isEmpty(ks) ? null :
                    new ErrorElement(ErrorElement.BadRequestError.getName(), ErrorElement.BadRequestError + ": SessionProvider should provide a valid KS token", ErrorElement.ErrorCode.BadRequestErrorCode);
        }


        private RequestBuilder getPlaybackContextRequest(String baseUrl, String ks, String referrer, MediaAsset mediaAsset) {
            AssetService.KalturaPlaybackContextOptions contextOptions = new AssetService.KalturaPlaybackContextOptions(mediaAsset.contextType);
            if (mediaAsset.mediaFileIds != null) { // else - will fetch all available sources
                contextOptions.setMediaFileIds(mediaAsset.mediaFileIds);
            }

            if (mediaAsset.urlType != null) {
                contextOptions.setUrlType(mediaAsset.urlType);
            }

            // protocol will be added only if no protocol was give or http/https was set
            // for All no filter will be done via protocol and it will not be added to the request.
            if (mediaAsset.protocol == null) {
                contextOptions.setMediaProtocol(Uri.parse(baseUrl).getScheme());
            } else if (!HttpProtocol.All.equals(mediaAsset.protocol)) {
                contextOptions.setMediaProtocol(mediaAsset.protocol);
            }

            if (!TextUtils.isEmpty(referrer)) {
                contextOptions.setReferrer(referrer);
            }

            return AssetService.getPlaybackContext(baseUrl, ks, mediaAsset.assetId,
                    mediaAsset.assetType, contextOptions);
        }

        private RequestBuilder getMediaAssetRequest(String baseUrl, String ks, MediaAsset mediaAsset) {
            return AssetService.get(baseUrl, ks, mediaAsset.assetId, mediaAsset.assetReferenceType);
        }

        private RequestBuilder getRemoteRequest(String baseUrl, String ks, String referrer, MediaAsset mediaAsset) {

            String multiReqKs;

            MultiRequestBuilder builder = (MultiRequestBuilder) PhoenixService.getMultirequest(baseUrl, ks)
                    .tag("asset-play-data-multireq");

            if (TextUtils.isEmpty(ks)) {
                multiReqKs = "{1:result:ks}";
                builder.add(OttUserService.anonymousLogin(baseUrl, sessionProvider.partnerId(), null));
            } else {
                multiReqKs = ks;
            }

            builder.add(getPlaybackContextRequest(baseUrl, multiReqKs, referrer, mediaAsset));

            if (mediaAsset.assetReferenceType != null) {
                builder.add(getMediaAssetRequest(baseUrl, multiReqKs, mediaAsset));
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
            final RequestBuilder requestBuilder = getRemoteRequest(getApiBaseUrl(), ks, referrer, mediaAsset)
                    .completion(response -> {
                        log.v(loadId + ": got response to [" + loadReq + "]");
                        loadReq = null;

                        try {
                            onAssetGetResponse(response);

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
        private void onAssetGetResponse(final ResponseElement response) throws InterruptedException {
            ErrorElement error;
            PKMediaEntry mediaEntry = null;

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

                    if (parsedResponses.size() > 2) {
                        // position size -1 is asset get result size - 2 is playbackContext size - 3 is the login data
                        loginResult = parsedResponses.get(parsedResponses.size() - 3);
                    }

                    if (parsedResponses.size() > 1) {
                        // position size -1 is asset get result size - 2 is playbackContext size - 3 is the login data
                        playbackContextResult = parsedResponses.get(parsedResponses.size() - 2);
                        assetGetResult = parsedResponses.get(parsedResponses.size() - 1);
                    }

                    if ((parsedResponses.size() > 2 && (loginResult == null || loginResult.error != null)) || playbackContextResult == null || assetGetResult == null || playbackContextResult.error != null || assetGetResult.error != null) {
                        error = updateErrorElement(response, loginResult, playbackContextResult, assetGetResult);
                    } else {
                        KalturaPlaybackContext kalturaPlaybackContext = (KalturaPlaybackContext) playbackContextResult;
                        KalturaMediaAsset kalturaMediaAsset = (KalturaMediaAsset) assetGetResult;

                        Map<String, String> metadata = createOttMetadata(kalturaMediaAsset);
                        boolean is360Content = is360Supported(metadata);

                        error = kalturaPlaybackContext.hasError(); // check for error or unauthorized content

                        if (error != null) {
                            if (!isCanceled() && completion != null) {
                                completion.onComplete(Accessories.buildResult(null, error));
                            }
                            notifyCompletion();
                            return;
                        }

                        mediaEntry = ProviderParser.getMedia(mediaAsset.assetId,
                                mediaAsset.formats != null ? mediaAsset.formats : mediaAsset.mediaFileIds,
                                kalturaPlaybackContext.getSources(), is360Content);
                        mediaEntry.setMetadata(metadata);
                        mediaEntry.setName(kalturaMediaAsset.getName());
                        if (isDvrLiveMedia()) {
                            mediaEntry.setMediaType(PKMediaEntry.MediaEntryType.DvrLive);
                        } else if (isLiveMediaEntry(kalturaMediaAsset)) {
                            mediaEntry.setMediaType(PKMediaEntry.MediaEntryType.Live);
                        } else {
                            mediaEntry.setMediaType(PKMediaEntry.MediaEntryType.Vod);
                        }

                        if (mediaEntry.getSources().size() == 0) { // makes sure there are sources available for play
                            error = new ErrorElement(ErrorElement.NotFound.getName(), "Content can't be played due to lack of sources", ErrorElement.ErrorCode.NotFoundCode);
                        }
                    }
                } catch (JsonParseException | InvalidParameterException ex) {
                    error = new ErrorElement(ErrorElement.LoadError.getName(), "failed parsing remote response: " + ex.getMessage(), ErrorElement.ErrorCode.LoadErrorCode);
                } catch (IndexOutOfBoundsException ex) {
                    error = new ErrorElement(ErrorElement.GeneralError.getName(), "responses list doesn't contain the expected responses number: " + ex.getMessage(), ErrorElement.ErrorCode.GeneralErrorCode);
                }
            } else {
                error = response.getError() != null ? response.getError() : new ErrorElement(ErrorElement.LoadError.getName(), "error response in multirequest. response: " + response.getResponse(), ErrorElement.ErrorCode.LoadErrorCode);
            }

            log.i(loadId + ": load operation " + (isCanceled() ? "canceled" : "finished with " + (error == null ? "success" : "failure")));

            if (!isCanceled() && completion != null) {
                completion.onComplete(Accessories.buildResult(mediaEntry, error));
            }

            log.w(loadId + " media load finished, callback passed...notifyCompletion");
            notifyCompletion();

        }

        private boolean isValidResponse(ResponseElement response) {

            if (isErrorResponse(response)) {
                ErrorElement errorResponse = parseErrorRersponse(response);
                if (errorResponse == null) {
                    errorResponse = new ErrorElement(ErrorElement.GeneralError.getName(), "multirequest response is null", ErrorElement.ErrorCode.GeneralErrorCode);
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
                    apiExceptionError = new ErrorElement(ErrorElement.GeneralError.getName(), "multirequest KalturaAPIException", ErrorElement.ErrorCode.GeneralErrorCode);
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
            return mediaAsset.assetType == APIDefines.KalturaAssetType.Epg && mediaAsset.contextType == APIDefines.PlaybackContextType.StartOver;
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

        metadata.put("assetId", String.valueOf(kalturaMediaAsset.getId()));
        if (!TextUtils.isEmpty(kalturaMediaAsset.getEntryId())) {
            metadata.put("entryId", kalturaMediaAsset.getEntryId());
        }
        if (kalturaMediaAsset.getName() != null) {
            metadata.put("name", kalturaMediaAsset.getName());
        }
        if (kalturaMediaAsset.getDescription() != null) {
            metadata.put("description", kalturaMediaAsset.getDescription());
        }

        metadata.put("assetType", mediaAsset.assetType.value);
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
        //error = ErrorElement.LoadError.message("failed to get multirequest responses on load request for asset "+mediaAsset.assetId);
        ErrorElement error;
        if (loginResult != null && loginResult.error != null) {
            error = PhoenixErrorHelper.getErrorElement(loginResult.error); // get predefined error if exists for this error code
        } else if (playbackContextResult != null && playbackContextResult.error != null) {
            error = PhoenixErrorHelper.getErrorElement(playbackContextResult.error); // get predefined error if exists for this error code
        } else if (assetGetResult != null && assetGetResult.error != null) {
            error = PhoenixErrorHelper.getErrorElement(assetGetResult.error); // get predefined error if exists for this error code
        } else {
            error = response != null && response.getError() != null ? response.getError() : new ErrorElement(ErrorElement.LoadError.getName(), "either response is null or response.getError() is null", ErrorElement.ErrorCode.LoadErrorCode);
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
                (mediaAsset.assetType == APIDefines.KalturaAssetType.Epg && mediaAsset.contextType == APIDefines.PlaybackContextType.StartOver);
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
