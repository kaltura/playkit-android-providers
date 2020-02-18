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
import com.kaltura.playkit.PKMediaFormat;
import com.kaltura.playkit.PKMediaSource;
import com.kaltura.playkit.PKSubtitleFormat;
import com.kaltura.playkit.player.PKExternalSubtitle;

import com.kaltura.playkit.providers.api.base.model.KalturaDrmPlaybackPluginData;
import com.kaltura.playkit.providers.api.ovp.KalturaOvpErrorHelper;
import com.kaltura.playkit.providers.api.ovp.KalturaOvpParser;
import com.kaltura.playkit.providers.api.ovp.OvpConfigs;
import com.kaltura.playkit.providers.api.ovp.model.FlavorAssetsFilter;
import com.kaltura.playkit.providers.api.ovp.model.KalturaBaseEntryListResponse;
import com.kaltura.playkit.providers.api.ovp.model.KalturaCaptionType;
import com.kaltura.playkit.providers.api.ovp.model.KalturaEntryContextDataResult;
import com.kaltura.playkit.providers.api.ovp.model.KalturaFlavorAsset;
import com.kaltura.playkit.providers.api.ovp.model.KalturaMediaEntry;
import com.kaltura.playkit.providers.api.ovp.model.KalturaMetadata;
import com.kaltura.playkit.providers.api.ovp.model.KalturaMetadataListResponse;
import com.kaltura.playkit.providers.api.ovp.model.KalturaPlaybackCaption;
import com.kaltura.playkit.providers.api.ovp.model.KalturaPlaybackContext;
import com.kaltura.playkit.providers.api.ovp.model.KalturaPlaybackSource;
import com.kaltura.playkit.providers.api.ovp.services.BaseEntryService;
import com.kaltura.playkit.providers.api.ovp.services.MetaDataService;
import com.kaltura.playkit.providers.api.ovp.services.OvpService;
import com.kaltura.playkit.providers.api.ovp.services.OvpSessionService;
import com.kaltura.playkit.providers.base.BECallableLoader;
import com.kaltura.playkit.providers.base.BEMediaProvider;
import com.kaltura.playkit.providers.base.FormatsHelper;
import com.kaltura.playkit.providers.base.OnMediaLoadCompletion;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static com.kaltura.playkit.providers.MediaProvidersUtils.buildBadRequestErrorElement;
import static com.kaltura.playkit.providers.MediaProvidersUtils.buildGeneralErrorElement;
import static com.kaltura.playkit.providers.MediaProvidersUtils.buildLoadErrorElement;
import static com.kaltura.playkit.providers.MediaProvidersUtils.isDRMSchemeValid;
import static com.kaltura.playkit.providers.MediaProvidersUtils.updateDrmParams;

public class KalturaOvpMediaProvider extends BEMediaProvider {

    private static final PKLog log = PKLog.get("KalturaOvpMediaProvider");

    public static final String KALTURA_API_EXCEPTION = "KalturaAPIException";
    public static final String OBJECT_TYPE = "objectType";
    public static final String CODE = "code";
    public static final String MESSAGE = "message";

    public static final boolean CanBeEmpty = true;

    private String entryId;
    private String uiConfId;
    private String referrer;
    private boolean useApiCaptions;

    private int maxBitrate;
    private Map<String, Object> flavorsFilter;

    public KalturaOvpMediaProvider() {
        super(log.tag);
    }

