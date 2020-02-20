package com.kaltura.playkit.providers.ovp;

import android.text.TextUtils;

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
import com.kaltura.playkit.providers.api.ovp.KalturaOvpParser;
import com.kaltura.playkit.providers.api.ovp.OvpConfigs;

import com.kaltura.playkit.providers.api.ovp.model.KalturaBaseEntryListResponse;
import com.kaltura.playkit.providers.api.ovp.model.KalturaMediaEntry;
import com.kaltura.playkit.providers.api.ovp.model.KalturaMetadata;
import com.kaltura.playkit.providers.api.ovp.model.KalturaMetadataListResponse;
import com.kaltura.playkit.providers.api.ovp.model.KalturaPlaylist;

import com.kaltura.playkit.providers.api.ovp.services.BaseEntryService;

import com.kaltura.playkit.providers.api.ovp.services.OvpService;
import com.kaltura.playkit.providers.api.ovp.services.OvpSessionService;
import com.kaltura.playkit.providers.api.ovp.services.PlaylistService;
import com.kaltura.playkit.providers.base.BECallableLoader;

import com.kaltura.playkit.providers.base.BEPlaylistProvider;
import com.kaltura.playkit.providers.base.OnMediaLoadCompletion;
import com.kaltura.playkit.providers.base.OnPlaylistLoadCompletion;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import static com.kaltura.netkit.utils.ErrorElement.GeneralError;

public class KalturaOvpPlaylistProvider extends BEPlaylistProvider {

    private static final PKLog log = PKLog.get("KalturaOvpPlaylistProvider");

