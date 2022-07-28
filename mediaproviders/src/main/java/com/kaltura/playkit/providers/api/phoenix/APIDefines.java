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

package com.kaltura.playkit.providers.api.phoenix;

import com.kaltura.playkit.PKMediaEntry;

/**
 * @hide
 */

public class APIDefines {

    public enum AssetReferenceType {
        Media("media"),
        InternalEpg("epg_internal"),
        ExternalEpg("epg_external"),
        Npvr("npvr");

        public String value;

        AssetReferenceType(String value){
            this.value = value;
        }
    }

    public enum LiveStreamType {
        Catchup("catchup"),
        StartOver("startOver"),
        TrickPlay("trickPlay");

        public String value;

        LiveStreamType(String value){
            this.value = value;
        }
    }


    public enum MediaType {
        Vod(KalturaAssetType.Media, PKMediaEntry.MediaEntryType.Vod),
        Channel(KalturaAssetType.Media, PKMediaEntry.MediaEntryType.Live),
        Recording(KalturaAssetType.Recording,PKMediaEntry.MediaEntryType.Vod),
        EPG(KalturaAssetType.Epg, PKMediaEntry.MediaEntryType.Live);

        private KalturaAssetType assetType;
        private PKMediaEntry.MediaEntryType mediaEntryType;

        MediaType(KalturaAssetType assetType, PKMediaEntry.MediaEntryType mediaEntryType){
            this.assetType = assetType;
            this.mediaEntryType = mediaEntryType;
        }

        public KalturaAssetType getAssetType() {
            return assetType;
        }

        public PKMediaEntry.MediaEntryType getMediaEntryType() {
            return mediaEntryType;
        }
    }


    public enum KalturaAssetType {
        Media("media"),
        Epg("epg"),
        Recording("recording");

        public String value;

        KalturaAssetType(String value){
            this.value = value;
        }
    }

    public enum PlaybackContextType {
        Download("DOWNLOAD"),
        Trailer("TRAILER"),
        Catchup("CATCHUP"),
        StartOver("START_OVER"),
        Playback("PLAYBACK");

        public String value;

        PlaybackContextType(String value){
            this.value = value;
        }
    }

    public enum KalturaUrlType {
        PlayManifest("PLAYMANIFEST"),
        Direct("DIRECT");

        public String value;

        KalturaUrlType(String value){
            this.value = value;
        }
    }

    public enum KalturaStreamerType {
        Applehttp("applehttp"),
        Mpegdash("mpegdash"),
        Url("url"),
        Smothstreaming("smothstreaming"),
        Multicast("multicast"),
        None("none");

        public String value;

        KalturaStreamerType(String value){
            this.value = value;
        }
    }
}

