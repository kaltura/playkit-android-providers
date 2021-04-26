package com.kaltura.playkit.providers.ovp;

import android.text.TextUtils;

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

import com.kaltura.playkit.PKPlaylist;
import com.kaltura.playkit.PKPlaylistMedia;
import com.kaltura.playkit.providers.PlaylistMetadata;
import com.kaltura.playkit.providers.PlaylistProvider;
import com.kaltura.playkit.providers.api.SimpleSessionProvider;
import com.kaltura.playkit.providers.api.ovp.KalturaOvpParser;
import com.kaltura.playkit.providers.api.ovp.OvpConfigs;

import com.kaltura.playkit.providers.api.ovp.model.KalturaBaseEntryListResponse;
import com.kaltura.playkit.providers.api.ovp.model.KalturaMediaEntry;
import com.kaltura.playkit.providers.api.ovp.model.KalturaMetadataListResponse;
import com.kaltura.playkit.providers.api.ovp.model.KalturaPlaylist;
import com.kaltura.playkit.providers.api.ovp.services.BaseEntryService;
import com.kaltura.playkit.providers.api.ovp.services.MetaDataService;
import com.kaltura.playkit.providers.api.ovp.services.OvpService;
import com.kaltura.playkit.providers.api.ovp.services.OvpSessionService;
import com.kaltura.playkit.providers.api.ovp.services.PlaylistService;
import com.kaltura.playkit.providers.base.BEBaseProvider;
import com.kaltura.playkit.providers.base.BECallableLoader;
import com.kaltura.playkit.providers.base.OnPlaylistLoadCompletion;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.kaltura.playkit.providers.ovp.KalturaOvpProviderUtils.getDefaultWidgetId;
import static com.kaltura.playkit.providers.ovp.KalturaOvpProviderUtils.getMediaEntryType;
import static com.kaltura.playkit.providers.ovp.KalturaOvpProviderUtils.parseMetadata;

public class KalturaOvpPlaylistProvider extends BEBaseProvider<PKPlaylist> implements PlaylistProvider {

    private static final PKLog log = PKLog.get("KalturaOvpPlaylistProvider");

    public static final boolean CanBeEmpty = true;

    private String playlistId;
    private List<OVPMediaAsset> mediaAssets;
    private PlaylistMetadata playlistMetadata;
    private Integer pageSize;
    private Integer pageIndex;


    public KalturaOvpPlaylistProvider() {
        super(log.tag);
    }

    public KalturaOvpPlaylistProvider(final String baseUrl, final int partnerId, final String ks) {
        this();
        setSessionProvider(new SimpleSessionProvider(baseUrl, partnerId, ks));
    }

    /**
     * MANDATORY! provides the baseUrl and the session token(ks) for the API calls.
     *
     * @param sessionProvider - {@link SessionProvider}
     * @return - instance of KalturaOvpMediaProvider
     */
    public KalturaOvpPlaylistProvider setSessionProvider(SessionProvider sessionProvider) {
        this.sessionProvider = sessionProvider;
        return this;
    }

    /**
     * Incase ussage of playlistId only
     *
     * @param playlistId - Kaltura playlistId
     * @return - instance of KalturaOvpMediaProvider
     */
    public KalturaOvpPlaylistProvider setPlaylistId(String playlistId) {
        this.playlistId = playlistId;
        return this;
    }


    /**
     * Incase ussage of no playlistId available only
     *
     * @param playlistMetadata - Kaltura playlistMetadata
     * @param mediaAssets - OVPMediaAsset to fetch
     * @return - instance of KalturaOvpMediaProvider
     */

    public KalturaOvpPlaylistProvider setPlaylistParams(PlaylistMetadata playlistMetadata, List<OVPMediaAsset> mediaAssets) {
        this.playlistMetadata = playlistMetadata;
        this.mediaAssets = mediaAssets;
        return this;
    }

    /**
     * Optional! the pager filter
     *
     * @param pageSize - Kaltura page filter num of pages
     * @param pageIndex - Kaltura page filter page number to retrieve
     * @return - instance of KalturaOvpMediaProvider
     */
    public KalturaOvpPlaylistProvider setPlaylistPagerFilter(Integer pageSize, Integer pageIndex) {
        this.pageSize = pageSize;
        this.pageIndex = pageIndex;
        return this;
    }

    /**
     * optional parameter.
     * Defaults to {@link APIOkRequestsExecutor} implementation.
     *
     * @param executor - executor
     * @return - instance of KalturaOvpMediaProvider
     */
    public KalturaOvpPlaylistProvider setRequestExecutor(RequestQueue executor) {
        this.requestsExecutor = executor;
        return this;
    }


