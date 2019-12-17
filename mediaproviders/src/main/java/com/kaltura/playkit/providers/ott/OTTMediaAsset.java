package com.kaltura.playkit.providers.ott;

import com.kaltura.playkit.providers.api.phoenix.APIDefines;

import java.util.List;

public class OTTMediaAsset {

    String assetId;

    APIDefines.KalturaAssetType assetType;

    APIDefines.AssetReferenceType assetReferenceType;

    APIDefines.PlaybackContextType contextType;

    List<String> formats;

    List<String> mediaFileIds;

    String protocol;

    String referrer;

    public OTTMediaAsset() {
    }

    public OTTMediaAsset setAssetId(String assetId) {
        this.assetId = assetId;
        return this;
    }

    public OTTMediaAsset setAssetType(APIDefines.KalturaAssetType assetType) {
        this.assetType = assetType;
        return this;
    }

    public OTTMediaAsset setAssetReferenceType(APIDefines.AssetReferenceType assetReferenceType) {
        this.assetReferenceType = assetReferenceType;
        return this;
    }

    public OTTMediaAsset setContextType(APIDefines.PlaybackContextType contextType) {
        this.contextType = contextType;
        return this;
    }

    public OTTMediaAsset setFormats(List<String> formats) {
        this.formats = formats;
        return this;
    }

    public OTTMediaAsset setMediaFileIds(List<String> mediaFileIds) {
        this.mediaFileIds = mediaFileIds;
        return this;
    }

    public OTTMediaAsset setProtocol(String protocol) {
        this.protocol = protocol;
        return this;
    }

    public OTTMediaAsset setReferrer(String referrer) {
        this.referrer = referrer;
        return this;
    }

    public boolean hasFormats() {
        return formats != null && formats.size() > 0;
    }

    public boolean hasFiles() {
        return mediaFileIds != null && mediaFileIds.size() > 0;
    }
}