package com.kaltura.playkit.providers.ovp;

public class OVPMediaAsset {

    String entryId;
    String ks;
    String referrer;

    public OVPMediaAsset() {
    }

    public OVPMediaAsset setEntryId(String entryId) {
        this.entryId = entryId;
        return this;
    }

    public OVPMediaAsset setKs(String ks) {
        this.ks = ks;
        return this;
    }

    public OVPMediaAsset setReferrer(String referrer) {
        this.referrer = referrer;
        return this;
    }
}