    @Override
    protected Loader createNewLoader(OnCompletion<ResultElement<PKPlaylist>> completion) {
        if (playlistId != null) {
            return new Loader(requestsExecutor, sessionProvider, playlistId, pageSize, pageIndex, completion);
        } else {
            return new Loader(requestsExecutor, sessionProvider, mediaAssets, completion);
        }
    }

    @Override
    protected ErrorElement validateParams() {
        return TextUtils.isEmpty(playlistId) && mediaAssets == null ?
                ErrorElement.BadRequestError.message(ErrorElement.BadRequestError + ": Missing required parameters, playlistId") :
                null;
    }

    @Override
    public void load(OnPlaylistLoadCompletion completion) {
        load((OnCompletion<ResultElement<PKPlaylist>>)completion);
    }

    class Loader extends BECallableLoader {

        private String playlistId;
        private List<OVPMediaAsset> mediaAssets;
        private Integer pageSize;
        private Integer pageIndex;

        Loader(RequestQueue requestsExecutor, SessionProvider sessionProvider, String playlistId, Integer pageSize, Integer pageIndex, OnCompletion<ResultElement<PKPlaylist>> completion) {
            super(log.tag + "#Loader", requestsExecutor, sessionProvider, completion);

            this.playlistId = playlistId;
            this.pageSize = pageSize;
            this.pageIndex = pageIndex;

            log.v(loadId + ": construct new Loader");
        }

        Loader(RequestQueue requestsExecutor, SessionProvider sessionProvider, List<OVPMediaAsset> mediaAssets, OnCompletion<ResultElement<PKPlaylist>> completion) {
            super(log.tag + "#Loader", requestsExecutor, sessionProvider, completion);

            this.mediaAssets = mediaAssets;

            log.v(loadId + ": construct new Loader");
        }

        @Override
        protected ErrorElement validateKs(String ks) {
            if (TextUtils.isEmpty(ks)) {
                if (CanBeEmpty) {
                    log.w("provided ks is empty, Anonymous session will be used.");
                } else {
                    return ErrorElement.BadRequestError.message(ErrorElement.BadRequestError + ": SessionProvider should provide a valid KS token");
                }
            }
            return null;
        }

        private RequestBuilder getPlaylistInfo(String baseUrl, String ks, int partnerId) {
            MultiRequestBuilder multiRequestBuilder = (MultiRequestBuilder) OvpService.getMultirequest(baseUrl, ks, partnerId)
                    .tag("entry-info-multireq");

            boolean isAnonymusKS = TextUtils.isEmpty(ks);
            if (isAnonymusKS) {
                multiRequestBuilder.add(OvpSessionService.anonymousSession(baseUrl, getDefaultWidgetId(partnerId)));

                ks = "{1:result:ks}";
            }

            return multiRequestBuilder.add(PlaylistService.get(baseUrl, ks, playlistId),
                    PlaylistService.execute(baseUrl, ks, playlistId, pageSize, pageIndex));
        }

        private RequestBuilder getPlaylistInfoByEntryIdList(String baseUrl, String ks, int partnerId) {
            MultiRequestBuilder multiRequestBuilder = (MultiRequestBuilder) OvpService.getMultirequest(baseUrl, ks, partnerId)
                    .tag("entry-info-multireq");

            boolean isAnonymusKS = TextUtils.isEmpty(ks);
            if (isAnonymusKS) {
                multiRequestBuilder.add(OvpSessionService.anonymousSession(baseUrl, getDefaultWidgetId(partnerId)));

                ks = "{1:result:ks}";
            }

            for (OVPMediaAsset ovpMediaAsset : mediaAssets) {
                String requestKS = TextUtils.isEmpty(ovpMediaAsset.getKs()) ? ks : ovpMediaAsset.getKs();
                boolean redirectFromEntryId = ovpMediaAsset.redirectFromEntryId != null ? ovpMediaAsset.redirectFromEntryId : true;
                multiRequestBuilder.add(BaseEntryService.list(baseUrl, requestKS, ovpMediaAsset.entryId, ovpMediaAsset.referenceId, redirectFromEntryId), MetaDataService.list(baseUrl, requestKS, ovpMediaAsset.entryId));
            }

            return multiRequestBuilder;
        }

