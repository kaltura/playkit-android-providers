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

package com.kaltura.playkit.plugins.ovp;

import android.content.Context;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.kaltura.netkit.connect.executor.APIOkRequestsExecutor;
import com.kaltura.netkit.connect.executor.RequestQueue;
import com.kaltura.netkit.connect.request.RequestBuilder;
import com.kaltura.netkit.connect.response.ResponseElement;
import com.kaltura.netkit.utils.OnRequestCompletion;
import com.kaltura.playkit.BuildConfig;
import com.kaltura.playkit.MessageBus;
import com.kaltura.playkit.PKError;
import com.kaltura.playkit.PKEvent;
import com.kaltura.playkit.PKLog;
import com.kaltura.playkit.PKMediaConfig;
import com.kaltura.playkit.PKPlugin;
import com.kaltura.playkit.PlayKitManager;
import com.kaltura.playkit.Player;
import com.kaltura.playkit.PlayerEvent;
import com.kaltura.playkit.plugins.ads.AdEvent;
import com.kaltura.playkit.plugins.ads.AdInfo;
import com.kaltura.playkit.plugins.ads.AdPositionType;
import com.kaltura.playkit.providers.api.ovp.services.StatsService;
import com.kaltura.playkit.utils.Consts;

/**
 * Created by zivilan on 02/11/2016.
 */

public class KalturaStatsPlugin extends PKPlugin {
    private static final PKLog log = PKLog.get("KalturaStatsPlugin");

    private Context context;
    private AdInfo adInfo;
    private Player player;
    private PKMediaConfig mediaConfig;
    private KalturaStatsConfig pluginConfig;
    private MessageBus messageBus;
    private RequestQueue requestsExecutor;
    private int timerInterval;

    private float seekPercent = 0;
    float progress;
    private boolean playReached25 = false;
    private boolean playReached50 = false;
    private boolean playReached75 = false;
    private boolean playReached100 = false;
    private boolean isBuffering = false;
    private boolean intervalOn = false;
    private boolean hasSeeked = false;
    private boolean isWidgetLoaded = false;
    private boolean isMediaLoaded = false;
    private boolean isFirstPlay = true;
    private boolean durationValid = false;

    public enum KStatsEvent {
        WIDGET_LOADED(1),
        MEDIA_LOADED(2),
        PLAY(3),
        PLAY_REACHED_25(4),
        PLAY_REACHED_50(5),
        PLAY_REACHED_75(6),
        PLAY_REACHED_100(7),
        BUFFER_START(12),
        BUFFER_END(13),
        REPLAY(16),
        SEEK(17),
        PRE_BUMPER_PLAYED(21),
        POST_BUMPER_PLAYED(22),
        PREROLL_STARTED(24),
        MIDROLL_STARTED(25),
        POSTROLL_STARTED(26),
        PREROLL_CLICKED(28),
        MIDROLL_CLICKED(29),
        POSTROLL_CLICKED(30),
        PREROLL_25(32),
        PREROLL_50(33),
        PREROLL_75(34),
        MIDROLL_25(35),
        MIDROLL_50(36),
        MIDROLL_75(37),
        POSTROLL_25(38),
        POSTROLL_50(39),
        POSTROLL_75(40),
        ERROR(99);

        private final int value;

        KStatsEvent(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }

