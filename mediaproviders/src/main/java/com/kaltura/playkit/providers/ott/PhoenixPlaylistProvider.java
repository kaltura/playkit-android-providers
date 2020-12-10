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
import com.kaltura.playkit.PKPlaylist;
import com.kaltura.playkit.PKPlaylistMedia;

import com.kaltura.playkit.providers.PlaylistMetadata;
import com.kaltura.playkit.providers.PlaylistProvider;
import com.kaltura.playkit.providers.api.SimpleSessionProvider;
import com.kaltura.playkit.providers.api.phoenix.APIDefines;
import com.kaltura.playkit.providers.api.phoenix.PhoenixParser;
import com.kaltura.playkit.providers.api.phoenix.model.KalturaLoginSession;
import com.kaltura.playkit.providers.api.phoenix.model.KalturaMediaAsset;
import com.kaltura.playkit.providers.api.phoenix.services.AssetService;
import com.kaltura.playkit.providers.api.phoenix.services.OttUserService;
import com.kaltura.playkit.providers.api.phoenix.services.PhoenixService;
import com.kaltura.playkit.providers.base.BEBaseProvider;
import com.kaltura.playkit.providers.base.BECallableLoader;
import com.kaltura.playkit.providers.base.BEResponseListener;
import com.kaltura.playkit.providers.base.OnPlaylistLoadCompletion;
import com.kaltura.playkit.utils.Consts;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.kaltura.netkit.utils.ErrorElement.GeneralError;
import static com.kaltura.playkit.providers.ott.PhoenixProviderUtils.createOttMetadata;
import static com.kaltura.playkit.providers.ott.PhoenixProviderUtils.is360Supported;
import static com.kaltura.playkit.providers.ott.PhoenixProviderUtils.isLiveMediaEntry;

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

public class PhoenixPlaylistProvider extends BEBaseProvider<PKPlaylist> implements PlaylistProvider {

    private static final PKLog log = PKLog.get("PhoenixPlaylistProvider");

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

    @Override
    protected Loader createNewLoader(OnCompletion<ResultElement<PKPlaylist>> completion) {
        return new Loader(requestsExecutor, sessionProvider, playlist, completion);
    }

    @Override
    public void load(OnPlaylistLoadCompletion completion) {
        load((OnCompletion<ResultElement<PKPlaylist>>)completion);
    }

    class Loader extends BECallableLoader {

        private PKPlaylistRequest playlistRequest;

        public Loader(RequestQueue requestsExecutor, SessionProvider sessionProvider, PKPlaylistRequest playlistRequest, OnCompletion<ResultElement<PKPlaylist>> completion) {
            super(log.tag + "#Loader", requestsExecutor, sessionProvider, completion);

            this.playlistRequest = playlistRequest;

            log.v(loadId + ": construct new Loader");
        }

        @Override
        protected ErrorElement validateKs(String ks) { // enable anonymous session creation
            return EnableEmptyKs || !TextUtils.isEmpty(ks) ? null :
                    ErrorElement.BadRequestError.message(ErrorElement.BadRequestError + ": SessionProvider should provide a valid KS token");
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

            for (OTTMediaAsset mediaAsset : playlistRequest.mediaAssets) {
                String requestKS = TextUtils.isEmpty(mediaAsset.getKs()) ? multiReqKs : mediaAsset.getKs();
                APIDefines.AssetReferenceType assetReferenceType = mediaAsset.assetReferenceType != null ? mediaAsset.assetReferenceType : APIDefines.AssetReferenceType.Media;
                builder.add((RequestBuilder) AssetService.get(baseUrl, requestKS, mediaAsset.assetId, assetReferenceType));
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
         * Parse and create a {@link PKPlaylist} object from the API response.
         *
         * @param response - server response
         * @throws InterruptedException - {@link InterruptedException}
         */
        private void onAssetGetResponse(final ResponseElement response, String ks) throws InterruptedException {
            ErrorElement error = null;
            List<KalturaMediaAsset> kalturaMediaAssets = null;
            List<Map<String, String>> assetsMetadtaList = null;
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
                    } else if (parsedResponsesObject instanceof BaseResult) {
                        // Fix potential bug in BE that response will come in single object and not as List
                        parsedResponses.add((BaseResult) parsedResponsesObject);
                        loginResult = (BaseResult) parsedResponsesObject;
                    }

                    if (!parsedResponses.isEmpty() && parsedResponses.get(0) instanceof KalturaLoginSession) {
                        loginResult = parsedResponses.get(0);
                    }

                    if (loginResult != null && loginResult.error != null) {
                        error = loginResult.error;
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

                    int mediaAssetsStartIndex = 0;
                    if (loginResult != null && parsedResponses.size() > 1) {
                        mediaAssetsStartIndex = 1;
                    }

                    kalturaMediaAssets = new ArrayList<>();
                    assetsMetadtaList = new ArrayList<>();
                    for (; mediaAssetsStartIndex < parsedResponses.size(); mediaAssetsStartIndex++) {
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

            log.v(loadId + " playlist load finished, callback passed...notifyCompletion");
            notifyCompletion();
        }

        private PKPlaylist getPKPlaylist(String playlistKs, List<KalturaMediaAsset> entriesList, List<Map<String, String>> assetsMetadtaList) {
            if (entriesList == null || assetsMetadtaList == null) {
                return null;
            }

            List<PKPlaylistMedia> mediaList = new ArrayList<>();

            int listIndex = 0;
            for (KalturaMediaAsset kalturaMediaEntry : entriesList) {
                if (kalturaMediaEntry == null || kalturaMediaEntry.getMediaFiles() == null || kalturaMediaEntry.getMediaFiles().isEmpty()) {
                    mediaList.add(null);
                    listIndex++;
                    continue;
                }

                String thumbnailUrl = (kalturaMediaEntry.getImages() != null && !kalturaMediaEntry.getImages().isEmpty()) ? kalturaMediaEntry.getImages().get(0).getUrl() : "";

                if (kalturaMediaEntry.getMediaFiles().get(0) != null) {
                    mediaList.add(new PKPlaylistMedia().
                            setId(String.valueOf(kalturaMediaEntry.getId())).
                            setName(kalturaMediaEntry.getName()).
                            setDescription(kalturaMediaEntry.getDescription()).
                            setType(getMediaEntryType(listIndex, kalturaMediaEntry)).
                            setMsDuration(kalturaMediaEntry.getMediaFiles().get(0).getDuration() * Consts.MILLISECONDS_MULTIPLIER).
                            setThumbnailUrl(thumbnailUrl).
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

        private PKMediaEntry.MediaEntryType getMediaEntryType(int index, KalturaMediaAsset kalturaMediaAsset) {
            PKMediaEntry.MediaEntryType mediaEntryType = PKMediaEntry.MediaEntryType.Vod;
            if (isDvrLiveMedia(index)) {
                mediaEntryType = PKMediaEntry.MediaEntryType.DvrLive;
            } else if (isLiveMediaEntry(kalturaMediaAsset)) {
                mediaEntryType = PKMediaEntry.MediaEntryType.Live;
            }
            return mediaEntryType;
        }

        private boolean isDvrLiveMedia(int index) {
            if (playlist != null && playlist.mediaAssets != null && !playlist.mediaAssets.isEmpty() && index >= 0 && index < playlist.mediaAssets.size() && playlist.mediaAssets.get(index) != null) {
                return playlist.mediaAssets.get(index).assetType == APIDefines.KalturaAssetType.Epg && playlist.mediaAssets.get(index).contextType == APIDefines.PlaybackContextType.StartOver;
            }
            return false;
        }
    }
}
