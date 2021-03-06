package com.kaltura.playkit.providers;

import com.kaltura.netkit.utils.ErrorElement;
import com.kaltura.playkit.PKDrmParams;
import com.kaltura.playkit.PKMediaSource;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.kaltura.playkit.providers.api.base.model.KalturaDrmPlaybackPluginData;

import static com.kaltura.playkit.PKDrmParams.Scheme.PlayReadyCENC;
import static com.kaltura.playkit.PKDrmParams.Scheme.Unknown;
import static com.kaltura.playkit.PKDrmParams.Scheme.WidevineCENC;
import static com.kaltura.playkit.PKDrmParams.Scheme.WidevineClassic;

public class MediaProvidersUtils {

    public static boolean isDRMSchemeValid(PKMediaSource pkMediaSource, List<KalturaDrmPlaybackPluginData> drmData) {
        if (drmData == null) {
            return false;
        }

        Iterator<KalturaDrmPlaybackPluginData> drmDataItr = drmData.iterator();
        while(drmDataItr.hasNext()) {
            KalturaDrmPlaybackPluginData drmDataItem = drmDataItr.next();
            if (getScheme(drmDataItem.getScheme()) == Unknown) {
                drmDataItr.remove();
            }
        }
        return !drmData.isEmpty();
    }

    public static void updateDrmParams(PKMediaSource pkMediaSource, List<KalturaDrmPlaybackPluginData> drmData) {
        List<PKDrmParams> drmParams = new ArrayList<>();
        for (KalturaDrmPlaybackPluginData drm : drmData) {
            PKDrmParams.Scheme drmScheme = getScheme(drm.getScheme());
            drmParams.add(new PKDrmParams(drm.getLicenseURL(), drmScheme));
        }
        pkMediaSource.setDrmData(drmParams);
    }

    public static PKDrmParams.Scheme getScheme(String name) {

        switch (name) {
            case "WIDEVINE_CENC":
            case "drm.WIDEVINE_CENC":
                return WidevineCENC;
            case "PLAYREADY_CENC":
            case "drm.PLAYREADY_CENC":
                return PlayReadyCENC;
            case "WIDEVINE":
            case "widevine.WIDEVINE":
                return WidevineClassic;
            default:
                return Unknown;
        }
    }

    public static ErrorElement buildGeneralErrorElement(String message) {
        return new ErrorElement(ErrorElement.GeneralError.getName(), message, ErrorElement.ErrorCode.GeneralErrorCode);
    }

    public static ErrorElement buildLoadErrorElement(String message) {
        return new ErrorElement(ErrorElement.LoadError.getName(), message,ErrorElement.ErrorCode.LoadErrorCode);
    }

    public static ErrorElement buildNotFoundlErrorElement(String message) {
        return new ErrorElement(ErrorElement.NotFound.getName(), message, ErrorElement.ErrorCode.NotFoundCode);
    }

    public static ErrorElement buildBadRequestErrorElement(String message) {
        return new ErrorElement(ErrorElement.BadRequestError.getName(), message, ErrorElement.ErrorCode.BadRequestErrorCode);
    }
}
