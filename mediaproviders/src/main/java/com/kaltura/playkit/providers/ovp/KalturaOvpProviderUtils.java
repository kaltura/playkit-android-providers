package com.kaltura.playkit.providers.ovp;

import android.net.UrlQuerySanitizer;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.kaltura.netkit.connect.response.ResponseElement;
import com.kaltura.netkit.utils.ErrorElement;
import com.kaltura.playkit.PKLog;
import com.kaltura.playkit.PKMediaEntry;
import com.kaltura.playkit.PKMediaFormat;
import com.kaltura.playkit.PKMediaSource;
import com.kaltura.playkit.PKSubtitleFormat;
import com.kaltura.playkit.player.PKExternalSubtitle;
import com.kaltura.playkit.providers.api.ovp.model.FlavorAssetsFilter;
import com.kaltura.playkit.providers.api.ovp.model.KalturaCaptionType;
import com.kaltura.playkit.providers.api.ovp.model.KalturaEntryContextDataResult;
import com.kaltura.playkit.providers.api.ovp.model.KalturaFlavorAsset;
import com.kaltura.playkit.providers.api.ovp.model.KalturaMediaEntry;
import com.kaltura.playkit.providers.api.ovp.model.KalturaMetadata;
import com.kaltura.playkit.providers.api.ovp.model.KalturaMetadataListResponse;
import com.kaltura.playkit.providers.api.ovp.model.KalturaPlaybackCaption;
import com.kaltura.playkit.providers.api.ovp.model.KalturaPlaybackContext;
import com.kaltura.playkit.providers.base.FormatsHelper;

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
import java.util.regex.Pattern;

public class KalturaOvpProviderUtils {

    private static final PKLog log = PKLog.get("KalturaOvpProviderUtils");

    public static final String KALTURA_API_EXCEPTION = "KalturaAPIException";
    public static final String OBJECT_TYPE = "objectType";
    public static final String CODE = "code";
    public static final String MESSAGE = "message";

    static void extractMetadata(String xml, Map<String, String> metadataMap) {

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

    static String getDefaultWidgetId(int partnerId) {
        return "_" + partnerId;
    }

    static boolean isAPIExceptionResponse(ResponseElement response) {
        return response == null|| (response.isSuccess() && response.getError() == null && response.getResponse() != null && response.getResponse().contains(KALTURA_API_EXCEPTION));
    }

    static boolean isErrorResponse(ResponseElement response) {
        return response == null|| (!response.isSuccess() && response.getError() != null);
    }


    static ErrorElement parseErrorRersponse(ResponseElement response) {
        if (response != null) {
            return response.getError();
        }
        return null;
    }

    static ErrorElement parseAPIExceptionError(ResponseElement response) {

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

    static Map<String,String> getAPIExceptionData(JSONObject error) {

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

    static List<PKExternalSubtitle> createExternalSubtitles(KalturaPlaybackContext playbackContext, String ks) {
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

                subtitleURL = appendUserKS(subtitleURL, ks);

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

    protected static String appendUserKS(String url, String ks) {
        if (!TextUtils.isEmpty(url) && !TextUtils.isEmpty(ks)) {
            UrlQuerySanitizer urlQuerySanitizer = new UrlQuerySanitizer(url);
            if (urlQuerySanitizer.getParameterList().isEmpty()) {
                url += "/ks/" + ks;
            } else {
                url += "&ks=" + ks;
            }
        }
        return url;
    }

    static boolean isValidPlaybackCaption(KalturaPlaybackCaption kalturaPlaybackCaption) {
        return !TextUtils.isEmpty(kalturaPlaybackCaption.getUrl()) &&
                !TextUtils.isEmpty(kalturaPlaybackCaption.getFormat()) &&
                !TextUtils.isEmpty(kalturaPlaybackCaption.getLabel()) &&
                !TextUtils.isEmpty(kalturaPlaybackCaption.getLanguageCode());
    }

    static void populateMetadata(Map<String, String> metadata, KalturaMediaEntry entry) {
        if (entry.hasId()) {
            metadata.put("entryId", entry.getId());
            metadata.put("mediaAssetUUID", entry.getId());
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

    static PKMediaEntry initPKMediaEntry(String tags) {
        PKMediaEntry pkMediaEntry = new PKMediaEntry();
        //If there is '360' tag -> Create VRPKMediaEntry with default VRSettings
        if(tags != null
                && !tags.isEmpty()
                && Pattern.compile("\\b360\\b").matcher(tags).find()){
            pkMediaEntry.setIsVRMediaType(true);
        }
        return pkMediaEntry;
    }

    static Map<String, String> parseMetadata(KalturaMetadataListResponse metadataList) {
        Map<String, String> metadata = new HashMap<>();
        if (metadataList != null && metadataList.objects != null && metadataList.objects.size() > 0) {
            for (KalturaMetadata metadataItem : metadataList.objects) {
                extractMetadata(metadataItem.xml, metadata);
            }
        }
        return metadata;
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

    static PKMediaEntry.MediaEntryType getMediaEntryType(KalturaMediaEntry kalturaMediaEntry) {
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
}
