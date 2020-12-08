package com.kaltura.playkit.providers.ott;

import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.kaltura.netkit.connect.response.BaseResult;
import com.kaltura.netkit.connect.response.ResponseElement;
import com.kaltura.netkit.utils.ErrorElement;
import com.kaltura.playkit.PKLog;
import com.kaltura.playkit.PKMediaEntry;
import com.kaltura.playkit.providers.api.phoenix.APIDefines;
import com.kaltura.playkit.providers.api.phoenix.PhoenixErrorHelper;
import com.kaltura.playkit.providers.api.phoenix.model.KalturaMediaAsset;
import com.kaltura.playkit.providers.api.phoenix.model.KalturaRecordingAsset;
import com.kaltura.playkit.providers.api.phoenix.model.KalturaRecordingType;
import com.kaltura.playkit.providers.api.phoenix.model.KalturaThumbnail;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class PhoenixProviderUtils {
    private static final PKLog log = PKLog.get("PhoenixProviderUtils");

    private static String LIVE_ASSET_OBJECT_TYPE = "KalturaLiveAsset"; //Might be needed to support in KalturaProgramAsset for EPG

    public static final String KALTURA_API_EXCEPTION = "KalturaAPIException";
    public static final String ERROR = "error";
    public static final String OBJECT_TYPE = "objectType";
    public static final String CODE = "code";
    public static final String MESSAGE = "message";
    public static final String RESULT = "result";

    static boolean isAPIExceptionResponse(ResponseElement response) {
        return response == null || (response.isSuccess() && response.getError() == null && response.getResponse() != null && response.getResponse().contains(KALTURA_API_EXCEPTION));
    }

    static boolean isErrorResponse(ResponseElement response) {
        return response == null || (!response.isSuccess() && response.getError() != null);
    }

    static ErrorElement parseErrorRersponse(ResponseElement response) {
        if (response != null) {
            return response.getError();
        }
        return null;
    }

    static ErrorElement parseAPIExceptionError(ResponseElement response) {

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

    static boolean is360Supported(Map<String, String> metadata) {
        return ("360".equals(metadata.get("tags")));
    }

    @NonNull
    static Map<String, String> createOttMetadata(KalturaMediaAsset kalturaMediaAsset) {
        Map<String, String> metadata = new HashMap<>();
        if (kalturaMediaAsset == null) {
            return metadata;
        }

        JsonObject tags = kalturaMediaAsset.getTags();
        if (tags != null) {
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
        }

        JsonObject metas = kalturaMediaAsset.getMetas();
        if (metas != null) {
            for (Map.Entry<String, JsonElement> entry : metas.entrySet()) {
                metadata.put(entry.getKey(), safeGetValue(entry.getValue()));
            }
        }

        if (kalturaMediaAsset.getImages() != null) {
            for (KalturaThumbnail image : kalturaMediaAsset.getImages()) {
                if (image != null && image.getUrl() != null) {
                    if (image.getWidth() != null && image.getHeight() != null) {
                        metadata.put(image.getWidth() + "X" + image.getHeight(), image.getUrl());
                    } else if (image.getRatio() != null) {
                        metadata.put(image.getRatio(), image.getUrl());
                    }
                }
            }
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
            String recordingId = ((KalturaRecordingAsset) kalturaMediaAsset).getRecordingId();
            if (recordingId != null){
                metadata.put("recordingId", recordingId);
            }

            KalturaRecordingType recordingType = ((KalturaRecordingAsset) kalturaMediaAsset).getRecordingType();
            if (recordingType != null) {
                metadata.put("recordingType", recordingType.name());
            }
        }
        return metadata;
    }

    static String safeGetValue(JsonElement value) {
        if (value == null || value.isJsonNull() || !value.isJsonObject()) {
            return null;
        }

        final JsonElement valueElement = value.getAsJsonObject().get("value");
        return (valueElement != null && !valueElement.isJsonNull()) ? valueElement.getAsString() : null;
    }

    static ErrorElement updateErrorElement(ResponseElement response, BaseResult loginResult, BaseResult playbackContextResult, BaseResult assetGetResult) {
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

    static boolean isDvrLiveMediaEntry(KalturaMediaAsset kalturaMediaAsset, OTTMediaAsset mediaAsset) {

        if (LIVE_ASSET_OBJECT_TYPE.equals(kalturaMediaAsset.getObjectType()) && kalturaMediaAsset.getEnableTrickPlay()) {
            return true;
        }
        return mediaAsset.assetType == APIDefines.KalturaAssetType.Epg && mediaAsset.contextType == APIDefines.PlaybackContextType.StartOver;
    }

    static boolean isLiveMediaEntry(KalturaMediaAsset kalturaMediaAsset) {
        if (kalturaMediaAsset == null) {
            return false;
        }

        String externalIdsStr = kalturaMediaAsset.getExternalIds();
        return (LIVE_ASSET_OBJECT_TYPE.equals(kalturaMediaAsset.getObjectType()) ||
                !TextUtils.isEmpty(externalIdsStr) && TextUtils.isDigitsOnly(externalIdsStr) && Long.valueOf(externalIdsStr) != 0);
    }

    static boolean isRecordingMediaEntry(KalturaMediaAsset kalturaMediaAsset) {
        return kalturaMediaAsset instanceof KalturaRecordingAsset;
    }

    static class MediaTypeConverter {

        public static PKMediaEntry.MediaEntryType toMediaEntryType(String mediaType) {
            switch (mediaType) {
                default:
                    return PKMediaEntry.MediaEntryType.Unknown;
            }
        }
    }
}
