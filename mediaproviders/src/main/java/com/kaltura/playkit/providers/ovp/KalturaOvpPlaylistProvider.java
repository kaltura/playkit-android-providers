package com.kaltura.playkit.providers.ovp;

import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.google.gson.JsonSyntaxException;
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
import com.kaltura.netkit.utils.OnRequestCompletion;
import com.kaltura.netkit.utils.SessionProvider;
import com.kaltura.playkit.PKLog;

import com.kaltura.playkit.PKMediaEntry;
import com.kaltura.playkit.PKPlaylist;
import com.kaltura.playkit.PKPlaylistMedia;
import com.kaltura.playkit.PKPlaylistType;
import com.kaltura.playkit.providers.api.ovp.KalturaOvpParser;
import com.kaltura.playkit.providers.api.ovp.OvpConfigs;

import com.kaltura.playkit.providers.api.ovp.model.KalturaEntryType;
import com.kaltura.playkit.providers.api.ovp.model.KalturaMediaEntry;
import com.kaltura.playkit.providers.api.ovp.model.KalturaPlaylist;
import com.kaltura.playkit.providers.api.ovp.model.KalturaPlaylistType;
import com.kaltura.playkit.providers.api.ovp.model.KalturaStartWidgetSessionResponse;
import com.kaltura.playkit.providers.api.ovp.services.OvpService;
import com.kaltura.playkit.providers.api.ovp.services.OvpSessionService;
import com.kaltura.playkit.providers.api.ovp.services.PlaylistService;
import com.kaltura.playkit.providers.base.BECallableLoader;
import com.kaltura.playkit.providers.base.BEMediaProvider;

import com.kaltura.playkit.providers.base.OnMediaLoadCompletion;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.kaltura.netkit.utils.ErrorElement.GeneralError;

public class KalturaOvpPlaylistProvider extends BEMediaProvider {

    private static final PKLog log = PKLog.get("KalturaOvpPlaylistProvider");

    public static final String KALTURA_API_EXCEPTION = "KalturaAPIException";
    public static final String OBJECT_TYPE = "objectType";
    public static final String CODE = "code";
    public static final String MESSAGE = "message";

    public static final boolean CanBeEmpty = true;

    private String playlistId;
    private Integer pageSize;
    private Integer pageIndex;


    public KalturaOvpPlaylistProvider() {
        super(log.tag);
    }

