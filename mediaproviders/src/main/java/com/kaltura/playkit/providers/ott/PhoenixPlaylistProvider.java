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

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.kaltura.netkit.connect.executor.APIOkRequestsExecutor;
import com.kaltura.netkit.connect.executor.RequestQueue;
import com.kaltura.netkit.connect.request.MultiRequestBuilder;
import com.kaltura.netkit.connect.request.RequestBuilder;
import com.kaltura.netkit.connect.response.BaseResult;

import com.kaltura.netkit.connect.response.ResponseElement;
import com.kaltura.netkit.utils.Accessories;
import com.kaltura.netkit.utils.ErrorElement;

import com.kaltura.netkit.utils.SessionProvider;
import com.kaltura.playkit.PKLog;
import com.kaltura.playkit.PKMediaEntry;
import com.kaltura.playkit.PKPlaylist;
import com.kaltura.playkit.PKPlaylistMedia;

import com.kaltura.playkit.providers.PlaylistMetadata;
import com.kaltura.playkit.providers.api.SimpleSessionProvider;
import com.kaltura.playkit.providers.api.phoenix.APIDefines;
import com.kaltura.playkit.providers.api.phoenix.PhoenixErrorHelper;
import com.kaltura.playkit.providers.api.phoenix.PhoenixParser;
import com.kaltura.playkit.providers.api.phoenix.model.KalturaLoginSession;
import com.kaltura.playkit.providers.api.phoenix.model.KalturaMediaAsset;
import com.kaltura.playkit.providers.api.phoenix.model.KalturaRecordingAsset;
import com.kaltura.playkit.providers.api.phoenix.model.KalturaThumbnail;
import com.kaltura.playkit.providers.api.phoenix.services.AssetService;
import com.kaltura.playkit.providers.api.phoenix.services.OttUserService;
import com.kaltura.playkit.providers.api.phoenix.services.PhoenixService;
import com.kaltura.playkit.providers.base.BECallableLoader;
import com.kaltura.playkit.providers.base.BEPlaylistProvider;
import com.kaltura.playkit.providers.base.BEResponseListener;
import com.kaltura.playkit.providers.base.OnPlaylistLoadCompletion;
import com.kaltura.playkit.utils.Consts;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.kaltura.netkit.utils.ErrorElement.GeneralError;

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

public class PhoenixPlaylistProvider extends BEPlaylistProvider {

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

        public PlaylistMetadata playlistMetadata;

        public List<OTTMediaAsset> mediaAssets;

