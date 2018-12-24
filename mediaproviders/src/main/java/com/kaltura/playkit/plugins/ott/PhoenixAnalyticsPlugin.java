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
import android.text.TextUtils;

import com.google.gson.JsonObject;
import com.kaltura.netkit.connect.executor.APIOkRequestsExecutor;
import com.kaltura.netkit.connect.executor.RequestQueue;
import com.kaltura.netkit.connect.request.RequestBuilder;
import com.kaltura.netkit.connect.response.ResponseElement;
import com.kaltura.netkit.utils.OnRequestCompletion;
import com.kaltura.playkit.BuildConfig;
import com.kaltura.playkit.MessageBus;
import com.kaltura.playkit.PKEvent;
import com.kaltura.playkit.PKLog;
import com.kaltura.playkit.PKMediaConfig;
import com.kaltura.playkit.PKPlugin;
import com.kaltura.playkit.Player;
import com.kaltura.playkit.PlayerEvent;
import com.kaltura.playkit.plugins.ads.AdEvent;
import com.kaltura.playkit.providers.api.phoenix.services.BookmarkService;
import com.kaltura.playkit.utils.Consts;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Timer;
import java.util.TimerTask;

public class PhoenixAnalyticsPlugin extends PKPlugin {
    private static final PKLog log = PKLog.get("PhoenixAnalyticsPlugin");
    private static final double MEDIA_ENDED_THRESHOLD = 0.98;
    public static final String CONCURRENCY_ERROR_COODE = "4001";

    // Fields shared with TVPAPIAnalyticsPlugin
    int mediaHitInterval;
    Timer timer;
    Player player;
    Context context;
    MessageBus messageBus;
    PKMediaConfig mediaConfig;
    RequestQueue requestsExecutor;

    String fileId;
    String currentMediaId = "UnKnown";
    String baseUrl;
    long lastKnownPlayerPosition = 0;
    long lastKnownPlayerDuration = 0;
    private boolean isAdPlaying;

    private String ks;
    private int partnerId;
    private boolean playEventWasFired;
    private boolean intervalOn = false;
    private boolean isFirstPlay = true;
    private boolean isMediaFinished = false;


    enum PhoenixActionType {
        HIT,
        PLAY,
        STOP,
        PAUSE,
        FIRST_PLAY,
        SWOOSH,
        LOAD,
        FINISH,
        BITRATE_CHANGE,
        ERROR
    }

