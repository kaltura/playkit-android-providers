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

package com.kaltura.playkit.providers.api.ovp.model;

public class KalturaPlaybackCaption {

    private String label;
    private String format;
    private String language;
    private String webVttUrl;
    private String url;
    private boolean isDefault;

    public String getLabel() {
        return label;
    }

    public String getFormat() {
        return format;
    }

    public String getLanguage() {
        return language;
    }

    public String getWebVttUrl() {
        return webVttUrl;
    }

    public String getUrl() {
        return url;
    }

    public boolean isDefault() {
        return isDefault;
    }
}

//"playbackCaptions": [{
//        "label": "English",
//        "format": "1",
//        "language": "English",
//        "webVttUrl": "https:\/\/cdnsecakmi.kaltura.com\/api_v3\/index.php\/service\/caption_captionasset\/action\/serveWebVTT\/captionAssetId\/theEntryId\/segmentIndex\/-1\/version\/2\/captions.vtt",
//        "url": "https:\/\/cdnsecakmi.kaltura.com\/api_v3\/index.php\/service\/caption_captionAsset\/action\/serve\/captionAssetId\/theEntryId\/v\/2",
//        "isDefault": false,
//        "objectType": "KalturaCaptionPlaybackPluginData"
//        }],