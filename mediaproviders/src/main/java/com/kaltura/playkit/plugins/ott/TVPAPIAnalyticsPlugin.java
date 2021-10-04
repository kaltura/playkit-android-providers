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

import android.content.Context;

import com.google.gson.JsonObject;
import com.kaltura.netkit.connect.executor.APIOkRequestsExecutor;
import com.kaltura.netkit.connect.request.RequestBuilder;
import com.kaltura.playkit.BuildConfig;
import com.kaltura.playkit.MessageBus;
import com.kaltura.playkit.PKLog;
import com.kaltura.playkit.PKPlugin;
import com.kaltura.playkit.Player;
import com.kaltura.playkit.providers.api.tvpapi.services.MediaMarkService;
import com.kaltura.playkit.utils.Consts;

import java.util.Locale;
import java.util.Timer;

public class TVPAPIAnalyticsPlugin extends PhoenixAnalyticsPlugin {
    private static final PKLog log = PKLog.get("TVPAPIAnalyticsPlugin");
    private JsonObject initObject;

    public static final Factory factory = new Factory() {
        @Override
        public String getName() {
            return "TVPAPIAnalytics";
        }

        @Override
        public PKPlugin newInstance() {
            return new TVPAPIAnalyticsPlugin();
        }

        @Override
        public String getVersion() {
            return BuildConfig.VERSION_NAME;
        }

        @Override
        public void warmUp(Context context) {

        }
    };

    @Override
    protected void onLoad(Player player, Object config, final MessageBus messageBus, Context context) {
        log.d("onLoad");
        setPluginMembers(config);
        this.player = player;
        this.context = context;
        this.messageBus = messageBus;

        this.timer = new Timer();
        this.requestsExecutor = APIOkRequestsExecutor.getSingleton();
        if (baseUrl != null && !baseUrl.isEmpty() &&  initObject != null) {
            addListeners();
        } else {
            log.e("Error, base url/initObj - incorrect");
        }
    }

    private void setPluginMembers(Object config) {
        TVPAPIAnalyticsConfig pluginConfig = parseConfig(config);
        if (pluginConfig != null) {
            this.baseUrl = pluginConfig.getBaseUrl();
            this.initObject = pluginConfig.getInitObject();
            long timerInterval = Consts.DEFAULT_ANALYTICS_TIMER_INTERVAL_HIGH;
            int timerIntervalSec = pluginConfig.getTimerInterval();
            if (timerIntervalSec > 0) {
                timerInterval = timerIntervalSec * Consts.MILLISECONDS_MULTIPLIER;
            }
            this.mediaHitInterval = (int) timerInterval;
        }
    }



    @Override
    protected void onUpdateConfig(Object config) {
        setPluginMembers(config);
        if (baseUrl == null || baseUrl.isEmpty() || initObject == null) {
            cancelTimer();
            messageBus.removeListeners(this);
        }
    }

    /**
     * Send Bookmark/add event using Kaltura Phoenix Rest API
     * @param eventType - Enum stating the event type to send
     */
    @Override
    protected void sendAnalyticsEvent(final PhoenixActionType eventType){
        String method = eventType == PhoenixActionType.HIT ? "MediaHit": "MediaMark";
        String action = eventType.name().toLowerCase(Locale.ENGLISH);
        log.d("TVPAPIAnalyticsPlugin sendAnalyticsEvent " + eventType + ", method = " + method + ", action = " + action);

        if (initObject == null) {
            return;
        }

        if (mediaConfig == null || mediaConfig.getMediaEntry() == null || mediaConfig.getMediaEntry().getId() == null) {
            log.e("Error mediaConfig is not valid");
            return;
        }
        RequestBuilder requestBuilder = MediaMarkService.sendTVPAPIEvent(baseUrl + "m=" + method, initObject, action,
                currentMediaId, fileId, lastKnownPlayerPosition);

        requestBuilder.completion(response -> {
            if (response.isSuccess() && response.getResponse().toLowerCase(Locale.ENGLISH).contains("concurrent")){
                messageBus.post(new OttEvent(OttEvent.OttEventType.Concurrency));
                messageBus.post(new TVPAPIAnalyticsEvent.TVPAPIAnalyticsReport(eventType.toString()));
                log.d("onComplete send event: " + eventType);
            }
        });
        requestsExecutor.queue(requestBuilder.build());
    }

    private static TVPAPIAnalyticsConfig parseConfig(Object config) {
        if (config instanceof TVPAPIAnalyticsConfig) {
            return ((TVPAPIAnalyticsConfig) config);

        } else if (config instanceof JsonObject) {
            JsonObject jsonConfig = (JsonObject) config;
            String baseUrl = jsonConfig.get("baseUrl").getAsString();
            int timerInterval = jsonConfig.get("timerInterval").getAsInt();
            JsonObject initObj = jsonConfig.getAsJsonObject("initObj");
            return new TVPAPIAnalyticsConfig(baseUrl, timerInterval, initObj);
        }
        return null;
    }
}
