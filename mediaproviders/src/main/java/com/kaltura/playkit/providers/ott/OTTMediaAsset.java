package com.kaltura.playkit.providers.ott;

import com.kaltura.playkit.providers.BaseMediaAsset;
import com.kaltura.playkit.providers.api.phoenix.APIDefines;

import java.util.List;

public class OTTMediaAsset extends BaseMediaAsset {

    String assetId;

    APIDefines.KalturaAssetType assetType;

    APIDefines.AssetReferenceType assetReferenceType;

    APIDefines.PlaybackContextType contextType;

    APIDefines.KalturaUrlType urlType;

    List<String> formats;

    List<String> mediaFileIds;

    String protocol;


    public OTTMediaAsset() {
    }

    public OTTMediaAsset setAssetId(String assetId) {
        this.assetId = assetId;
        return this;
    }

    public OTTMediaAsset setKs(String ks) {
        super.setKs(ks);
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


    public OTTMediaAsset setUrlType(APIDefines.KalturaUrlType urlType) {
        this.urlType = urlType;
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
        super.setReferrer(referrer);
        return this;
    }

    public String getAssetId() {
        return assetId;
    }

    public APIDefines.KalturaAssetType getAssetType() {
        return assetType;
    }

    public APIDefines.AssetReferenceType getAssetReferenceType() {
        return assetReferenceType;
    }

    public APIDefines.PlaybackContextType getContextType() {
        return contextType;
    }

    public APIDefines.KalturaUrlType getUrlType() {
        return urlType;
    }

    public List<String> getFormats() {
        return formats;
    }

    public List<String> getMediaFileIds() {
        return mediaFileIds;
    }

    public String getProtocol() {
        return protocol;
    }

    public boolean hasFormats() {
        return formats != null && formats.size() > 0;
    }

    public boolean hasFiles() {
        return mediaFileIds != null && mediaFileIds.size() > 0;
    }
}