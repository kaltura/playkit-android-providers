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
    private String languageCode;

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

    public String getLanguageCode() {
        return languageCode;
    }
}