    public static final String KALTURA_API_EXCEPTION = "KalturaAPIException";
    public static final String OBJECT_TYPE = "objectType";
    public static final String CODE = "code";
    public static final String MESSAGE = "message";

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
    protected Callable<Void> createNewLoader(OnPlaylistLoadCompletion completion) {
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

    class Loader extends BECallableLoader {

        private String playlistId;
        private List<OVPMediaAsset> mediaAssets;
        private Integer pageSize;
        private Integer pageIndex;

        Loader(RequestQueue requestsExecutor, SessionProvider sessionProvider, String playlistId, Integer pageSize, Integer pageIndex, OnPlaylistLoadCompletion completion) {
            super(log.tag + "#Loader", requestsExecutor, sessionProvider, completion);

            this.playlistId = playlistId;
            this.pageSize = pageSize;
            this.pageIndex = pageIndex;

            log.v(loadId + ": construct new Loader");
        }

        Loader(RequestQueue requestsExecutor, SessionProvider sessionProvider, List<OVPMediaAsset> mediaAssets, OnPlaylistLoadCompletion completion) {
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
                multiRequestBuilder.add(BaseEntryService.list(baseUrl, ks, ovpMediaAsset.entryId)); // , MetaDataService.list(baseUrl, ks, ovpMediaAsset.entryId) not added as for now.
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

                        if (response == null || response.getError() != null) {
                            error = response.getError() != null ? response.getError() : ErrorElement.LoadError;
                            completion.onComplete(Accessories.buildResult(null, error));
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

                        for (int i = 0 ; i < responses.size() - 1 ; i++) { //index 3 in ArrayList does not contain error object
                            if (responses.get(i).error != null) {
                                completion.onComplete(Accessories.buildResult(null, responses.get(i).error));
                                return;
                            }
                        }

//                                int widgetSessionResponseIndex = 0;
                        int playlistListIndex = responses.size() > 2 ? 1 : 0;
                        int entriesListIndex = playlistListIndex + 1;

                        if (!TextUtils.isEmpty(ks) && responses.size() == entriesListIndex || responses.size() == (entriesListIndex + 1)) {
//                                    KalturaStartWidgetSessionResponse widgetSessionResponse = null;
//                                    if (responses.size() == (entriesListIndex + 1)) {
//                                        widgetSessionResponse = (KalturaStartWidgetSessionResponse) responses.get(widgetSessionResponseIndex);
//                                    }
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
            final RequestBuilder entryRequest = getPlaylistInfoByEntryIdList(getApiBaseUrl(), ks, sessionProvider.partnerId())//getEntryInfo(getApiBaseUrl(), ks, sessionProvider.partnerId())
                    .completion(response -> {
                        PKPlaylist playlistResult = null;
                        ErrorElement error = null;

                        if (response == null || response.getError() != null) {
                            error = response.getError() != null ? response.getError() : ErrorElement.LoadError;
                            completion.onComplete(Accessories.buildResult(null, error));
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

                        if (!TextUtils.isEmpty(ks) && responses.size() == mediaAssets.size() || responses.size() == (mediaAssets.size() + 1)) {
                            List<KalturaMediaEntry> entriesList = new ArrayList<>();
                            //List<Map<String,String>> metadataList = new ArrayList<>();
                            int playlistListIndex = TextUtils.isEmpty(ks) ? 1 : 0;
                            for( ; playlistListIndex < responses.size() ; playlistListIndex++) {
                                if (responses.get(playlistListIndex).error != null) {
                                    entriesList.add(null);
                                    continue;
                                }

                                if (responses.get(playlistListIndex) instanceof KalturaBaseEntryListResponse) {
                                    entriesList.add(((KalturaBaseEntryListResponse) responses.get(playlistListIndex)).objects.get(0));
                                }
//                                      else if (responses.get(playlistListIndex) instanceof KalturaMetadataListResponse) {
//                                            KalturaMetadataListResponse metadataListResponse = (KalturaMetadataListResponse) responses.get(playlistListIndex);
//
//                                            metadataList.add(parseMetadata(metadataListResponse));
//                                        }
                            }

                            int listIndex = 0;
                            List<PKPlaylistMedia> mediaList = new ArrayList<>();
                            for (KalturaMediaEntry kalturaMediaEntry : entriesList) {
                                if (kalturaMediaEntry == null) {
                                    mediaList.add(null);
                                    listIndex++;
                                    continue;
                                }
                                mediaList.add(new PKPlaylistMedia().
                                        setMediaIndex(listIndex++).
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

        private String getApiBaseUrl() {
            String sep = sessionProvider.baseUrl().endsWith("/") ? "" : "/";
            return sessionProvider.baseUrl() + sep + OvpConfigs.ApiPrefix;
        }

        private Map<String, String> parseMetadata(KalturaMetadataListResponse metadataList) {
            Map<String, String> metadata = new HashMap<>();
            if (metadataList != null && metadataList.objects != null && metadataList.objects.size() > 0) {
                for (KalturaMetadata metadataItem : metadataList.objects) {
                    extractMetadata(metadataItem.xml, metadata);
                }
            }
            return metadata;
        }

        private void extractMetadata(String xml, Map<String, String> metadataMap) {

            XmlPullParserFactory xmlPullfactory;
            try {
                xmlPullfactory = XmlPullParserFactory.newInstance();
                xmlPullfactory.setNamespaceAware(true);

                XmlPullParser xmlPullParser = xmlPullfactory.newPullParser();
                xmlPullParser.setInput(new StringReader(xml));
                int eventType = xmlPullParser.getEventType();

                boolean metadataParseStarted = false;
                String key = "";
                String value = "";

                while (eventType != XmlPullParser.END_DOCUMENT) {

                    if(eventType == XmlPullParser.START_DOCUMENT) {
                        log.d("extractMetadata Start document");
                    } else if(eventType == XmlPullParser.START_TAG) {
                        if ("metadata".equals(xmlPullParser.getName())) {
                            metadataParseStarted = true;
                        } else {
                            key = xmlPullParser.getName();
                        }
                    } else if(eventType == XmlPullParser.END_TAG) {

                        if ("metadata".equals(xmlPullParser.getName())) {
                            metadataParseStarted = false;
                        } else {
                            if (metadataParseStarted) {
                                log.d( "extractMetadata key/val = " + key + "/" + value);
                                if (!TextUtils.isEmpty(key)) {
                                    metadataMap.put(key, value);
                                }
                                key = "";
                                value = "";
                            }
                        }
                    } else if(eventType == XmlPullParser.TEXT) {
                        value = xmlPullParser.getText();
                    }
                    eventType = xmlPullParser.next();
                }
                log.d("extractMetadata End document");
            } catch (XmlPullParserException | IOException e) {
                log.e("extractMetadata: XML parsing failed", e);
            }
        }

        private boolean isValidResponse(ResponseElement response, OnMediaLoadCompletion completion) {

            if (isErrorResponse(response)) {
                ErrorElement errorResponse = parseErrorRersponse(response);
                if (errorResponse == null) {
                    errorResponse = GeneralError;
                }
                if (!isCanceled() && completion != null) {
                    completion.onComplete(Accessories.buildResult(null, errorResponse));
                }

                notifyCompletion();
                return false;
            }

            if (isAPIExceptionResponse(response)) {
                ErrorElement apiExceptionError = parseAPIExceptionError(response);
                if (apiExceptionError == null) {
                    apiExceptionError = GeneralError;
                }

                if (!isCanceled() && completion != null) {
                    completion.onComplete(Accessories.buildResult(null, apiExceptionError));
                }

                notifyCompletion();
                return false;
            }
            return true;
        }
    }

    private PKPlaylist getPKPlaylist(String playlistKs, KalturaPlaylist kalturaPlaylist,  List<KalturaMediaEntry> entriesList) {
        List<PKPlaylistMedia> mediaList = new ArrayList<>();
        int listIndex = 0;
        for (KalturaMediaEntry kalturaMediaEntry : entriesList) {
            mediaList.add(new PKPlaylistMedia().
                    setMediaIndex(listIndex++).
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

    private PKMediaEntry.MediaEntryType getMediaEntryType(KalturaMediaEntry kalturaMediaEntry) {
        PKMediaEntry.MediaEntryType mediaEntryType = PKMediaEntry.MediaEntryType.Vod;

        if (kalturaMediaEntry.getDvrStatus() != null) {
            if (kalturaMediaEntry.getDvrStatus() == 0) {
                mediaEntryType = PKMediaEntry.MediaEntryType.Live;
            } else {
                mediaEntryType = PKMediaEntry.MediaEntryType.DvrLive;
            }
        }
        return mediaEntryType;
    }

    private String getDefaultWidgetId(int partnerId) {
        return "_" + partnerId;
    }

    private boolean isAPIExceptionResponse(ResponseElement response) {
        return response == null|| (response.isSuccess() && response.getError() == null && response.getResponse() != null && response.getResponse().contains(KALTURA_API_EXCEPTION));
    }

    private boolean isErrorResponse(ResponseElement response) {
        return response == null|| (!response.isSuccess() && response.getError() != null);
    }


    private ErrorElement parseErrorRersponse(ResponseElement response) {
        if (response != null) {
            return response.getError();
        }
        return null;
    }

    private ErrorElement parseAPIExceptionError(ResponseElement response) {

        if (response != null) {
            String responseStr = response.getResponse();
            try {
                String ovpError = "OVPError";
                if (responseStr != null && responseStr.startsWith("{") && responseStr.endsWith("}")) {

                    JSONObject error = new JSONObject(response.getResponse());
                    if (error != null) {
                        Map<String, String> errorMap = getAPIExceptionData(error);
                        if (errorMap != null) {
                            return new ErrorElement(errorMap.get(MESSAGE), errorMap.get(CODE), errorMap.get(OBJECT_TYPE)).setName(ovpError);
                        }
                    }

                } else if (responseStr != null && responseStr.startsWith("[") && responseStr.endsWith("]")) {
                    JSONArray result = new JSONArray(response.getResponse());
                    for (int idx = 0; idx < result.length(); idx++) {
                        JSONObject error = (JSONObject) result.get(idx);
                        Map<String, String> errorMap = getAPIExceptionData(error);
                        if (errorMap != null && KALTURA_API_EXCEPTION.equals(errorMap.get(OBJECT_TYPE))) {
                            return new ErrorElement(errorMap.get(MESSAGE), errorMap.get(CODE), errorMap.get(OBJECT_TYPE)).setName(ovpError);
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
