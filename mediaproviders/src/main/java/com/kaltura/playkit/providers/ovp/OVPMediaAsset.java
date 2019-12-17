package com.kaltura.playkit.providers.ovp;

public class OVPMediaAsset {

    String entryId;
    String ks;

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
}
