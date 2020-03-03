package com.kaltura.playkit.providers;

public class BaseMediaAsset {

    private String ks;
    private String referrer;

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
}
