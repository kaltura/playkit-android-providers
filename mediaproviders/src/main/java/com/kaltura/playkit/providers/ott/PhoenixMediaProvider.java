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

import com.google.gson.JsonParseException;
import com.kaltura.netkit.connect.executor.APIOkRequestsExecutor;
import com.kaltura.netkit.connect.executor.RequestQueue;
import com.kaltura.netkit.connect.request.MultiRequestBuilder;
import com.kaltura.netkit.connect.request.RequestBuilder;
import com.kaltura.netkit.connect.response.BaseResult;
import com.kaltura.netkit.connect.response.ResponseElement;
import com.kaltura.netkit.connect.response.ResultElement;
import com.kaltura.netkit.utils.Accessories;
import com.kaltura.netkit.utils.ErrorElement;
import com.kaltura.netkit.utils.OnCompletion;
import com.kaltura.netkit.utils.SessionProvider;
import com.kaltura.playkit.PKLog;
import com.kaltura.playkit.PKMediaEntry;
import com.kaltura.playkit.PKMediaFormat;
import com.kaltura.playkit.PKMediaSource;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.kaltura.playkit.providers.MediaEntryProvider;
import com.kaltura.playkit.providers.api.SimpleSessionProvider;
import com.kaltura.playkit.providers.api.base.model.KalturaDrmPlaybackPluginData;
import com.kaltura.playkit.providers.api.phoenix.APIDefines;
import com.kaltura.playkit.providers.api.phoenix.PhoenixParser;
import com.kaltura.playkit.providers.api.phoenix.model.KalturaMediaAsset;
import com.kaltura.playkit.providers.api.phoenix.model.KalturaPlaybackContext;
import com.kaltura.playkit.providers.api.phoenix.model.KalturaPlaybackSource;
import com.kaltura.playkit.providers.api.phoenix.services.AssetService;
import com.kaltura.playkit.providers.api.phoenix.services.OttUserService;
import com.kaltura.playkit.providers.api.phoenix.services.PhoenixService;
import com.kaltura.playkit.providers.base.BEBaseProvider;
import com.kaltura.playkit.providers.base.BECallableLoader;
import com.kaltura.playkit.providers.base.BEResponseListener;
import com.kaltura.playkit.providers.base.FormatsHelper;
import com.kaltura.playkit.providers.base.OnMediaLoadCompletion;
import com.kaltura.playkit.providers.ott.PhoenixProviderUtils.MediaTypeConverter;

import com.kaltura.playkit.utils.Consts;

import static com.kaltura.playkit.providers.MediaProvidersUtils.buildBadRequestErrorElement;
import static com.kaltura.playkit.providers.MediaProvidersUtils.buildGeneralErrorElement;
import static com.kaltura.playkit.providers.MediaProvidersUtils.buildLoadErrorElement;
import static com.kaltura.playkit.providers.MediaProvidersUtils.buildNotFoundlErrorElement;
import static com.kaltura.playkit.providers.MediaProvidersUtils.isDRMSchemeValid;
import static com.kaltura.playkit.providers.MediaProvidersUtils.updateDrmParams;
import static com.kaltura.playkit.providers.ott.PhoenixProviderUtils.createOttMetadata;
import static com.kaltura.playkit.providers.ott.PhoenixProviderUtils.is360Supported;
import static com.kaltura.playkit.providers.ott.PhoenixProviderUtils.isAPIExceptionResponse;
import static com.kaltura.playkit.providers.ott.PhoenixProviderUtils.isLiveMediaEntry;
import static com.kaltura.playkit.providers.ott.PhoenixProviderUtils.isDvrLiveMediaEntry;
import static com.kaltura.playkit.providers.ott.PhoenixProviderUtils.parseAPIExceptionError;
import static com.kaltura.playkit.providers.ott.PhoenixProviderUtils.parseErrorRersponse;
import static com.kaltura.playkit.providers.ott.PhoenixProviderUtils.updateErrorElement;


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

public class PhoenixMediaProvider extends BEBaseProvider<PKMediaEntry> implements MediaEntryProvider {

    private static final PKLog log = PKLog.get("PhoenixMediaProvider");

    private static final boolean EnableEmptyKs = true;

    private OTTMediaAsset mediaAsset;

    private BEResponseListener responseListener;

    public PhoenixMediaProvider() {
        super(log.tag);
        this.mediaAsset = new OTTMediaAsset();
    }

    public PhoenixMediaProvider(final String baseUrl, final int partnerId, final String ks) {
        this();
        setSessionProvider(new SimpleSessionProvider(baseUrl, partnerId, ks));
    }