        public PKPlaylistRequest() { }
    }

    public PhoenixPlaylistProvider() {
        super(log.tag);
        this.playlist = new PKPlaylistRequest();
    }

    public PhoenixPlaylistProvider(final String baseUrl, final int partnerId, final String ks) {
        this();
        setSessionProvider(new SimpleSessionProvider(baseUrl, partnerId, ks));
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
     * MANDATORY! the playlist metadata and the media assets to fetch the data for.
     *
     * @param mediaAssets - assets configuration of requested entry.
     * @param   - assets configuration of requested entry.
     * @return - instance of PhoenixMediaProvider
     */
    public PhoenixPlaylistProvider setPlaylistParams(@NonNull PlaylistMetadata playlistMetadata, @NonNull List<OTTMediaAsset> mediaAssets) {
        if (playlist != null) {
            playlist.playlistMetadata = playlistMetadata;
            playlist.mediaAssets = mediaAssets;
        }
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


    protected Loader factorNewLoader(OnPlaylistLoadCompletion completion) {
        return new Loader(requestsExecutor, sessionProvider, playlist, completion);
    }

    /**
     * Checks for non empty value on the mandatory parameters.
     *
     * @return - error in case of at least 1 invalid mandatory parameter.
     */
    @Override
    protected ErrorElement validateParams() {

        if (playlist.mediaAssets == null || playlist.mediaAssets.isEmpty()) {
            return ErrorElement.BadRequestError.addMessage("Missing required parameter [assetIds]");
        }

        return null;
    }


    class Loader extends BECallableLoader {

        private PKPlaylistRequest playlistRequest;


        public Loader(RequestQueue requestsExecutor, SessionProvider sessionProvider, PKPlaylistRequest playlistRequest, OnPlaylistLoadCompletion completion) {
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

            for(OTTMediaAsset mediaAsset : playlistRequest.mediaAssets) {
                builder.add(getPlaylistRequest(baseUrl, multiReqKs, mediaAsset.assetId, mediaAsset.assetReferenceType != null ? mediaAsset.assetReferenceType : APIDefines.AssetReferenceType.Media));

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
            
            if (response.isSuccess()) {
                KalturaMediaAsset asset = null;

                try {

                    log.d(loadId + ": parsing response  [" + Loader.this.toString() + "]");
                    BaseResult loginResult = null;

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
                    } else {
                        boolean allErrors = true;
                        for (BaseResult baseResult : parsedResponses) {
                            if (baseResult.error == null) {
                                allErrors = false;
                                break;
                            }
                        }
                        if (allErrors) {
                            completion.onComplete(Accessories.buildResult(null, parsedResponses.get(0).error));
                            return;
                        }
                    }
//                    if (loginResult != null) {
//                         ks = ((KalturaLoginSession)loginResult).getKs();
//                    }

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
                        } else {
                            kalturaMediaAssets.add(null);
                            assetsMetadtaList.add(null);
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
            if (entriesList == null || assetsMetadtaList == null) {
                return null;
            }

            List<PKPlaylistMedia> mediaList = new ArrayList<>();

            int listIndex = 0;
            for (KalturaMediaAsset kalturaMediaEntry : entriesList) {
                if (kalturaMediaEntry == null || kalturaMediaEntry.getMediaFiles() == null || kalturaMediaEntry.getMediaFiles().isEmpty() ||
                        kalturaMediaEntry.getImages() == null || kalturaMediaEntry.getImages().isEmpty()) {
                    mediaList.add(null);
                    listIndex++;
                    continue;
                }

                if (kalturaMediaEntry.getMediaFiles().get(0) != null) {
                    mediaList.add(new PKPlaylistMedia().
                            setMediaIndex(listIndex).
                            setId(String.valueOf(kalturaMediaEntry.getId())).
                            setName(kalturaMediaEntry.getName()).
                            setDescription(kalturaMediaEntry.getDescription()).
                            setType(getMediaEntryType(listIndex,kalturaMediaEntry )).
                            setMsDuration(kalturaMediaEntry.getMediaFiles().get(0).getDuration() * Consts.MILLISECONDS_MULTIPLIER).
                            setThumbnailUrl(kalturaMediaEntry.getImages().get(0).getUrl()).
                            setTags(assetsMetadtaList.get(listIndex).get("tags")).
                            setMetadata(assetsMetadtaList.get(listIndex)));
                }
                listIndex++;
            }

            if (playlistRequest.playlistMetadata == null) {
                playlistRequest.playlistMetadata = new PlaylistMetadata();
            }

            PKPlaylist playlist = new PKPlaylist().
                    setKs(playlistKs).
                    setId(playlistRequest.playlistMetadata.getId()).
                    setName(playlistRequest.playlistMetadata.getName()).
                    setDescription(playlistRequest.playlistMetadata.getDescription()).
                    setThumbnailUrl(playlistRequest.playlistMetadata.getThumbnailUrl()).
                    setMediaList(mediaList);

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

    private PKMediaEntry.MediaEntryType getMediaEntryType(int index, KalturaMediaAsset kalturaMediaAsset) {
        PKMediaEntry.MediaEntryType mediaEntryType = PKMediaEntry.MediaEntryType.Vod;
        if (isDvrLiveMedia(index)) {
            mediaEntryType = PKMediaEntry.MediaEntryType.DvrLive;
        } else if (isLiveMediaEntry(kalturaMediaAsset)) {
            mediaEntryType = PKMediaEntry.MediaEntryType.Live;
        }
        return mediaEntryType;
    }

    private boolean isLiveMediaEntry(KalturaMediaAsset kalturaMediaAsset) {
        if (kalturaMediaAsset == null) {
            return false;
        }

        String externalIdsStr = kalturaMediaAsset.getExternalIds();
        return (LIVE_ASSET_OBJECT_TYPE.equals(kalturaMediaAsset.getObjectType()) ||
                !TextUtils.isEmpty(externalIdsStr) && TextUtils.isDigitsOnly(externalIdsStr) && Long.valueOf(externalIdsStr) != 0);
    }

    private boolean isDvrLiveMedia(int index) {
        if (playlist != null && playlist.mediaAssets != null && !playlist.mediaAssets.isEmpty() && index >= 0 && index < playlist.mediaAssets.size() && playlist.mediaAssets.get(index) != null) {
            return playlist.mediaAssets.get(index).assetType == APIDefines.KalturaAssetType.Epg && playlist.mediaAssets.get(index).contextType == APIDefines.PlaybackContextType.StartOver;
        }
        return false;
    }

    private boolean isRecordingMediaEntry(KalturaMediaAsset kalturaMediaAsset) {
        return kalturaMediaAsset instanceof KalturaRecordingAsset;
    }
}