    public static final Factory factory = new Factory() {
        @Override
        public String getName() {
            return "KalturaStats";
        }

        @Override
        public PKPlugin newInstance() {
            return new KalturaStatsPlugin();
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
        this.pluginConfig = parseConfig(config);
        setTimerInterval();
        this.messageBus = messageBus;
        addListeners();
        this.context = context;
        log.d("onLoad finished");
    }

    private void addListeners() {
        messageBus.addListener(this, PlayerEvent.metadataAvailable, event -> {
            printReceivedEvent(event);
            if (playerDurationUnset()) {
                return;
            }
            sendMediaLoaded();
        });

        messageBus.addListener(this, PlayerEvent.stateChanged, event -> {
            printReceivedEvent(event);
            onStateChangedEvent(event);
        });

        messageBus.addListener(this, PlayerEvent.error, event -> {
            printReceivedEvent(event);
            PKError error = event.error;
            if (error != null && !error.isFatal()) {
                log.v("Error eventType = " + error.errorType + " severity = " + error.severity + " errorMessage = " + error.message);
                return;
            }
            sendAnalyticsEvent(KStatsEvent.ERROR);
        });

        messageBus.addListener(this, PlayerEvent.play, event -> {
            printReceivedEvent(event);
            if (playerDurationUnset()) {
                return;
            }
        });

        messageBus.addListener(this, PlayerEvent.seeked, event -> {
            printReceivedEvent(event);
            if (playerDurationUnset()) {
                return;
            }
            hasSeeked = true;
            seekPercent = (float) player.getCurrentPosition() / player.getDuration();
            sendAnalyticsEvent(KStatsEvent.SEEK);
        });

        messageBus.addListener(this, PlayerEvent.canPlay, event -> {
            printReceivedEvent(event);
            if (playerDurationUnset()) {
                return;
            }
        });

        messageBus.addListener(this, PlayerEvent.playing, event -> {
            printReceivedEvent(event);
            if (playerDurationUnset()) {
                return;
            }
            if (isFirstPlay) {
                sendWidgetLoaded();
                sendMediaLoaded();
                log.d("FIRST PLAYBACK sending KStatsEvent.PLAY");
                sendAnalyticsEvent(KStatsEvent.PLAY);
                isFirstPlay = false;
            }
        });

        messageBus.addListener(this, PlayerEvent.replay, event -> {
            printReceivedEvent(event);
            sendAnalyticsEvent(KStatsEvent.REPLAY);
        });

        messageBus.addListener(this, PlayerEvent.playheadUpdated, event -> {
            if (playerDurationUnset()) {
                return;
            }
            PlayerEvent.PlayheadUpdated playheadUpdated = (PlayerEvent.PlayheadUpdated) event;
            if (playheadUpdated.position >= 0 && playheadUpdated.duration > 0) {
                progress = (float) playheadUpdated.position / playheadUpdated.duration;
            } else {
                progress = 0f;
            }
            maybeSentPlayerReachedEvent();
        });

        messageBus.addListener(this, PlayerEvent.durationChanged, event -> {
            printReceivedEvent(event);
            if (playerDurationUnset()) {
                return;
            }
            long currDuration = event.duration;
            if (currDuration >= 0) {
                durationValid = true;
            }
        });

        messageBus.addListener(this, PlayerEvent.ended, event -> {
            printReceivedEvent(event);
            if (playerDurationUnset()) {
                return;
            }
            sendPlayReached25();
            sendPlayReached50();
            sendPlayReached75();
            sendPlayReached100();
        });

        messageBus.addListener(this, AdEvent.started, event -> {
            printReceivedAdEvent(event);
            adInfo = ((AdEvent.AdStartedEvent) event).adInfo;
            if (adInfo != null) {
                if (AdPositionType.PRE_ROLL.equals(adInfo.getAdPositionType())) {
                    sendAnalyticsEvent(KStatsEvent.PREROLL_STARTED);
                } else if (AdPositionType.MID_ROLL.equals(adInfo.getAdPositionType())) {
                    sendAnalyticsEvent(KStatsEvent.MIDROLL_STARTED);
                } else if (AdPositionType.POST_ROLL.equals(adInfo.getAdPositionType())) {
                    sendAnalyticsEvent(KStatsEvent.POSTROLL_STARTED);
                }
            }
        });

        messageBus.addListener(this, AdEvent.firstQuartile, event -> {
            printReceivedAdEvent(event);
            if (adInfo != null) {
                if (AdPositionType.PRE_ROLL.equals(adInfo.getAdPositionType())) {
                    sendAnalyticsEvent(KStatsEvent.PREROLL_25);
                } else if (AdPositionType.MID_ROLL.equals(adInfo.getAdPositionType())) {
                    sendAnalyticsEvent(KStatsEvent.MIDROLL_25);
                } else if (AdPositionType.POST_ROLL.equals(adInfo.getAdPositionType())) {
                    sendAnalyticsEvent(KStatsEvent.POSTROLL_25);
                }
            }
        });

        messageBus.addListener(this, AdEvent.midpoint, event -> {
            printReceivedAdEvent(event);
            if (adInfo != null) {
                if (AdPositionType.PRE_ROLL.equals(adInfo.getAdPositionType())) {
                    sendAnalyticsEvent(KStatsEvent.PREROLL_50);
                } else if (AdPositionType.MID_ROLL.equals(adInfo.getAdPositionType())) {
                    sendAnalyticsEvent(KStatsEvent.MIDROLL_50);
                } else if (AdPositionType.POST_ROLL.equals(adInfo.getAdPositionType())) {
                    sendAnalyticsEvent(KStatsEvent.POSTROLL_50);
                }
            }
        });

        messageBus.addListener(this, AdEvent.thirdQuartile, event -> {
            printReceivedAdEvent(event);
            if (adInfo != null) {
                if (AdPositionType.PRE_ROLL.equals(adInfo.getAdPositionType())) {
                    sendAnalyticsEvent(KStatsEvent.PREROLL_75);
                } else if (AdPositionType.MID_ROLL.equals(adInfo.getAdPositionType())) {
                    sendAnalyticsEvent(KStatsEvent.MIDROLL_75);
                } else if (AdPositionType.POST_ROLL.equals(adInfo.getAdPositionType())) {
                    sendAnalyticsEvent(KStatsEvent.POSTROLL_75);
                }
            }
        });

        messageBus.addListener(this, AdEvent.adClickedEvent, event -> {
            printReceivedAdEvent(event);
            if (adInfo != null) {
                if (AdPositionType.PRE_ROLL.equals(adInfo.getAdPositionType())) {
                    sendAnalyticsEvent(KStatsEvent.PREROLL_CLICKED);
                } else if (AdPositionType.MID_ROLL.equals(adInfo.getAdPositionType())) {
                    sendAnalyticsEvent(KStatsEvent.MIDROLL_CLICKED);
                } else if (AdPositionType.POST_ROLL.equals(adInfo.getAdPositionType())) {
                    sendAnalyticsEvent(KStatsEvent.POSTROLL_CLICKED);
                }
            }
        });

        messageBus.addListener(this, AdEvent.error, event -> {
            printReceivedAdEvent(event);
            sendAnalyticsEvent(KStatsEvent.ERROR);
        });
    }

    private boolean playerDurationUnset() {
        if (player == null ||  player.getDuration() < 0) {
            return true;
        }
        return false;
    }

    private void setTimerInterval() {
        timerInterval = pluginConfig.getTimerInterval() * (int) Consts.MILLISECONDS_MULTIPLIER;
        if (timerInterval <= 0) {
            timerInterval = Consts.DEFAULT_ANALYTICS_TIMER_INTERVAL_LOW;
        }
    }

    @Override
    public void onDestroy() {
        log.d("onDestroy");
        if (messageBus != null) {
            messageBus.removeListeners(this);
        }
        intervalOn = false;
    }

    @Override
    protected void onUpdateMedia(PKMediaConfig mediaConfig) {
        this.mediaConfig = mediaConfig;
        resetPlayerFlags();
    }

    @Override
    protected void onUpdateConfig(Object config) {
        this.pluginConfig = parseConfig(config);
        setTimerInterval();
    }

    private static KalturaStatsConfig parseConfig(Object config) {
        if (config instanceof KalturaStatsConfig) {
            return ((KalturaStatsConfig) config);

        } else if (config instanceof JsonObject) {
            return new Gson().fromJson(((JsonObject) config), KalturaStatsConfig.class);
        }
        return null;
    }

    @Override
    protected void onApplicationPaused() {
    }

    @Override
    protected void onApplicationResumed() {
    }

    private void onStateChangedEvent(PlayerEvent.StateChanged event) {
        log.d("New PlayerState = " + event.newState.name());
        switch (event.newState) {
            case IDLE:
                break;
            case LOADING:
                if (isBuffering) {
                    isBuffering = false;
                    sendAnalyticsEvent(KStatsEvent.BUFFER_END);
                }
                break;
            case READY:
                if (isBuffering) {
                    isBuffering = false;
                    sendAnalyticsEvent(KStatsEvent.BUFFER_END);
                }
                if (!intervalOn) {
                    intervalOn = true;
                }
                sendWidgetLoaded();
                break;
            case BUFFERING:
                isBuffering = true;
                sendAnalyticsEvent(KStatsEvent.BUFFER_START);
                break;
        }
    }

    private void printReceivedEvent(PKEvent event) {
        log.d("Player Event = " + event.eventType().name());
    }

    private void printReceivedAdEvent(PKEvent event) {
        log.d("Ad Event = " + event.eventType().name());
    }

    private void sendPlayReached100() {
        if (!playReached100) {
            log.d("PLAY_REACHED_100");
            sendAnalyticsEvent(KStatsEvent.PLAY_REACHED_100);
            playReached100 = true;
        }
    }

    private void sendPlayReached75() {
        if (!playReached75) {
            log.d("PLAY_REACHED_75");
            sendAnalyticsEvent(KStatsEvent.PLAY_REACHED_75);
            playReached75 = true;
        }
    }

    private void sendPlayReached50() {
        if (!playReached50) {
            log.d("PLAY_REACHED_50");
            sendAnalyticsEvent(KStatsEvent.PLAY_REACHED_50);
            playReached50 = true;
        }
    }

    private void sendPlayReached25() {
        if (!playReached25) {
            log.d("PLAY_REACHED_25");
            sendAnalyticsEvent(KStatsEvent.PLAY_REACHED_25);
            playReached25 = true;
        }
    }

    private void sendWidgetLoaded() {
        if (!isWidgetLoaded && durationValid) {
            isWidgetLoaded = true;
            sendAnalyticsEvent(KStatsEvent.WIDGET_LOADED);
        }
    }

    private void sendMediaLoaded() {
        if (!isMediaLoaded && durationValid) {
            isMediaLoaded = true;
            sendAnalyticsEvent(KStatsEvent.MEDIA_LOADED);
        }
    }

    /**
     * Reset the flags in case of media change or media ended
     */
    private void resetPlayerFlags() {
        seekPercent = 0;
        playReached25 = false;
        playReached50 = false;
        playReached75 = false;
        playReached100 = false;
        hasSeeked = false;
        isWidgetLoaded = false;
        isMediaLoaded = false;
        isFirstPlay = true;
    }

    public void maybeSentPlayerReachedEvent() {
        //log.d("progress = " + progress + " seekPercent = " + seekPercent);
        if (!playReached25 && progress >= 0.25 && seekPercent < 0.5) {
            sendPlayReached25();
        } else if (!playReached50 && progress >= 0.5 && seekPercent < 0.75) {
            sendPlayReached25();
            sendPlayReached50();
        } else if (!playReached75 && progress >= 0.75 && seekPercent < 1) {
            sendPlayReached25();
            sendPlayReached50();
            sendPlayReached75();
        }
    }
    /**
     * Send stats event to Kaltura stats DB
     *
     * @param eventType - Enum stating Kaltura state events
     */
    private void sendAnalyticsEvent(final KStatsEvent eventType) {
        if (pluginConfig == null || pluginConfig.getEntryId() == null) {
            log.w("Can not send analytics event. Mandatory field entryId is missing");
            return;
        }
        String sessionId = (player.getSessionId() != null) ? player.getSessionId() : "";

        long duration = player.getDuration() == Consts.TIME_UNSET ? -1 : player.getDuration() / Consts.MILLISECONDS_MULTIPLIER;
        long currentPosition = player.getCurrentPosition() == Consts.POSITION_UNSET ? -1 : player.getCurrentPosition() / Consts.MILLISECONDS_MULTIPLIER;

        final RequestBuilder requestBuilder = StatsService.sendStatsEvent(pluginConfig.getBaseUrl(), pluginConfig.getPartnerId(), eventType.getValue(), PlayKitManager.CLIENT_TAG, duration,
                sessionId, currentPosition, pluginConfig.getUiconfId(), pluginConfig.getEntryId(), "_" + pluginConfig.getPartnerId(), hasSeeked,
                pluginConfig.getContextId(), context.getPackageName(), pluginConfig.getUserId(), pluginConfig.getHasKanalony());

        requestBuilder.completion(new OnRequestCompletion() {
            @Override
            public void onComplete(ResponseElement response) {
                log.d("onComplete send event: " + eventType.toString());
                messageBus.post(new KalturaStatsEvent.KalturaStatsReport(eventType.toString()));
                if (hasSeeked) {
                    hasSeeked = false;
                }
            }
        });
        requestsExecutor.queue(requestBuilder.build());
    }
}