    public KalturaOvpPlaylistProvider(final String baseUrl, final int partnerId, final String ks) {
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
     * MANDATORY! the entry id, to fetch the data for.
     *
     * @param playlistId - Kaltura playlistId
     * @return - instance of KalturaOvpMediaProvider
     */
    public KalturaOvpPlaylistProvider setPlaylistId(String playlistId) {
        this.playlistId = playlistId;
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
    protected Loader factorNewLoader(OnMediaLoadCompletion completion) {
        return new Loader(requestsExecutor, sessionProvider, playlistId, pageSize, pageIndex, completion);
    }

    @Override
    protected ErrorElement validateParams() {
        return TextUtils.isEmpty(this.playlistId) ?
                ErrorElement.BadRequestError.message(ErrorElement.BadRequestError + ": Missing required parameters, playlistId") :
                null;
    }

    class Loader extends BECallableLoader {

        private String playlistId;
        private Integer pageSize;
        private Integer pageIndex;

        Loader(RequestQueue requestsExecutor, SessionProvider sessionProvider, String playlistId, Integer pageSize, Integer pageIndex, OnMediaLoadCompletion completion) {
            super(log.tag + "#Loader", requestsExecutor, sessionProvider, completion);

            this.playlistId = playlistId;
            this.pageSize = pageSize;
            this.pageIndex = pageIndex;

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

        /**
         * Builds and passes to the executor, the multirequest for entry info and playback info fetching.
         *
         * @param ks - Kaltura KS
         */
        @Override
        protected void requestRemote(final String ks) throws InterruptedException {
            if (!TextUtils.isEmpty(playlistId)) {
                final RequestBuilder entryRequest = getPlaylistInfo(getApiBaseUrl(), ks, sessionProvider.partnerId())//getEntryInfo(getApiBaseUrl(), ks, sessionProvider.partnerId())
                        .completion(new OnRequestCompletion() {
                            @Override
                            public void onComplete(ResponseElement response) {
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

                                int widgetSessionResponseIndex = 0;
                                int playlistListIndex = responses.size() > 2 ? 1 : 0;
                                int entriesListIndex = playlistListIndex + 1;

                                if (!TextUtils.isEmpty(ks) && responses.size() == entriesListIndex || responses.size() == (entriesListIndex + 1)) {
                                    KalturaStartWidgetSessionResponse widgetSessionResponse = null;
                                    if (responses.size() == (entriesListIndex + 1)) {
                                        widgetSessionResponse = (KalturaStartWidgetSessionResponse) responses.get(widgetSessionResponseIndex);
                                    }
                                    KalturaPlaylist kalturaPlaylist = (KalturaPlaylist) responses.get(playlistListIndex);
                                    List<KalturaMediaEntry> entriesList = (List<KalturaMediaEntry>) responses.get(entriesListIndex);
                                    playlistResult = getPKPlaylist(widgetSessionResponse == null ? ks : widgetSessionResponse.getKs(), kalturaPlaylist, entriesList);
                                    if (completion != null) {
                                        completion.onComplete(Accessories.buildResult(playlistResult, null));
                                    }
                                } else {
                                    if (!isCanceled() && completion != null) {
                                        completion.onComplete(Accessories.buildResult(null, ErrorElement.LoadError.message("failed to get responses on load requests")));
                                    }
                                }
                            }

                            private PKPlaylist getPKPlaylist(String playlistKs, KalturaPlaylist kalturaPlaylist,  List<KalturaMediaEntry> entriesList) {
                                List<PKPlaylistMedia> mediaList = new ArrayList<>();
                                for (KalturaMediaEntry kalturaMediaEntry : entriesList) {
                                    mediaList.add(new PKPlaylistMedia().
                                            setId(kalturaMediaEntry.getId()).
                                            setName(kalturaMediaEntry.getName()).
                                            setDescription(kalturaMediaEntry.getDescription()).
                                            setType(getTypeOf(kalturaMediaEntry.getType())).
                                            setDataUrl(kalturaMediaEntry.getDataUrl()).
                                            setMsDuration(kalturaMediaEntry.getMsDuration()).
                                            setDvrStatus(kalturaMediaEntry.getDvrStatus()).
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
                                        setDuration(kalturaPlaylist.getDuration()).
                                        setPlaylistType(getValueOf(kalturaPlaylist.getPlaylistType())).
                                        setMediaList(mediaList);

                                return playlist;
                            }

                            private PKPlaylistType getValueOf(KalturaPlaylistType playlistType) {
                                if (playlistType == null) {
                                    return PKPlaylistType.Unknown;
                                }

                                switch (playlistType) {
                                    case STATIC_LIST:
                                        return PKPlaylistType.Static;
                                    case DYNAMIC:
                                        return PKPlaylistType.Dynamic;
                                    case EXTERNAL:
                                        return PKPlaylistType.External;
                                    default:
                                        return PKPlaylistType.Unknown;
                                }
                            }

                            private PKMediaEntry.MediaEntryType getTypeOf(KalturaEntryType type) {
                                if (type == null) {
                                    return PKMediaEntry.MediaEntryType.Unknown;
                                }

                                switch (type) {
                                    case MEDIA_CLIP:
                                        return PKMediaEntry.MediaEntryType.Vod;
                                    case LIVE_STREAM:
                                        return PKMediaEntry.MediaEntryType.Live;
                                    default:
                                        return PKMediaEntry.MediaEntryType.Unknown;
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
        }

        private String getApiBaseUrl() {
            String sep = sessionProvider.baseUrl().endsWith("/") ? "" : "/";
            return sessionProvider.baseUrl() + sep + OvpConfigs.ApiPrefix;
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