    public static final Factory factory = new Factory() {
        @Override
        public String getName() {
            return "PhoenixAnalytics";
        }

        @Override
        public PKPlugin newInstance() {
            return new PhoenixAnalyticsPlugin();
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

        this.requestsExecutor = APIOkRequestsExecutor.getSingleton();
        this.player = player;
        this.context = context;
        this.messageBus = messageBus;
        this.timer = new Timer();
        setConfigMembers(config);
        if (baseUrl != null && !baseUrl.isEmpty() && partnerId > 0) {
            messageBus.listen(mEventListener, PlayerEvent.Type.PLAY, PlayerEvent.Type.PAUSE, PlayerEvent.Type.PLAYING, PlayerEvent.Type.PLAYHEAD_UPDATED, PlayerEvent.Type.ENDED, PlayerEvent.Type.ERROR, PlayerEvent.Type.STOPPED, PlayerEvent.Type.REPLAY, PlayerEvent.Type.SEEKED, PlayerEvent.Type.SOURCE_SELECTED, AdEvent.Type.CONTENT_PAUSE_REQUESTED, AdEvent.Type.CONTENT_RESUME_REQUESTED);
        } else {
            log.e("Error, base url/partner - incorrect");
        }
    }

    private void setConfigMembers(Object config) {
        PhoenixAnalyticsConfig pluginConfig = parseConfig(config);
        if (pluginConfig == null) {
            log.e("Error, pluginConfig == null");
            return;
        }
        this.baseUrl = pluginConfig.getBaseUrl();
        this.partnerId = pluginConfig.getPartnerId();
        this.ks = pluginConfig.getKS();
        this.mediaHitInterval = (pluginConfig.getTimerInterval() > 0) ? pluginConfig.getTimerInterval() * (int) Consts.MILLISECONDS_MULTIPLIER : Consts.DEFAULT_ANALYTICS_TIMER_INTERVAL_HIGH;
    }

    @Override
    protected void onUpdateMedia(PKMediaConfig mediaConfig) {
        this.mediaConfig = mediaConfig;
        isFirstPlay = true;
        playEventWasFired = false;
        isMediaFinished = false;
    }

    @Override
    protected void onUpdateConfig(Object config) {
        setConfigMembers(config);
        if (baseUrl == null || baseUrl.isEmpty() || partnerId <= 0) {
            cancelTimer();
            messageBus.remove(mEventListener, (Enum[]) PlayerEvent.Type.values());
        }
    }

    @Override
    protected void onApplicationPaused() {
        log.d("PhoenixAnalyticsPlugin onApplicationPaused");
        if (player != null) {
            long playerPosOnPause = player.getCurrentPosition();
            if (playerPosOnPause > 0 && !isAdPlaying) {
                lastKnownPlayerPosition = playerPosOnPause / Consts.MILLISECONDS_MULTIPLIER;
            }
        }
        cancelTimer();
    }

    @Override
    protected void onApplicationResumed() {
        log.d("PhoenixAnalyticsPlugin onApplicationResumed");
        if (!isAdPlaying) {
            startMediaHitInterval();
        }
    }

    @Override
    public void onDestroy() {
        log.d("onDestroy");
        cancelTimer();
    }

    private PKEvent.Listener mEventListener = new PKEvent.Listener() {
        @Override
        public void onEvent(PKEvent event) {
            if (event instanceof AdEvent) {
                log.d("Ad Event = " + ((AdEvent) event).type.name() + ", lastKnownPlayerPosition = " + lastKnownPlayerPosition);
                switch (((AdEvent) event).type) {
                    case CONTENT_PAUSE_REQUESTED:
                        isAdPlaying = true;
                        break;
                    case CONTENT_RESUME_REQUESTED:
                        isAdPlaying = false;
                        break;
                    default:
                        break;
                }
            }
            if (event instanceof PlayerEvent) {
                if (event.eventType() != PlayerEvent.Type.PLAYHEAD_UPDATED) {
                    log.d("Player Event = " + ((PlayerEvent) event).type.name() + ", lastKnownPlayerPosition = " + lastKnownPlayerPosition);
                }
                switch (((PlayerEvent) event).type) {
                    case PLAYHEAD_UPDATED:
                        if (!isAdPlaying) {
                            PlayerEvent.PlayheadUpdated playheadUpdated = (PlayerEvent.PlayheadUpdated) event;
                            if (playheadUpdated != null && playheadUpdated.position > 0) {
                                lastKnownPlayerPosition = playheadUpdated.position / Consts.MILLISECONDS_MULTIPLIER;
                                lastKnownPlayerDuration = playheadUpdated.duration / Consts.MILLISECONDS_MULTIPLIER;
                            }
                        }
                        break;
                    case STOPPED:
                        if (isMediaFinished) {
                            return;
                        }
                        isAdPlaying = false;
                        sendAnalyticsEvent(PhoenixActionType.STOP);
                        resetTimer();
                        break;
                    case ENDED:
                        resetTimer();
                        sendAnalyticsEvent(PhoenixActionType.FINISH);
                        playEventWasFired = false;
                        isMediaFinished = true;
                        isFirstPlay = true;
                        break;
                    case ERROR:
                        resetTimer();
                        sendAnalyticsEvent(PhoenixActionType.ERROR);
                        break;
                    case SOURCE_SELECTED:
                        PlayerEvent.SourceSelected sourceSelected = (PlayerEvent.SourceSelected) event;
                        fileId = sourceSelected.source.getId();

                        if (mediaConfig != null && mediaConfig.getMediaEntry() != null) {
                            currentMediaId = mediaConfig.getMediaEntry().getId();
                        }
                        lastKnownPlayerPosition = 0;
                        if (mediaConfig != null && mediaConfig.getStartPosition() != null) {
                            lastKnownPlayerPosition = mediaConfig.getStartPosition();
                        }
                        sendAnalyticsEvent(PhoenixActionType.LOAD);
                        break;
                    case PAUSE:
                        if (isMediaFinished) {
                            return;
                        }
                        if (playEventWasFired) {
                            sendAnalyticsEvent(PhoenixActionType.PAUSE);
                            playEventWasFired = false;
                        }
                        resetTimer();
                        break;
                    case PLAY:
                        if (isMediaFinished) {
                            return;
                        }
                        if (isFirstPlay) {
                            playEventWasFired = true;
                            sendAnalyticsEvent(PhoenixActionType.FIRST_PLAY);
                            sendAnalyticsEvent(PhoenixActionType.HIT);
                        }
                        if (!intervalOn) {
                            startMediaHitInterval();
                        }
                        break;
                    case PLAYING:
                        isMediaFinished = false;
                        if (!isFirstPlay && !playEventWasFired) {
                            sendAnalyticsEvent(PhoenixActionType.PLAY);
                            playEventWasFired = true;
                        } else {
                            isFirstPlay = false;
                        }
                        isAdPlaying = false;
                        break;
                    case SEEKED:
                    case REPLAY:
                        //Receiving one of this events, mean that media position was reset.
                        isMediaFinished = false;
                        break;
                    default:
                        break;
                }
            }
        }
    };

    void cancelTimer() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
        intervalOn = false;
    }

    private void resetTimer() {
        cancelTimer();
        timer = new Timer();
    }