        /**
         * Builds and passes to the executor, the multirequest for entry info and playback info fetching.
         *
         * @param ks - Kaltura KS
         */
        @Override
        protected void requestRemote(final String ks) throws InterruptedException {
            if (!TextUtils.isEmpty(playlistId)) {
                handleByPlaylistIdResponse(ks);
            } else if (mediaAssets != null && !mediaAssets.isEmpty()) {
                handleByPlaylistAssets(ks);
            }
        }

        private void handleByPlaylistIdResponse(String ks) throws InterruptedException {
            final RequestBuilder entryRequest = getPlaylistInfo(getApiBaseUrl(), ks, sessionProvider.partnerId())
                    .completion(response -> {
                        PKPlaylist playlistResult = null;
                        ErrorElement error = null;

                        if (isErrorInResponse(response, error)) {
                            return;
                        }

                        log.v(loadId + ": got response to [" + loadReq + "]" + " isCanceled = " + isCanceled);
                        loadReq = null;

                        List<BaseResult> responses = KalturaOvpParser.parse(response.getResponse());
                        if (responses == null || responses.size() == 0) {
                            error = ErrorElement.LoadError.message("failed to get responses on load requests");
                            completion.onComplete(Accessories.buildResult(null, error));
                            return;
                        }

                        for (int i = 0 ; i < responses.size() - 1 ; i++) {
                            if (responses.get(i).error != null) {
                                completion.onComplete(Accessories.buildResult(null, responses.get(i).error));
                                return;
                            }
                        }

                        int playlistListIndex = responses.size() > 2 ? 1 : 0;
                        int entriesListIndex = playlistListIndex + 1;

                        if (!TextUtils.isEmpty(ks) && responses.size() == entriesListIndex || responses.size() == (entriesListIndex + 1)) {
                            KalturaPlaylist kalturaPlaylist = (KalturaPlaylist) responses.get(playlistListIndex);
                            List<KalturaMediaEntry> entriesList = (List<KalturaMediaEntry>) responses.get(entriesListIndex);
                            playlistResult = getPKPlaylist(ks, kalturaPlaylist, entriesList);
                            if (completion != null) {
                                completion.onComplete(Accessories.buildResult(playlistResult, null));
                            }
                        } else {
                            if (!isCanceled() && completion != null) {
                                completion.onComplete(Accessories.buildResult(null, ErrorElement.LoadError.message("failed to get responses on load requests")));
                            }
                        }
                    });

            synchronized (syncObject) {
                loadReq = requestQueue.queue(entryRequest.build());
                log.d(loadId + ": request queued for execution [" + loadReq + "]");
            }

            if (!isCanceled()) {
                waitCompletion();
            }
        }

