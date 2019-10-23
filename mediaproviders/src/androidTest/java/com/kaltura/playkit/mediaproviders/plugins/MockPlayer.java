package com.kaltura.playkit.mediaproviders.plugins;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.kaltura.playkit.PKController;
import com.kaltura.playkit.PKEvent;
import com.kaltura.playkit.PKMediaConfig;
import com.kaltura.playkit.PKMediaFormat;
import com.kaltura.playkit.PKRequestParams;
import com.kaltura.playkit.PKTrackConfig;
import com.kaltura.playkit.Player;
import com.kaltura.playkit.player.ABRSettings;
import com.kaltura.playkit.player.LoadControlBuffers;
import com.kaltura.playkit.player.PKAspectRatioResizeMode;
import com.kaltura.playkit.player.PlayerView;
import com.kaltura.playkit.player.SubtitleStyleSettings;
import com.kaltura.playkit.player.VideoCodecSettings;
import com.kaltura.playkit.player.vr.VRSettings;
import com.kaltura.playkit.utils.Consts;


/**
 * Created by zivilan on 11/12/2016.
 */

public class MockPlayer implements Player {
    private boolean isPlaying = false;
    private int duration = 100;
    private long currentPosition = 0;

    @Override
    public Settings getSettings() {
        return new Settings() {

            @Override
            public Settings setContentRequestAdapter(PKRequestParams.Adapter contentRequestAdapter) {
                return this;
            }

            @Override
            public Settings setLicenseRequestAdapter(PKRequestParams.Adapter licenseRequestAdapter) {
                return this;
            }

            @Override
            public Settings setCea608CaptionsEnabled(boolean cea608CaptionsEnabled) {
                return this;
            }

            @Override
            public Settings setMpgaAudioFormatEnabled(boolean mpgaAudioFormatEnabled) {
                return this;
            }

            @Override
            public Settings useTextureView(boolean useTextureView) {
                return this;
            }

            @Override
            public Settings setAllowCrossProtocolRedirect(boolean crossProtocolRedirectEnabled) {
                return this;
            }

            @Override
            public Settings allowClearLead(boolean allowClearLead) {
                return this;
            }

            @Override
            public Settings enableDecoderFallback(boolean enableDecoderFallback) {
                return this;
            }

            @Override
            public Settings setSecureSurface(boolean isSurfaceSecured) {
                return this;
            }

            @Override
            public Settings setAdAutoPlayOnResume(boolean autoPlayOnResume) {
                return this;
            }

            @Override
            public Settings setPlayerBuffers(LoadControlBuffers loadControlBuffers) {
                return this;
            }

            @Override
            public Settings setVRPlayerEnabled(boolean vrPlayerEnabled) {
                return this;
            }

            @Override
            public Settings setPreferredAudioTrack(PKTrackConfig preferredAudioTrackConfig) {
                return this;
            }

            @Override
            public Settings setPreferredTextTrack(PKTrackConfig preferredTextTrackConfig) {
                return this;
            }

            @Override
            public Settings setPreferredMediaFormat(PKMediaFormat preferredMediaFormat) {
                return this;
            }

            @Override
            public Settings setSubtitleStyle(SubtitleStyleSettings subtitleStyleSettings) {
                return this;
            }

            @Override
            public Settings setABRSettings(ABRSettings abrSettings) {
                return this;
            }

            @Override
            public Settings setSurfaceAspectRatioResizeMode(PKAspectRatioResizeMode resizeMode) {
                return this;
            }

            @Override
            public Settings forceSinglePlayerEngine(boolean forceSinglePlayerEngine) {
                return this;
            }

            @Override
            public Settings setHideVideoViews(boolean hide) {
                return this;
            }

            @Override
            public Settings setVRSettings(VRSettings vrSettings) {
                return this;
            }

            @Override
            public Settings setPreferredVideoCodecSettings(VideoCodecSettings videoCodecSettings) {
                return null;
            }

            @Override
            public Settings setCustomLoadControlStrategy(Object o) {
                return null;
            }

            @Override
            public Settings setTunneledAudioPlayback(boolean b) {
                return null;
            }


        };
    }

    @Override
    public void prepare(@NonNull PKMediaConfig playerConfig) {

    }

    @Override
    public void updatePluginConfig(@NonNull String pluginName, @Nullable Object pluginConfig) {

    }

    @Override
    public void onApplicationPaused() {

    }

    @Override
    public void onApplicationResumed() {

    }

    @Override
    public void onOrientationChanged() {

    }

    @Override
    public void destroy() {

    }

    @Override
    public void stop() {
        // stop player
    }

    @Override
    public void play() {
        isPlaying = true;
    }

    @Override
    public void pause() {
        isPlaying = true;
    }

    @Override
    public void replay() {

    }

    @Override
    public PlayerView getView() {
        return null;
    }

    @Override
    public long getCurrentPosition() {
        return currentPosition;
    }

    @Override
    public long getPositionInWindowMs() {
        return 0;
    }

    @Override
    public long getCurrentProgramTime() {
        return 0;
    }

    @Override
    public long getDuration() {
        return duration;
    }

    @Override
    public long getBufferedPosition() {
        return 0;
    }

    @Override
    public void setVolume(float volume) {

    }

    @Override
    public boolean isPlaying() {
        return isPlaying;
    }

    @Override
    public PKEvent.Listener addEventListener(@NonNull PKEvent.Listener listener, Enum... events) {
        return null;
    }

    @Override
    public void removeEventListener(@NonNull PKEvent.Listener listener, Enum... events) {

    }

    @Override
    public PKEvent.Listener addStateChangeListener(@NonNull PKEvent.Listener listener) {
        return null;
    }

    @Override
    public void removeStateChangeListener(@NonNull PKEvent.Listener listener) {

    }

    @Override
    public void removeListener(@NonNull PKEvent.Listener listener) {

    }

    @Override
    public void changeTrack(String uniqueId) {

    }

    @Override
    public void seekTo(long position) {
        currentPosition = position;
    }

    @Override
    public String getSessionId() {
        return null;
    }

    @Override
    public boolean isLive() {
        return false;
    }

    @Override
    public PKMediaFormat getMediaFormat() {
        return null;
    }

    @Override
    public <T extends PKController> T getController(Class<T> type) {
        return null;
    }

    @Override
    public void updateSubtitleStyle(SubtitleStyleSettings subtitleStyleSettings) {

    }

    @Override
    public void updateSurfaceAspectRatioResizeMode(PKAspectRatioResizeMode resizeMode) {

    }

    @Override
    public <E extends PKEvent> void addListener(Object groupId, Class<E> type, PKEvent.Listener<E> listener) {

    }

    @Override
    public void addListener(Object groupId, Enum type, PKEvent.Listener listener) {

    }

    @Override
    public void removeListeners(@NonNull Object groupId) {

    }

    @Override
    public void setPlaybackRate(float rate) {

    }

    @Override
    public float getPlaybackRate() {
        return Consts.DEFAULT_PLAYBACK_RATE_SPEED;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }
}
