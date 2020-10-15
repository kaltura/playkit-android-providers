package com.kaltura.playkit.providers.ovp;

import com.kaltura.playkit.PKMediaEntry;
import com.kaltura.playkit.providers.BaseMediaAsset;

public class OVPMediaAsset extends BaseMediaAsset {

    String entryId;

    public OVPMediaAsset() {
    }

    public OVPMediaAsset setEntryId(String entryId) {
        this.entryId = entryId;
        return this;
    }

    public OVPMediaAsset setKs(String ks) {
        super.setKs(ks);
        return this;
    }

    public OVPMediaAsset setReferrer(String referrer) {
        super.setReferrer(referrer);
        return this;
    }

    public OVPMediaAsset setMediaEntryType(PKMediaEntry.MediaEntryType mediaEntryType) {
        super.setMediaEntryType(mediaEntryType);
        return this;
    }

    public String getEntryId() {
        return entryId;
    }
}
