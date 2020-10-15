package com.kaltura.playkit.providers;

import com.kaltura.playkit.PKMediaEntry;

public class BaseMediaAsset {

    String ks;
    String referrer;
    PKMediaEntry.MediaEntryType mediaEntryType;

    public String getKs() {
        return ks;
    }

    public BaseMediaAsset setKs(String ks) {
        this.ks = ks;
        return this;
    }

    public String getReferrer() {
        return referrer;
    }

    public BaseMediaAsset setReferrer(String referrer) {
        this.referrer = referrer;
        return  this;
    }

    public PKMediaEntry.MediaEntryType getMediaEntryType() {
        return  mediaEntryType;
    }

    public BaseMediaAsset setMediaEntryType(PKMediaEntry.MediaEntryType mediaEntryType) {
        this.mediaEntryType = mediaEntryType;
        return  this;
    }
}
