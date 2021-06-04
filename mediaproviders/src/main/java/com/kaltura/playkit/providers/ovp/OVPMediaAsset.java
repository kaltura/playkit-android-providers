package com.kaltura.playkit.providers.ovp;

import com.kaltura.playkit.providers.BaseMediaAsset;

public class OVPMediaAsset extends BaseMediaAsset {

    String entryId;
    String referenceId;
    Boolean redirectFromEntryId = true;

    public OVPMediaAsset() {
    }

    public OVPMediaAsset setEntryId(String entryId) {
        this.entryId = entryId;
        return this;
    }

    public OVPMediaAsset setReferenceId(String referenceId) {
        this.referenceId = referenceId;
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

    public OVPMediaAsset setRedirectFromEntryId(Boolean redirectFromEntryId) {
        if (redirectFromEntryId != null) {
            this.redirectFromEntryId = redirectFromEntryId;
        }
        return this;
    }

    public String getEntryId() {
        return entryId;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public Boolean getRedirectFromEntryId() {
        return redirectFromEntryId;
    }

    public String getUUID() {

        if (getGson() == null) {
            createGsonObject();
        }

        String mediaAssetJson = getGson().toJson(this);
        return toBase64(mediaAssetJson);
    }
}