    public PhoenixMediaProvider(final String baseUrl, final int partnerId, final OTTMediaAsset mediaAsset) {
        this(baseUrl, partnerId, mediaAsset.getKs());
        this.mediaAsset = mediaAsset;
    }

    /**
     * NOT MANDATORY! The referrer url, to fetch the data for.
     *
     * @param referrer - application referrer.
     * @return - instance of PhoenixMediaProvider
     */
    public PhoenixMediaProvider setReferrer(String referrer) {
        mediaAsset.setReferrer(referrer);
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
     * @param urlType - can be one of the following types {@link APIDefines.KalturaUrlType}
     * @return - instance of PhoenixMediaProvider
     */
    public PhoenixMediaProvider setPKUrlType(@NonNull APIDefines.KalturaUrlType urlType) {
        this.mediaAsset.urlType = urlType;
        return this;
    }

    /**
     * OPTIONAL
     *
     * @param streamerType - can be one of the following types {@link APIDefines.KalturaStreamerType}
     * @return - instance of PhoenixMediaProvider
     */
    public PhoenixMediaProvider setPKStreamerType(@NonNull APIDefines.KalturaStreamerType streamerType) {
        this.mediaAsset.streamerType = streamerType;
        return this;
    }

    /**
     * OPTIONAL Force the epgId
     *
     * @param epgId - used for live media only, epgId is a specific program id in that channel
     * @return - instance of PhoenixMediaProvider
     */
    public PhoenixMediaProvider setEpgId(@NonNull String epgId) {
        this.mediaAsset.epgId = epgId;
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

    /**
     * OPTIONAL - if not available will not be used
     * Provide a Map<String,String> for providers adapter data                                                                                                 .
     *
     * @param adapterData - map of adapter key and value
     * @return - instance of PhoenixMediaProvider
     */
    public PhoenixMediaProvider setAdapterData(@NonNull Map<String,String> adapterData) {
        this.mediaAsset.adapterData = adapterData;
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

    protected Loader createNewLoader(OnCompletion<ResultElement<PKMediaEntry>> completion) {
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
            return buildBadRequestErrorElement("Missing required parameter [assetId]");
        }

        if (mediaAsset.contextType == null) {
            mediaAsset.contextType = APIDefines.PlaybackContextType.Playback;
        }

        if (mediaAsset.urlType == null) {
            mediaAsset.urlType = APIDefines.KalturaUrlType.PlayManifest;
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

    @Override
    public void load(OnMediaLoadCompletion completion) {
        load((OnCompletion<ResultElement<PKMediaEntry>>)completion);
    }

    class Loader extends BECallableLoader {

        private OTTMediaAsset mediaAsset;


        public Loader(RequestQueue requestsExecutor, SessionProvider sessionProvider, OTTMediaAsset mediaAsset, OnCompletion<ResultElement<PKMediaEntry>> completion) {
            super(log.tag + "#Loader", requestsExecutor, sessionProvider, completion);

            this.mediaAsset = mediaAsset;

            log.v(loadId + ": construct new Loader");
        }

        @Override
        protected ErrorElement validateKs(String ks) { // enable anonymous session creation
            return EnableEmptyKs || !TextUtils.isEmpty(ks) ? null :
                    buildBadRequestErrorElement(ErrorElement.BadRequestError + ": SessionProvider should provide a valid KS token");
        }

        private RequestBuilder getPlaybackContextRequest(String baseUrl, String ks, OTTMediaAsset mediaAsset) {
            AssetService.KalturaPlaybackContextOptions contextOptions = new AssetService.KalturaPlaybackContextOptions(mediaAsset.contextType);
            if (mediaAsset.hasFileIds()) { // else - will fetch all available sources
                contextOptions.setMediaFileIds(mediaAsset.mediaFileIds);
            }

            if (mediaAsset.urlType != null) {
                contextOptions.setUrlType(mediaAsset.urlType);
            }

            if (mediaAsset.streamerType != null) {
                contextOptions.setStreamerType(mediaAsset.streamerType);
            }
            
            if (mediaAsset.hasAdapterData()) {
                contextOptions.setAdapterData(mediaAsset.adapterData);
            }

            // protocol will be added only if no protocol was give or http/https was set
            // for All no filter will be done via protocol and it will not be added to the request.
            if (mediaAsset.protocol == null) {
                contextOptions.setMediaProtocol(Uri.parse(baseUrl).getScheme());
            } else if (!HttpProtocol.All.equals(mediaAsset.protocol)) {
                contextOptions.setMediaProtocol(mediaAsset.protocol);
            }

            if (!TextUtils.isEmpty(mediaAsset.getReferrer())) {
                contextOptions.setReferrer(mediaAsset.getReferrer());
            }

            return AssetService.getPlaybackContext(baseUrl, ks, mediaAsset.assetId,
                    mediaAsset.assetType, contextOptions);
        }

        private RequestBuilder getMediaAssetRequest(String baseUrl, String ks, OTTMediaAsset mediaAsset) {
            return AssetService.get(baseUrl, ks, mediaAsset.assetId, mediaAsset.assetReferenceType);
        }

        private RequestBuilder getRemoteRequest(String baseUrl, String ks, OTTMediaAsset mediaAsset) {

            String multiReqKs;

            MultiRequestBuilder builder = (MultiRequestBuilder) PhoenixService.getMultirequest(baseUrl, ks)
                    .tag("asset-play-data-multireq");

            if (TextUtils.isEmpty(ks)) {
                multiReqKs = "{1:result:ks}";
                builder.add(OttUserService.anonymousLogin(baseUrl, sessionProvider.partnerId(), null));
            } else {
                multiReqKs = ks;
            }

            builder.add(getPlaybackContextRequest(baseUrl, multiReqKs, mediaAsset));

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
            final RequestBuilder requestBuilder = getRemoteRequest(getApiBaseUrl(), ks, mediaAsset)
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

                        Map<String, String> metadata = createOttMetadata(kalturaMediaAsset, mediaAsset);
                        boolean is360Content = is360Supported(metadata);
                        boolean isMulticastContent = (mediaAsset.streamerType == APIDefines.KalturaStreamerType.Multicast);

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
                                kalturaPlaybackContext.getSources(), is360Content, isMulticastContent);
                        mediaEntry.setMetadata(metadata);
                        mediaEntry.setName(kalturaMediaAsset.getName());
                        if (isDvrLiveMediaEntry(kalturaMediaAsset, mediaAsset)) {
                            mediaEntry.setMediaType(PKMediaEntry.MediaEntryType.DvrLive);
                        } else if (isLiveMediaEntry(kalturaMediaAsset)) {
                            mediaEntry.setMediaType(PKMediaEntry.MediaEntryType.Live);
                        } else {
                            mediaEntry.setMediaType(PKMediaEntry.MediaEntryType.Vod);
                        }

                        if (mediaEntry.getSources().size() == 0) { // makes sure there are sources available for play
                            error = buildNotFoundlErrorElement("Content can't be played due to lack of sources");
                        }
                    }
                } catch (JsonParseException | InvalidParameterException ex) {
                    error = buildLoadErrorElement("failed parsing remote response: " + ex.getMessage());
                } catch (IndexOutOfBoundsException ex) {
                    error = buildGeneralErrorElement("responses list doesn't contain the expected responses number: " + ex.getMessage());
                }
            } else {
                error = response.getError() != null ? response.getError() : buildLoadErrorElement("error response in multirequest. response: " + response.getResponse());
            }

            log.i(loadId + ": load operation " + (isCanceled() ? "canceled" : "finished with " + (error == null ? "success" : "failure")));

            if (!isCanceled() && completion != null) {
                completion.onComplete(Accessories.buildResult(mediaEntry, error));
            }

            log.v(loadId + " media load finished, callback passed...notifyCompletion");
            notifyCompletion();

        }

        private boolean isValidResponse(ResponseElement response) {

            if (PhoenixProviderUtils.isErrorResponse(response)) {
                ErrorElement errorResponse = parseErrorRersponse(response);
                if (errorResponse == null) {
                    errorResponse = buildGeneralErrorElement("multirequest response is null");
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
                    apiExceptionError = buildGeneralErrorElement("multirequest KalturaAPIException");
                }
                if (!isCanceled() && completion != null) {
                    completion.onComplete(Accessories.buildResult(null, apiExceptionError));
                    notifyCompletion();
                }
                return false;
            }
            return true;
        }
    }

    static class ProviderParser {

        public static PKMediaEntry getMedia(String assetId, final List<String> sourcesFilter, ArrayList<KalturaPlaybackSource> playbackSources, boolean is360Content, boolean isMulticastContent) {
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

                    if (isMulticastContent) {
                        mediaFormat = PKMediaFormat.udp;
                    }

                    if (mediaFormat == null) {
                        continue;
                    }

                    PKMediaSource pkMediaSource = new PKMediaSource()
                            .setId(playbackSource.getId() + "")
                            .setUrl(playbackSource.getUrl())
                            .setMediaFormat(mediaFormat);

                    List<KalturaDrmPlaybackPluginData> drmData = playbackSource.getDrmData();
                    if (drmData != null && !drmData.isEmpty()) {
                        if (!isDRMSchemeValid(pkMediaSource, drmData)) {
                            continue;
                        }
                        updateDrmParams(pkMediaSource, drmData);
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
            Collections.sort(playbackSources, (o1, o2) -> {

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
            });
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