    /**
     * Media Hit analytics event
     */
    private void startMediaHitInterval() {
        log.d("startMediaHitInterval - Timer");
        if (timer == null) {
            timer = new Timer();
        }
        intervalOn = true;
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                sendAnalyticsEvent(PhoenixActionType.HIT);
//                if (player.getCurrentPosition() > 0 && !isAdPlaying) {
//                    lastKnownPlayerPosition = player.getCurrentPosition() / Consts.MILLISECONDS_MULTIPLIER;
//                }
                if (lastKnownPlayerPosition > 0 && ((float) lastKnownPlayerPosition / lastKnownPlayerDuration > MEDIA_ENDED_THRESHOLD)) {
                    sendAnalyticsEvent(PhoenixActionType.FINISH);
                    playEventWasFired = false;
                    isMediaFinished = true;
                    isFirstPlay = true;
                }
            }
        }, mediaHitInterval, mediaHitInterval); // Get media hit interval from plugin config
    }

    /**
     * Send Bookmark/add event using Kaltura Phoenix Rest API
     *
     * @param eventType - Enum stating the event type to send
     */
    protected void sendAnalyticsEvent(final PhoenixActionType eventType) {

        if (TextUtils.isEmpty(this.ks)) {
            log.w("Blocking AnalyticsEvent KS is not valid");
            return;
        }

        if (isAdPlaying && (eventType != PhoenixActionType.STOP && eventType != PhoenixActionType.FINISH)) {
            log.d("Blocking AnalyticsEvent: " + eventType + " while ad is playing");
            return;
        }

        if (mediaConfig == null || mediaConfig.getMediaEntry() == null || mediaConfig.getMediaEntry().getId() == null) {
            log.e("Error mediaConfig is not valid");
            return;
        }
        if (eventType == PhoenixActionType.FINISH) {
            lastKnownPlayerPosition = lastKnownPlayerDuration;
        }
        log.d("PhoenixAnalyticsPlugin sendAnalyticsEvent " + eventType + " isAdPlaying " + isAdPlaying + " position = " + lastKnownPlayerPosition);

        RequestBuilder requestBuilder = BookmarkService.actionAdd(baseUrl, partnerId, ks,
                "media", currentMediaId, eventType.name(), lastKnownPlayerPosition, fileId);

        requestBuilder.completion(new OnRequestCompletion() {
            @Override
            public void onComplete(ResponseElement response) {
                log.d("onComplete send event: " + eventType);
                if (response == null) {
                    return;
                }

                if (response.getError() != null) { // in case of error from Derver side
                    sendGenericErrorEvent(response, eventType);
                } else {
                    if (response.isSuccess() && response.getError() == null && response.getResponse() != null && response.getResponse().contains("KalturaAPIException")) {
                        sendAPIExceptionErrorEvent(response, eventType);
                        messageBus.post(new PhoenixAnalyticsEvent.PhoenixAnalyticsReport(eventType.toString() + " Failed"));
                    } else {
                        messageBus.post(new PhoenixAnalyticsEvent.PhoenixAnalyticsReport(eventType.toString()));
                    }
                }
            }
        });
        requestsExecutor.queue(requestBuilder.build());
    }

    private void sendGenericErrorEvent(ResponseElement response, PhoenixActionType eventType) {
        if (response.getError() != null && !TextUtils.isEmpty(response.getError().getCode())) {
            try {
                messageBus.post(new PhoenixAnalyticsEvent.ErrorEvent(PhoenixAnalyticsEvent.Type.ERROR, Integer.parseInt(response.getError().getCode()), response.getError().getMessage()));
            } catch (NumberFormatException ex) {
                return;
            }
        }
    }

    private void sendAPIExceptionErrorEvent(ResponseElement response, PhoenixActionType eventType) {
        try {
            JSONObject apiException = new JSONObject(response.getResponse());
            if (apiException != null) {
                if (apiException.has("result")) {
                    JSONObject result = (JSONObject) apiException.get("result");
                    if (result != null && result.has("error")) {
                        JSONObject error = (JSONObject) result.get("error");
                        if (error != null) {
                            String errorCode = error.getString("code");
                            String errorMessage = error.getString("message");

                            if (TextUtils.equals(errorCode, CONCURRENCY_ERROR_COODE)) {
                                sendConcurrencyErrorEvent(errorMessage);
                            } else {
                                messageBus.post(new PhoenixAnalyticsEvent.BookmarkErrorEvent(Integer.parseInt(errorCode), errorMessage));
                            }
                        }
                    }
                }
            }
        } catch (JSONException | NumberFormatException ex ) {
            return;
        }
    }

    private void sendConcurrencyErrorEvent(String errorMessage) {
        messageBus.post(new OttEvent(OttEvent.OttEventType.Concurrency));
        messageBus.post(new PhoenixAnalyticsEvent.ConcurrencyErrorEvent(Integer.parseInt(CONCURRENCY_ERROR_COODE), errorMessage));
    }

    PKEvent.Listener getEventListener() {
        return mEventListener;
    }


    private static PhoenixAnalyticsConfig parseConfig(Object config) {
        if (config instanceof PhoenixAnalyticsConfig) {
            return ((PhoenixAnalyticsConfig) config);

        } else if (config instanceof JsonObject) {
            JsonObject params = (JsonObject) config;
            String baseUrl = params.get("baseUrl").getAsString();
            int partnerId = params.get("partnerId").getAsInt();
            int timerInterval = params.get("timerInterval").getAsInt();
            String ks = params.get("ks").getAsString();

            return new PhoenixAnalyticsConfig(partnerId, baseUrl, ks, timerInterval);
        }
        return null;
    }
}
