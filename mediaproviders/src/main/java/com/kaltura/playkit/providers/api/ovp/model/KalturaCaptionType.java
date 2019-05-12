package com.kaltura.playkit.providers.api.ovp.model;

public enum KalturaCaptionType {
    srt("1"),
    dfxp("2"),
    webvtt("3"),
    cap("4");


    private final String captionType;

    private KalturaCaptionType(String captionType) {
        this.captionType = captionType;
    }

    public String getCaptionType() {
        return captionType;
    }
}
