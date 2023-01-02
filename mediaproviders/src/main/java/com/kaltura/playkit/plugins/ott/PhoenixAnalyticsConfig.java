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

package com.kaltura.playkit.plugins.ott;

import android.text.TextUtils;

import com.google.gson.JsonObject;

/**
 * Created by gilad.nadav on 22/06/2017.
 */

public class PhoenixAnalyticsConfig {
    public static final String PARTNER_ID = "partnerId";
    public static final String BASE_URL   = "baseUrl";
    public static final String KS         = "ks";
    public static final String TIMER_INTERVAL = "timerInterval";
    public static final String DISABLE_MEDIAHIT = "disableMediaHit";
    public static final String DISABLE_MEDIAMARK = "disableMediaMark";
    public static final String EPG_ID = "epgId";
    public static final String EXPERIMENTAL_MEDIA_HIT = "experimentalLiveMediaHit";
    
    private int partnerId;
    private String baseUrl;
    private String ks;
    private int timerInterval; // In Seconds
    private boolean disableMediaHit;
    private boolean disableMediaMark;
    private String epgId;
    private boolean experimentalLiveMediaHit;
    private boolean forceConcurrencyOnUnpaidContent;

    public PhoenixAnalyticsConfig() {}

    public PhoenixAnalyticsConfig(int partnerId, String baseUrl, String ks, int timerInterval) {
        this.partnerId = partnerId;
        this.baseUrl = baseUrl;
        this.ks = ks;
        this.timerInterval = timerInterval;
        this.disableMediaHit = false;
        this.disableMediaMark = false;
        this.epgId = null;
        this.experimentalLiveMediaHit = false;
    }

    public PhoenixAnalyticsConfig(int partnerId, String baseUrl, String ks, int timerInterval, boolean disableMediaHit, boolean disableMediaMark) {
        this.partnerId = partnerId;
        this.baseUrl = baseUrl;
        this.ks = ks;
        this.timerInterval = timerInterval;
        this.disableMediaHit = disableMediaHit;
        this.disableMediaMark = disableMediaMark;
        this.epgId = null;
        this.experimentalLiveMediaHit = false;
    }

    public PhoenixAnalyticsConfig(int partnerId, String baseUrl, String ks, int timerInterval, String epgId) {
        this.partnerId = partnerId;
        this.baseUrl = baseUrl;
        this.ks = ks;
        this.timerInterval = timerInterval;
        this.epgId = epgId;
        this.disableMediaHit = false;
        this.disableMediaMark = false;
        this.experimentalLiveMediaHit = false;

    }

    public PhoenixAnalyticsConfig(int partnerId, String baseUrl, String ks, int timerInterval, boolean disableMediaHit, boolean disableMediaMark, String epgId) {
        this.partnerId = partnerId;
        this.baseUrl = baseUrl;
        this.ks = ks;
        this.timerInterval = timerInterval;
        this.disableMediaHit = disableMediaHit;
        this.disableMediaMark = disableMediaMark;
        this.epgId = epgId;
        this.experimentalLiveMediaHit = false;
    }

    public PhoenixAnalyticsConfig setPartnerId(int partnerId) {
        this.partnerId = partnerId;
        return this;
    }

    public PhoenixAnalyticsConfig setKS(String ks) {
        this.ks = ks;
        return this;
    }

    public PhoenixAnalyticsConfig setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
        return this;
    }

    public PhoenixAnalyticsConfig setTimerInterval(int timerInterval) {
        this.timerInterval = timerInterval;
        return this;
    }

    public PhoenixAnalyticsConfig setDisableMediaHit(boolean disableMediaHit) {
        this.disableMediaHit = disableMediaHit;
        return this;
    }

    public PhoenixAnalyticsConfig setDisableMediaMark(boolean disableMediaMark) {
        this.disableMediaMark = disableMediaMark;
        return this;
    }

    public PhoenixAnalyticsConfig setEpgId(String epgId) {
        this.epgId = epgId;
        return this;
    }

    public PhoenixAnalyticsConfig setExperimentalLiveMediaHit(boolean experimentalLiveMediaHit) {
        this.experimentalLiveMediaHit = experimentalLiveMediaHit;
        return this;
    }

    public PhoenixAnalyticsConfig setForceConcurrencyOnUnpaidContent(boolean forceConcurrencyOnUnpaidContent) {
        this.forceConcurrencyOnUnpaidContent = forceConcurrencyOnUnpaidContent;
        return this;
    }

    public int getPartnerId() {
        return partnerId;
    }

    public String getKS() {
        return ks;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public int getTimerInterval() {
        return timerInterval;
    }

    public boolean getDisableMediaHit() {
        return disableMediaHit;
    }

    public boolean getDisableMediaMark() {
        return disableMediaMark;
    }

    public String getEpgId() {
        return epgId;
    }

    public boolean getExperimentalLiveMediaHit() {
        return experimentalLiveMediaHit;
    }

    public boolean isForceConcurrencyOnUnpaidContent() {
        return forceConcurrencyOnUnpaidContent;
    }

    public JsonObject toJson() {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty(PARTNER_ID, partnerId);
        jsonObject.addProperty(BASE_URL, baseUrl);
        jsonObject.addProperty(KS, ks == null ? "" : ks);
        jsonObject.addProperty(TIMER_INTERVAL, timerInterval);
        jsonObject.addProperty(DISABLE_MEDIAHIT, disableMediaHit);
        jsonObject.addProperty(DISABLE_MEDIAMARK, disableMediaMark);
        if (!TextUtils.isEmpty(epgId)) {
            jsonObject.addProperty(EPG_ID, epgId);
        }
        jsonObject.addProperty(EXPERIMENTAL_MEDIA_HIT, experimentalLiveMediaHit);
        return jsonObject;
    }
}