        private void handleByPlaylistAssets(String ks) throws InterruptedException {
            final RequestBuilder entryRequest = getPlaylistInfoByEntryIdList(getApiBaseUrl(), ks, sessionProvider.partnerId())
                    .completion(response -> {
                        PKPlaylist playlistResult;
                        ErrorElement error = null;

                        if (isErrorInResponse(response, error)) {
                            return;
                        }

                        log.v(loadId + ": got response to [" + loadReq + "]" + " isCanceled = " + isCanceled);
                        loadReq = null;

                        List<BaseResult> responses = KalturaOvpParser.parse(response.getResponse());
                        if (responses == null || responses.size() == 0) {
                            error = ErrorElement.LoadError.message("failed to get responses on load requests");
                            completion.onComplete(Accessories.buildResult(null, error));
                            return;
                        }

                        if (TextUtils.isEmpty(ks) && responses.get(0).error != null) {
                            completion.onComplete(Accessories.buildResult(null, responses.get(0).error));
                            return;
                        } else {
                            boolean allErrors = true;
                            for (BaseResult baseResult : responses) {
                                if (baseResult.error == null) {
                                    allErrors = false;
                                    break;
                                }
                            }
                            if (allErrors) {
                                completion.onComplete(Accessories.buildResult(null, responses.get(0).error));
                                return;
                            }
                        }

                        if (!TextUtils.isEmpty(ks) && responses.size() == mediaAssets.size() * 2 || responses.size() == (mediaAssets.size() * 2 + 1)) {
                            List<KalturaMediaEntry> entriesList = new ArrayList<>();
                            List<Map<String,String>> metadataList = new ArrayList<>();
                            int playlistListIndex = TextUtils.isEmpty(ks) ? 1 : 0;
                            for( ; playlistListIndex < responses.size() ; playlistListIndex++) {
                                if (responses.get(playlistListIndex).error != null) {
                                    entriesList.add(null);
                                    continue;
                                }

                                if (responses.get(playlistListIndex) instanceof KalturaBaseEntryListResponse) {
                                    entriesList.add(((KalturaBaseEntryListResponse) responses.get(playlistListIndex)).objects.get(0));
                                }
                                if (responses.get(playlistListIndex) instanceof KalturaMetadataListResponse) {
                                    metadataList.add(parseMetadata((KalturaMetadataListResponse) responses.get(playlistListIndex)));
                                }
                            }

                            int listIndex = 0;
                            List<PKPlaylistMedia> mediaList = new ArrayList<>();
                            for (KalturaMediaEntry kalturaMediaEntry : entriesList) {
                                if (kalturaMediaEntry == null) {
                                    mediaList.add(null);
                                    listIndex++;
                                    continue;
                                }
                                Map<String,String> mediaMetadata = metadataList.get(listIndex);
                                mediaList.add(new PKPlaylistMedia().
                                        setId(kalturaMediaEntry.getId()).
                                        setName(kalturaMediaEntry.getName()).
                                        setDescription(kalturaMediaEntry.getDescription()).
                                        setType(getMediaEntryType(kalturaMediaEntry)).
                                        setDataUrl(kalturaMediaEntry.getDataUrl()).
                                        setMsDuration(kalturaMediaEntry.getMsDuration()).
                                        setThumbnailUrl(kalturaMediaEntry.getThumbnailUrl()).
                                        setFlavorParamsIds(kalturaMediaEntry.getFlavorParamsIds()).
                                        setMetadata(mediaMetadata).
                                        setTags(kalturaMediaEntry.getTags()));
                            }

                            if (playlistMetadata == null) {
                                playlistMetadata = new PlaylistMetadata();
                            }
                            playlistResult = new PKPlaylist().
                                    setKs(ks).
                                    setId(playlistMetadata.getId()).
                                    setName(playlistMetadata.getName()).
                                    setDescription(playlistMetadata.getDescription()).
                                    setThumbnailUrl(playlistMetadata.getThumbnailUrl()).
                                    setMediaList(mediaList);

                            if (completion != null) {
                                completion.onComplete(Accessories.buildResult(playlistResult, null));
                            }
                        } else {
                            if (!isCanceled() && completion != null) {
                                completion.onComplete(Accessories.buildResult(null, ErrorElement.LoadError.message("failed to get responses on load requests")));
                            }
                        }
                    });

            synchronized (syncObject) {
                loadReq = requestQueue.queue(entryRequest.build());
                log.d(loadId + ": request queued for execution [" + loadReq + "]");
            }

            if (!isCanceled()) {
                waitCompletion();
            }
        }

        private boolean isErrorInResponse(ResponseElement response, ErrorElement error) {
            if (response == null) {
                error = ErrorElement.LoadError.message("failed to get valid response, response == null");
            } else if (response.getError() != null) {
                error = response.getError();
            }

            if (error != null) {
                completion.onComplete(Accessories.buildResult(null, error));
                return true;
            }
            return false;
        }

        private String getApiBaseUrl() {
            String sep = sessionProvider.baseUrl().endsWith("/") ? "" : "/";
            return sessionProvider.baseUrl() + sep + OvpConfigs.ApiPrefix;
        }
    }

    private PKPlaylist getPKPlaylist(String playlistKs, KalturaPlaylist kalturaPlaylist,  List<KalturaMediaEntry> entriesList) {
        List<PKPlaylistMedia> mediaList = new ArrayList<>();
        for (KalturaMediaEntry kalturaMediaEntry : entriesList) {
            mediaList.add(new PKPlaylistMedia().
                    setId(kalturaMediaEntry.getId()).
                    setName(kalturaMediaEntry.getName()).
                    setDescription(kalturaMediaEntry.getDescription()).
                    setType(getMediaEntryType(kalturaMediaEntry)).
                    setDataUrl(kalturaMediaEntry.getDataUrl()).
                    setMsDuration(kalturaMediaEntry.getMsDuration()).
                    setThumbnailUrl(kalturaMediaEntry.getThumbnailUrl()).
                    setFlavorParamsIds(kalturaMediaEntry.getFlavorParamsIds()).
                    setTags(kalturaMediaEntry.getTags()));
        }

        PKPlaylist playlist = new PKPlaylist().
                setKs(playlistKs).
                setId(kalturaPlaylist.getId()).
                setName(kalturaPlaylist.getName()).
                setDescription(kalturaPlaylist.getDescription()).
                setThumbnailUrl(kalturaPlaylist.getThumbnailUrl()).
                setMediaList(mediaList);

        return playlist;
    }
}