    public KalturaOvpMediaProvider(final String baseUrl, final int partnerId, final String ks) {
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
    public KalturaOvpMediaProvider setSessionProvider(SessionProvider sessionProvider) {
        this.sessionProvider = sessionProvider;
        return this;
    }

    /**
     * NOT MANDATORY! The referrer url, to fetch the data for.
     *
     * @param referrer - application referrer.
     * @return - instance of KalturaOvpMediaProvider
     */
    public KalturaOvpMediaProvider setReferrer(String referrer) {
        this.referrer = referrer;
        return this;
    }

    /**
     * MANDATORY! the entry id, to fetch the data for.
     *
     * @param entryId - Kaltura entryID
     * @return - instance of KalturaOvpMediaProvider
     */
    public KalturaOvpMediaProvider setEntryId(String entryId) {
        this.entryId = entryId;
        return this;
    }

    /**
     * optional parameter.
     * Defaults to {@link APIOkRequestsExecutor} implementation.
     *
     * @param executor - executor
     * @return - instance of KalturaOvpMediaProvider
     */
    public KalturaOvpMediaProvider setRequestExecutor(RequestQueue executor) {
        this.requestsExecutor = executor;
        return this;
    }

    /**
     * optional parameter
     * will be used in media sources url
     *
     * @param uiConfId - Kaltura uiConfID
     * @return - instance of KalturaOvpMediaProvider
     */
    public KalturaOvpMediaProvider setUiConfId(String uiConfId) {
        this.uiConfId = uiConfId;
        return this;
    }

    public KalturaOvpMediaProvider setUseApiCaptions(boolean useApiCaptions) {
        this.useApiCaptions = useApiCaptions;
        return this;
    }

    @Override
    protected Loader factorNewLoader(OnMediaLoadCompletion completion) {
        return new Loader(requestsExecutor, sessionProvider, entryId, uiConfId, referrer, completion);
    }

    @Override
    protected ErrorElement validateParams() {
        return TextUtils.isEmpty(this.entryId) ?
                buildBadRequestErrorElement(ErrorElement.BadRequestError + ": Missing required parameters, entryId") :
                null;
    }

    class Loader extends BECallableLoader {

        private String entryId;
        private String uiConfId;
        private String referrer;

        Loader(RequestQueue requestsExecutor, SessionProvider sessionProvider, String entryId, String uiConfId, String referrer, OnMediaLoadCompletion completion) {
            super(log.tag + "#Loader", requestsExecutor, sessionProvider, completion);

            this.entryId = entryId;
            this.uiConfId = uiConfId;
            this.referrer = referrer;

            log.v(loadId + ": construct new Loader");
        }

        @Override
        protected ErrorElement validateKs(String ks) {
            if (TextUtils.isEmpty(ks)) {
                if (CanBeEmpty) {
                    log.w("provided ks is empty, Anonymous session will be used.");
                } else {
                    return buildBadRequestErrorElement(ErrorElement.BadRequestError + ": SessionProvider should provide a valid KS token");
                }
            }
            return null;
        }

        private RequestBuilder getEntryInfo(String baseUrl, String ks, int partnerId) {
            MultiRequestBuilder multiRequestBuilder = (MultiRequestBuilder) OvpService.getMultirequest(baseUrl, ks, partnerId)
                    .tag("entry-info-multireq");

            String baseEntryServiceEntryId = "{1:result:objects:0:id}";

            boolean isAnonymusKS = TextUtils.isEmpty(ks);
            if (isAnonymusKS) {
                multiRequestBuilder.add(OvpSessionService.anonymousSession(baseUrl, getDefaultWidgetId(partnerId)));

                ks = "{1:result:ks}";
                baseEntryServiceEntryId = "{2:result:objects:0:id}";
            }

            return multiRequestBuilder.add(BaseEntryService.list(baseUrl, ks, entryId),
                    BaseEntryService.getPlaybackContext(baseUrl, ks, baseEntryServiceEntryId, referrer),
                    MetaDataService.list(baseUrl, ks, baseEntryServiceEntryId));
        }

        /**
         * Builds and passes to the executor, the multirequest for entry info and playback info fetching.
         *
         * @param ks - Kaltura KS
         */
        @Override
        protected void requestRemote(final String ks) throws InterruptedException {
            final RequestBuilder entryRequest = getEntryInfo(getApiBaseUrl(), ks, sessionProvider.partnerId())
                    .completion(new OnRequestCompletion() {
                        @Override
                        public void onComplete(ResponseElement response) {
                            log.v(loadId + ": got response to [" + loadReq + "]" + " isCanceled = " + isCanceled);
                            loadReq = null;

                            try {
                                onEntryInfoMultiResponse(ks, response, (OnMediaLoadCompletion) completion);
                            } catch (InterruptedException e) {
                                interrupted();
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

        /**
         * Parse and create a {@link PKMediaEntry} object from the multirequest call sent to the BE.
         *
         * @param ks - Kaltura KS
         * @param response - Server response
         * @param completion - A callback to pass the constructed {@link PKMediaEntry} object on.
         */
        private void onEntryInfoMultiResponse(String ks, ResponseElement response, OnMediaLoadCompletion completion) throws InterruptedException {
            ErrorElement error = null;
            PKMediaEntry mediaEntry = null;

            if (isCanceled()) {
                log.v(loadId + ": i am canceled, exit response parsing ");
                return;
            }

            if (!isValidResponse(response, completion)) {
                return;
            }

            if (response.isSuccess()) {

                try {
                    //parse multi response from request response

                /* in this option, in case of error response, the type of the parsed response will be BaseResult, and not the expected object type,
                   since we parse the type dynamically from the result and we get "KalturaAPIException" objectType */
                    List<BaseResult> responses = KalturaOvpParser.parse(response.getResponse());//, TextUtils.isEmpty(sessionProvider.getSessionToken()) ? 1 : 0, KalturaBaseEntryListResponse.class, KalturaEntryContextDataResult.class);
                    /* in this option, responses types will always be as expected, and in case of an error, the error can be reached from the typed object, since
                     * all response objects should extend BaseResult */
                    //  List<BaseResult> responses = (List<BaseResult>) KalturaOvpParser.parse(response.getResponse(), KalturaBaseEntryListResponse.class, KalturaEntryContextDataResult.class);
                    if (responses.size() == 0) {
                        error = buildLoadErrorElement("failed to get responses on load requests");

                    } else {
                        // indexes should match the order of requests sent to the server.
                        int entryListResponseIdx = responses.size() > 3 ? 1 : 0;
                        int playbackResponseIdx = entryListResponseIdx + 1;
                        int metadataResponseIdx = playbackResponseIdx + 1;

                        if (responses.get(entryListResponseIdx).error != null) {
                            error = responses.get(entryListResponseIdx).error.addMessage("baseEntry/list request failed");
                        }
                        if (error == null && responses.get(playbackResponseIdx).error != null) {
                            error = responses.get(playbackResponseIdx).error.addMessage("baseEntry/getPlaybackContext request failed");
                        }

                        if (error == null) {
                            KalturaPlaybackContext kalturaPlaybackContext = (KalturaPlaybackContext) responses.get(playbackResponseIdx);
                            KalturaMetadataListResponse metadataList = (KalturaMetadataListResponse) responses.get(metadataResponseIdx);

                            if ((error = kalturaPlaybackContext.hasError()) == null) { // check for error or unauthorized content
                                mediaEntry = ProviderParser.getMediaEntry(sessionProvider.baseUrl(), ks, sessionProvider.partnerId() + "", uiConfId, useApiCaptions,
                                        ((KalturaBaseEntryListResponse) responses.get(entryListResponseIdx)).objects.get(0), kalturaPlaybackContext, metadataList);

                                if (mediaEntry.getSources().size() == 0) { // makes sure there are sources available for play
                                    error = KalturaOvpErrorHelper.getErrorElement("NoFilesFound");
                                }
                            }
                        }
                    }
                } catch (JsonSyntaxException | InvalidParameterException ex) {
                    error = buildLoadErrorElement("failed to create PKMediaEntry: " + ex.getMessage());
                } catch (IndexOutOfBoundsException ex) {
                    error = buildGeneralErrorElement("responses list doesn't contain the expected responses number: " + ex.getMessage());
                }

            } else {
                error = response.getError() != null ? response.getError() : buildLoadErrorElement("error response in multirequest. response: " + response.getResponse());
            }

            log.v(loadId + ": load operation " + (isCanceled() ? "canceled" : "finished with " + (error == null ? "success" : "failure: " + error)));

            if (!isCanceled() && completion != null) {
                completion.onComplete(Accessories.buildResult(mediaEntry, error));
            }

            notifyCompletion();
        }

        private boolean isValidResponse(ResponseElement response, OnMediaLoadCompletion completion) {

            if (isErrorResponse(response)) {
                ErrorElement errorResponse = parseErrorRersponse(response);
                if (errorResponse == null) {
                    errorResponse = buildGeneralErrorElement("multirequest response is null");
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
                    apiExceptionError = buildGeneralErrorElement("multirequest KalturaAPIException");
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

    private static class ProviderParser {

        /**
         * creates {@link PKMediaEntry} from entry's data and contextData
         *
         * @param baseUrl - base url
         * @param entry - {@link KalturaMediaEntry}
         * @param playbackContext - {@link KalturaPlaybackContext}
         * @return (in case of restriction on maxbitrate, filtering should be done by considering the flavors provided to the
         * source - if none meets the restriction, source should not be added to the mediaEntrys sources.)
         */
        public static PKMediaEntry getMediaEntry(String baseUrl, String ks, String partnerId, String uiConfId, boolean useApiCaptions, KalturaMediaEntry entry,
                                                 KalturaPlaybackContext playbackContext, KalturaMetadataListResponse metadataList) throws InvalidParameterException {

            ArrayList<KalturaPlaybackSource> kalturaSources = playbackContext.getSources();
            List<PKMediaSource> sources;


            if (kalturaSources != null && kalturaSources.size() > 0) {
                sources = parseFromSources(baseUrl, ks, partnerId, uiConfId, entry, playbackContext);
            } else {
                sources = new ArrayList<>();
            }

            Map<String, String> metadata = parseMetadata(metadataList);
            populateMetadata(metadata, entry);
            PKMediaEntry mediaEntry = initPKMediaEntry(entry.getTags());

            if (useApiCaptions && playbackContext.getPlaybackCaptions() != null && !playbackContext.getPlaybackCaptions().isEmpty()) {
                List<PKExternalSubtitle> subtitleList = createExternalSubtitles(playbackContext);
                if (!subtitleList.isEmpty()) {
                    mediaEntry.setExternalSubtitleList(subtitleList);
                }
            }

            return mediaEntry.setId(entry.getId()).setSources(sources)
                    .setDuration(entry.getMsDuration())
                    .setMetadata(metadata)
                    .setName(entry.getName())
                    .setMediaType(MediaTypeConverter.toMediaEntryType(entry));
        }

        private static List<PKExternalSubtitle> createExternalSubtitles(KalturaPlaybackContext playbackContext) {
            List<PKExternalSubtitle> subtitleList = new ArrayList<>();
            List<KalturaPlaybackCaption> playbackCaptionList = playbackContext.getPlaybackCaptions();

            for (KalturaPlaybackCaption kalturaPlaybackCaption : playbackCaptionList) {
                if (isValidPlaybackCaption(kalturaPlaybackCaption)) {
                    PKSubtitleFormat subtitleFormat;
                    String subtitleURL = kalturaPlaybackCaption.getUrl();

                    if (KalturaCaptionType.srt.equals(KalturaCaptionType.fromCaptionFormatString(kalturaPlaybackCaption.getFormat()))) {
                        subtitleFormat = PKSubtitleFormat.srt;
                    } else if (KalturaCaptionType.webvtt.equals(KalturaCaptionType.fromCaptionFormatString(kalturaPlaybackCaption.getFormat()))) {
                        subtitleFormat = PKSubtitleFormat.vtt;
                    } else {
                        subtitleURL = kalturaPlaybackCaption.getWebVttUrl();
                        if (subtitleURL == null) {
                            continue;
                        }
                        if (KalturaCaptionType.dfxp.equals(KalturaCaptionType.fromCaptionFormatString(kalturaPlaybackCaption.getFormat())) ||
                                KalturaCaptionType.cap.equals(KalturaCaptionType.fromCaptionFormatString(kalturaPlaybackCaption.getFormat()))) {
                            subtitleFormat = PKSubtitleFormat.vtt;
                        } else {
                            continue;
                        }
                    }

                    PKExternalSubtitle pkExternalSubtitle = new PKExternalSubtitle()
                            .setUrl(subtitleURL)
                            .setMimeType(subtitleFormat)
                            .setLabel(kalturaPlaybackCaption.getLabel())
                            .setLanguage(kalturaPlaybackCaption.getLanguageCode());

                    if (kalturaPlaybackCaption.isDefault()) {
                        pkExternalSubtitle.setDefault();
                    }
                    subtitleList.add(pkExternalSubtitle);
                }
            }
            return subtitleList;
        }

        private static boolean isValidPlaybackCaption(KalturaPlaybackCaption kalturaPlaybackCaption) {
            return !TextUtils.isEmpty(kalturaPlaybackCaption.getUrl()) &&
                    !TextUtils.isEmpty(kalturaPlaybackCaption.getFormat()) &&
                    !TextUtils.isEmpty(kalturaPlaybackCaption.getLabel()) &&
                    !TextUtils.isEmpty(kalturaPlaybackCaption.getLanguageCode());
        }

        private static void populateMetadata(Map<String, String> metadata, KalturaMediaEntry entry) {
            if (entry.hasId()) {
                metadata.put("entryId", entry.getId());
            }
            if (entry.hasName()) {
                metadata.put("name", entry.getName());
            }
            if (entry.hasDescription()) {
                metadata.put("description", entry.getDescription());
            }
            if (entry.hasThumbnail()) {
                metadata.put("thumbnailUrl", entry.getThumbnailUrl());
            }
        }

        private static PKMediaEntry initPKMediaEntry(String tags) {
            PKMediaEntry pkMediaEntry = new PKMediaEntry();
            //If there is '360' tag -> Create VRPKMediaEntry with default VRSettings
            if(tags != null
                    && !tags.isEmpty()
                    && Pattern.compile("\\b360\\b").matcher(tags).find()){
                pkMediaEntry.setIsVRMediaType(true);
            }
            return pkMediaEntry;
        }

        private static Map<String, String> parseMetadata(KalturaMetadataListResponse metadataList) {
            Map<String, String> metadata = new HashMap<>();
            if (metadataList != null && metadataList.objects != null && metadataList.objects.size() > 0) {
                for (KalturaMetadata metadataItem : metadataList.objects) {
                    extractMetadata(metadataItem.xml, metadata);
                }
            }
            return metadata;
        }

        private static void extractMetadata(String xml, Map<String, String> metadataMap) {

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

        /**
         * Parse PKMediaSource objects from the getPlaybackContext API response.
         * Goes over the sources and creates for each supported source (supported format) a correlating
         * PKMediaSource item, initiate with the relevant data.
         *
         * @param baseUrl         - baseUrl for the playing source construction
         * @param ks              - if not empty, will be added to the playing url path
         * @param partnerId       - partnerId
         * @param uiConfId        - if not empty, will be added to the playing url path
         * @param entry           - {@link KalturaMediaEntry}
         * @param playbackContext - the response object of the "baseEntry/getPlaybackContext" API.
         * @return - list of PKMediaSource created from sources list
         */
        @NonNull
        private static List<PKMediaSource> parseFromSources(String baseUrl, String ks, String partnerId, String uiConfId, KalturaMediaEntry entry, KalturaPlaybackContext playbackContext) {
            ArrayList<PKMediaSource> sources = new ArrayList<>();

            //-> create PKMediaSource-s according to sources list provided in "getContextData" response
            for (KalturaPlaybackSource playbackSource : playbackContext.getSources()) {

                if (!FormatsHelper.validateFormat(playbackSource)) { // only validated formats will be added to the sources.
                    continue;
                }

                String playUrl;
                PKMediaFormat mediaFormat = FormatsHelper.getPKMediaFormat(playbackSource.getFormat(), playbackSource.hasDrmData());

                // in case playbackSource doesn't have flavors we don't need to build the url and we'll use the provided one.
                if (playbackSource.hasFlavorIds()) {

                    String baseProtocol;
                    try {
                        baseProtocol = new URL(baseUrl).getProtocol();

                    } catch (MalformedURLException e) {
                        log.e("Provided base url is wrong");
                        baseProtocol = OvpConfigs.DefaultHttpProtocol;
                    }

                    PlaySourceUrlBuilder playUrlBuilder = new PlaySourceUrlBuilder()
                            .setBaseUrl(baseUrl)
                            .setEntryId(entry.getId())
                            .setFlavorIds(playbackSource.getFlavorIds())
                            .setFormat(playbackSource.getFormat())
                            .setKs(ks)
                            .setPartnerId(partnerId)
                            .setUiConfId(uiConfId)
                            .setProtocol(playbackSource.getProtocol(baseProtocol)); //get protocol from base url

                    String extension = "";
                    //-> find out what should be the extension: if playbackSource format doesn't have mapped value, mediaFormat is null,
                    //->  the extension will be fetched from the flavorAssets.
                    if (mediaFormat == null) {
                        // filter the flavors that the playbackSource supports
                        List<KalturaFlavorAsset> flavorAssets = FlavorAssetsFilter.filter(playbackContext.getFlavorAssets(), "id", playbackSource.getFlavorIdsList());
                        if (flavorAssets.size() > 0) {
                            extension = flavorAssets.get(0).getFileExt();
                        }
                    } else {
                        extension = mediaFormat.pathExt;
                    }

                    playUrlBuilder.setExtension(extension);

                    playUrl = playUrlBuilder.build();

                } else {
                    playUrl = playbackSource.getUrl();
                }

                if (playUrl == null) {
                    log.w( "failed to create play url from source, discarding source:" + (entry.getId() + "_" + playbackSource.getDeliveryProfileId()) + ", " + playbackSource.getFormat());
                    continue;
                }

                PKMediaSource pkMediaSource = new PKMediaSource().setUrl(playUrl).setId(entry.getId() + "_" + playbackSource.getDeliveryProfileId()).setMediaFormat(mediaFormat);
                //-> sources with multiple drm data are split to PKMediaSource per drm
                List<KalturaDrmPlaybackPluginData> drmData = playbackSource.getDrmData();
                if (drmData != null && !drmData.isEmpty()) {
                    if (!isDRMSchemeValid(pkMediaSource, drmData)){
                        continue;
                    }
                    updateDrmParams(pkMediaSource, drmData);
                }
                sources.add(pkMediaSource);
            }

            return sources;
        }

        @NonNull
        static List<PKMediaSource> parseFromFlavors(String ks, String partnerId, String uiConfId, KalturaMediaEntry entry, KalturaEntryContextDataResult contextData) {

            ArrayList<PKMediaSource> sources = new ArrayList<>();

            if (contextData != null) {
                //-> filter a list for flavors correspond to the list of "flavorParamsId"s received on the entry data response.
                List<KalturaFlavorAsset> matchingFlavorAssets = FlavorAssetsFilter.filter(contextData.getFlavorAssets(), "flavorParamsId", entry.getFlavorParamsIdsList());

                //-> construct a string of "ids" from the filtered KalturaFlavorAsset list.
                StringBuilder flavorIds = new StringBuilder(matchingFlavorAssets.size() > 0 ? matchingFlavorAssets.get(0).getId() : "");
                for (int i = 1; i < matchingFlavorAssets.size(); i++) {
                    flavorIds.append(",").append(matchingFlavorAssets.get(i).getId());
                }

                if (flavorIds.length() > 0) {
                    //-> create PKMediaSource for every predefine extension:
                    //Collection<PKMediaFormat> extensions = FormatsHelper.getSupportedExtensions();

                    for (Map.Entry<FormatsHelper.StreamFormat, PKMediaFormat> mediaFormatEntry : FormatsHelper.getSupportedFormats().entrySet()/*extensions*/) {
                        String formatName = mediaFormatEntry.getKey().formatName;//FormatsHelper.getFormatNameByMediaFormat(mediaFormat);
                        String playUrl = new PlaySourceUrlBuilder()
                                .setEntryId(entry.getId())
                                .setFlavorIds(flavorIds.toString())
                                .setKs(ks)
                                .setPartnerId(partnerId)
                                .setUiConfId(uiConfId)
                                .setExtension(mediaFormatEntry.getValue().pathExt)
                                .setFormat(formatName).build();

                        PKMediaSource mediaSource = new PKMediaSource().setId(entry.getId() + "_" + mediaFormatEntry.getValue().pathExt).setMediaFormat(mediaFormatEntry.getValue());
                        mediaSource.setUrl(playUrl);
                        sources.add(mediaSource);
                    }
                }
            }

            return sources;
        }


    }

    public static class MediaTypeConverter {

        public static PKMediaEntry.MediaEntryType toMediaEntryType(KalturaMediaEntry entry) {
            if (entry == null) {
                return PKMediaEntry.MediaEntryType.Unknown;
            }

            if (entry.hasDvrStatus()) {
                if (entry.hasDvrStatus() && entry.getDvrStatus() != null) {
                    if(entry.getDvrStatus() == 1) {
                        return PKMediaEntry.MediaEntryType.DvrLive;
                    } else if (entry.getDvrStatus() == 0) {
                        return PKMediaEntry.MediaEntryType.Live;
                    }
                }
            }

            switch (entry.getType()) {
                case MEDIA_CLIP:
                    return PKMediaEntry.MediaEntryType.Vod;
                case LIVE_STREAM:
                    return PKMediaEntry.MediaEntryType.Live;
                default:
                    return PKMediaEntry.MediaEntryType.Unknown;
            }
        }
    }
}
