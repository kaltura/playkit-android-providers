package com.kaltura.playkit.providers.ott;

import androidx.annotation.NonNull;

import com.kaltura.playkit.providers.BaseMediaAsset;
import com.kaltura.playkit.providers.api.phoenix.APIDefines;
import com.kaltura.playkit.providers.api.phoenix.APIDefines.AssetReferenceType;
import com.kaltura.playkit.providers.api.phoenix.APIDefines.KalturaAssetType;
import com.kaltura.playkit.providers.api.phoenix.APIDefines.KalturaUrlType;
import com.kaltura.playkit.providers.api.phoenix.APIDefines.KalturaStreamerType;
import com.kaltura.playkit.providers.api.phoenix.APIDefines.PlaybackContextType;

import java.util.List;
import java.util.Map;

import static com.kaltura.playkit.providers.ott.PhoenixMediaProvider.*;

public class OTTMediaAsset extends BaseMediaAsset {

    String assetId;

    KalturaAssetType assetType;

    AssetReferenceType assetReferenceType;

    PlaybackContextType contextType;

    KalturaUrlType urlType;

    KalturaStreamerType streamerType;

    List<String> formats;

    List<String> mediaFileIds;

    @HttpProtocol String protocol;

    Map<String, String> adapterData;

    public OTTMediaAsset() {
    }

    /**
     *  This constructor will create MediaAsset that will work fine for VOD content using PhoenixMediaProvider
     *  in case it is required to play live/catchup/recording or any other different stream configuration
     *  please use the builder methods below for updating the required parameters
     *
     *
     *  @param assetId  - ott mediaId
     *  @param formats  -list of one or more required media formats
     *  @param protocol - HttpProtocol.Https, HttpProtocol.Http, HttpProtocol.All
     *
     */
    public OTTMediaAsset(@NonNull String assetId, List<String> formats, @NonNull @HttpProtocol String protocol) {
        this.assetId = assetId;
        this.formats = formats;

        this.protocol = HttpProtocol.Https;
        if (HttpProtocol.Http.equals(protocol)) {
            this.protocol = HttpProtocol.Http;
        } else if (HttpProtocol.All.equals(protocol)) {
            this.protocol = HttpProtocol.All;
        }
    }

    public OTTMediaAsset setAssetId(String assetId) {
        this.assetId = assetId;
        return this;
    }

    public OTTMediaAsset setKs(String ks) {
        super.setKs(ks);
        return this;
    }

    public OTTMediaAsset setAssetType(KalturaAssetType assetType) {
        this.assetType = assetType;
        return this;
    }

    public OTTMediaAsset setAssetReferenceType(AssetReferenceType assetReferenceType) {
        this.assetReferenceType = assetReferenceType;
        return this;
    }

    public OTTMediaAsset setContextType(PlaybackContextType contextType) {
        this.contextType = contextType;
        return this;
    }


    public OTTMediaAsset setUrlType(KalturaUrlType urlType) {
        this.urlType = urlType;
        return this;
    }

    public OTTMediaAsset setStreamerType(KalturaStreamerType streamerType) {
        this.streamerType = streamerType;
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

    public OTTMediaAsset setAdapterData(Map<String, String> adapterData) {
        this.adapterData = adapterData;
        return this;
    }

    public String getAssetId() {
        return assetId;
    }

    public KalturaAssetType getAssetType() {
        return assetType;
    }

    public AssetReferenceType getAssetReferenceType() {
        return assetReferenceType;
    }

    public PlaybackContextType getContextType() {
        return contextType;
    }

    public KalturaUrlType getUrlType() {
        return urlType;
    }

    public KalturaStreamerType getStreamerType() {
        return streamerType;
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

    public Map<String, String> getAdapterData() {
        return adapterData;
    }

    public boolean hasFormats() {
        return formats != null && formats.size() > 0;
    }

    public boolean hasFileIds() {
        return mediaFileIds != null && mediaFileIds.size() > 0;
    }

    public boolean hasAdapterData() {
        return adapterData != null && !adapterData.isEmpty();
    }

    public String getUUID() {
        if (protocol == null) {
            protocol = HttpProtocol.Https;
        }

        if (contextType == null) {
            contextType = PlaybackContextType.Playback;
        }

        if (urlType == null) {
            urlType = urlType.PlayManifest;
        }

        if (assetType == null) {
            switch (contextType) {
                case Playback:
                case Trailer:
                    assetType = APIDefines.KalturaAssetType.Media;
                    break;

                case StartOver:
                case Catchup:
                    assetType = APIDefines.KalturaAssetType.Epg;
                    break;
            }
        }

        if (assetReferenceType == null) {
            assetReferenceType = AssetReferenceType.Media;
            if (assetType == KalturaAssetType.Epg) {
                assetReferenceType = AssetReferenceType.InternalEpg;
            }
        }

        String mediaAssetJson = getGson().toJson(this);
        return toBase64(mediaAssetJson);
    }
}
