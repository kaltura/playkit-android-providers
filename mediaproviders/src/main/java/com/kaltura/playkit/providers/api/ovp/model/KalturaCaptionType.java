package com.kaltura.playkit.providers.api.ovp.model;

public enum KalturaCaptionType {
    srt,
    dfxp,
    webvtt,
    cap;

    public static KalturaCaptionType fromCaptionFormatString(String captionFormat) {
        switch (captionFormat) {
            case "1": return srt;
            case "2": return dfxp;
            case "3": return webvtt;
            case "4": return cap;
            default: return null;
        }
    }
}
