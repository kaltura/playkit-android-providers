[![CI Status](https://api.travis-ci.org/kaltura/playkit-android-providers.svg?branch=develop)](https://travis-ci.org/github/kaltura/playkit-android-providers)
[![Download](https://img.shields.io/maven-central/v/com.kaltura.playkit/playkitproviders?label=Download)](https://search.maven.org/artifact/com.kaltura.playkit/playkitproviders)
[![License](https://img.shields.io/badge/license-AGPLv3-black.svg)](https://github.com/kaltura/playkit-android-kava/blob/master/LICENSE)
![Android](https://img.shields.io/badge/platform-android-green.svg)


# playkit-android-providers

This plugin gives seemless access of your medias ingested in Kaltura backend with [Playkit](https://github.com/kaltura/playkit-android#kaltura-player-sdk) or [Kaltura-Player](https://github.com/kaltura/kaltura-player-android#kaltura-player-for-android).
By using this plugin, developers don't need to care about Network calls, Thread Management etc. Simply pass the media Id and respective KS (Kaltura Session Token) and you are good to go for the playback.

Providers are designed for Kaltura OVP or Kaltura OTT customers. One can have a question what is the difference between OVP and OTT !
 
OVP BE takes care of storage, transcoding, delivery, packaging and distribution. It is media preparation part.

OTT BE takes care of Auth, Subscription and other distribution related services.

Apart from this, plugin provides Concurrency measurement feature. Plugin uses `PhoenixAnalytics` for this.

For more info, please connect with Kaltura CSM.

### Setup

If You are a Kaltura-Player developer then no setup is required.
Kaltura-Player dependency is enough to use the Providers.

`implementation 'com.kaltura.player:tvplayer:x.x.x'`

> [kaltura-Player Latest Release](https://github.com/kaltura/kaltura-player-android/releases)

Add Provider plugin dependency to `build.gradle`. In android, we keep all plugins aligned with same version.

`implementation 'com.kaltura.playkit:playkitproviders:x.x.x'`

**You can find the latest version here:**

> [Providers Latest Release](https://github.com/kaltura/playkit-android-providers/releases)

Moving ahead about the plugin,

## Provider Setup for Kaltura-Player developers

### OTT Provider

Create `OTTMediaAsset` object. It can be created using `OTTMediaAsset()` and `OTTMediaAsset(@NonNull String assetId, List<String> formats, @NonNull String protocol)'.

After creating `OTTMediaAsset`, this object needs to be passed to `OTTMediaOptions`.

```kotlin
        var ottMediaAsset = OTTMediaAsset()
        ottMediaAsset.assetId = "Asset_Id"
        ottMediaAsset.assetType = "Asset_Type"
        ottMediaAsset.contextType = "Context_Type"
        ottMediaAsset.assetReferenceType = "Asset_Reference_Type"
        ottMediaAsset.protocol = "Protocol"
        ottMediaAsset.ks = "Kaltura_Session_Token"
        ottMediaAsset.urlType = "URL_Type"
        ottMediaAsset.streamerType = "Streamer_Type"
        ottMediaAsset.adapterData = "Adapter_Type"
        
        if (ottMedia.format != null) {
            ottMediaAsset.setFormats("List_of_formats")
        }

        if (ottMedia.fileId != null) {
            ottMediaAsset.setMediaFileIds("List_of_file_ids")
        }

        val ottMediaOptions = OTTMediaOptions(ottMediaAsset)
```

#### Phoenix Media Provider Playback Combinations:

##### Vod/Live:

```
ottMediaAsset.contextType = APIDefines.PlaybackContextType.Playback
ottMediaAsset.assetType = APIDefines.KalturaAssetType.Media
ottMediaAsset.assetReferenceType = APIDefines.AssetReferenceType.Media
```

##### Catchup:

```
ottMediaAsset.contextType = APIDefines.PlaybackContextType.Catchup
ottMediaAsset.assetType = APIDefines.KalturaAssetType.Epg
ottMediaAsset.assetReferenceType = APIDefines.AssetReferenceType.InternalEpg //APIDefines.AssetReferenceType.ExternalEpg 
```

##### Start Over:

```
ottMediaAsset.contextType = APIDefines.PlaybackContextType.StartOver
ottMediaAsset.assetType = APIDefines.KalturaAssetType.Epg
ottMediaAsset.assetReferenceType = APIDefines.AssetReferenceType.InternalEpg //APIDefines.AssetReferenceType.ExternalEpg 
```

##### Recording:

```
ottMediaAsset.contextType = APIDefines.PlaybackContextType.Playback
ottMediaAsset.assetType = APIDefines.KalturaAssetType.Recording
ottMediaAsset.assetReferenceType = APIDefines.AssetReferenceType.Npvr
```

##### Trailer:

```
ottMediaAsset.contextType = APIDefines.PlaybackContextType.Trailer
ottMediaAsset.assetType = APIDefines.KalturaAssetType.Media
ottMediaAsset.assetReferenceType = APIDefines.AssetReferenceType.Media
```

For more details about the parameters, please check the APIs in the later part of the document.

[MediaAsset Config Options] (https://github.com/kaltura/playkit-android-providers/blob/develop/mediaproviders/src/main/java/com/kaltura/playkit/providers/api/phoenix/APIDefines.java)


And finally `OTTMediaOptions` should be passed to the Player for the playback in `loadMedia(@NonNull final OTTMediaOptions mediaOptions, @NonNull final KalturaPlayer.OnEntryLoadListener onEntryLoadListener)` API.

```kotlin
player?.loadMedia(ottMediaOptions) { mediaOptions, entry, error ->
                    var entryId = ""
                    if (entry != null) {
                        entryId = entry.getId()
                    }
                    log.d("OTTMedia onEntryLoadComplete; $entryId ; $error")
                    handleOnEntryLoadComplete(error)
                }
```

### OVP Provider

Create `OVPMediaAsset` object. It can be created using `OVPMediaAsset()`.

After creating `OVPMediaAsset `, this object needs to be passed to `OVPMediaOptions`.

```kotlin
        var ovpMediaAsset = OVPMediaAsset()
        ovpMediaAsset.entryId = "Entry_Id"
        //ovpMediaAsset.referenceId = "Reference_Id"
        ovpMediaAsset.redirectFromEntryId = "RedirectFromEntryId"
        ovpMediaAsset.ks = "KS"
        
        val ovpMediaOptions = OVPMediaOptions(ovpMediaAsset)
```
For more details about the parameters, please check the APIs in the later part of the document.

And finally `OVPMediaOptions ` should be passed to the Player for the playback in `loadMedia(@NonNull final OVPMediaOptions mediaOptions, @NonNull final KalturaPlayer.OnEntryLoadListener onEntryLoadListener)` API.

```kotlin
player?.loadMedia(ovpMediaOptions) { mediaOptions, entry, error ->
                    var entryId = ""
                    if (entry != null) {
                        entryId = entry.getId()
                    }
                    log.d("OVPMedia onEntryLoadComplete; $entryId; $error")
                    handleOnEntryLoadComplete(error)
                }
```

## Provider Setup for Playkit developers

### OTT Provider

`PhoenixMediaProvider` class handles the API calls for the OTT. 

`PhoenixMediaProvider(final String baseUrl, final int partnerId, final String ks)`

`baseURL`: URL for the BE calls. You can get it from **OPC or TVM**.

`partnerID`: Unique partner id is generated after the account creation.

`ks`: Kaltura Session Token. It can be user level auth token.

#### SessionProvider

##### `setSessionProvider(@NonNull SessionProvider sessionProvider)`

MANDATORY! provides the baseUrl and the session token(ks) for the API calls.

`SessionProvider` can be create as below,

```java

SessionProvider ksSessionProvider = new SessionProvider() {
            @Override
            public String baseUrl() {
                String phoenixBaseUrl = "STRING_Base_URL";
                return phoenixBaseUrl;
            }

            @Override
            public void getSessionToken(OnCompletion<PrimitiveResult> completion) {
                String ks = "STRING_KS";
                if (completion != null) {
                    completion.onComplete(new PrimitiveResult(ks));
                }
            }

            @Override
            public int partnerId() {
                int OttPartnerId = INT_Partner_Id;
                return OttPartnerId;
            }
        };

```

#### How to create a OTT Provider (PhoenixMediaProvider) request and get the response for the media playback.

#### 1. Create a request

```java
SessionProvider ksSessionProvider = new SessionProvider() {
            @Override
            public String baseUrl() {
                String phoenixBaseUrl = "STRING_Base_URL";
                return phoenixBaseUrl;
            }

            @Override
            public void getSessionToken(OnCompletion<PrimitiveResult> completion) {
                String ks = "STRING_KS";
                if (completion != null) {
                    completion.onComplete(new PrimitiveResult(ks));
                }
            }

            @Override
            public int partnerId() {
                int OttPartnerId = INT_Partner_Id;
                return OttPartnerId;
            }
        };
        String mediaIdPhoenix = mediaId;

        MediaEntryProvider mediaProvider = new PhoenixMediaProvider()
                .setSessionProvider(ksSessionProvider)
                .setAssetId(mediaIdPhoenix)
                .setPKStreamerType(APIDefines.KalturaStreamerType.Mpegdash)
                .setProtocol(PhoenixMediaProvider.HttpProtocol.Https)
                .setContextType(APIDefines.PlaybackContextType.Playback)
                .setAssetType(APIDefines.KalturaAssetType.Media)
                .setPKUrlType(APIDefines.KalturaUrlType.Direct);
```

#### 2. Get a response

Create a `OnMediaLoadCompletion` object.

```java
OnMediaLoadCompletion playLoadedEntry = new OnMediaLoadCompletion() {
            @Override
            public void onComplete(ResultElement<PKMediaEntry> response) {
                context.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (response.isSuccess()) {
                            // Prepare player
                        } else {
                            // Diagnose the error
                        }
                    }
                });
            }
        };

```

And pass this object it to `PhoenixMediaProvider ` object created in Step-1.

`mediaProvider.load(playLoadedEntry);`

### OVP Provider

`KalturaOvpMediaProvider` class handles the API calls for the OTT. 

`KalturaOvpMediaProvider(final String baseUrl, final int partnerId, final String ks)`

`baseURL`: URL for the BE calls. You can get it from **KMC**.

`partnerID`: Unique partner id is generated after the account creation.

`ks`: Kaltura Session Token. It can be user level auth token.

#### SessionProvider

##### `setSessionProvider(SessionProvider sessionProvider)`

MANDATORY! provides the baseUrl and the session token(ks) for the API calls.

Create object using `SimpleSessionProvider(String baseUrl, int partnerId, String ks)`
Build an OVP `SessionProvider` with the specified parameters. 

`baseUrl` Kaltura Server URL, such as "https://cdnapisec.kaltura.com".

`partnerId` Kaltura partner id

`ks` Kaltura Session token

#### How to create a OVP Provider (KalturaOvpMediaProvider) request and get the response for the media playback.

#### 1. Create a request

```java
MediaEntryProvider mediaProvider = new KalturaOvpMediaProvider()
                .setSessionProvider(new SimpleSessionProvider("OVP_BASE_URL", "OVP_PARNTER_ID", "KS"))
                .setEntryId("ENTRYID_FOR_MEDIA");
```

#### 2. Get a response

Create a `OnMediaLoadCompletion` object.

```java
OnMediaLoadCompletion playLoadedEntry = new OnMediaLoadCompletion() {
            @Override
            public void onComplete(ResultElement<PKMediaEntry> response) {
                context.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (response.isSuccess()) {
                            // Prepare player
                        } else {
                            // Diagnose the error
                        }
                    }
                });
            }
        };

```

And pass this object it to `KalturaOvpMediaProvider` object created in Step-1.

`mediaProvider.load(playLoadedEntry);`

## APIs (Common for both Kaltura-Player and Playkit)

##### `setAssetId(@NonNull String assetId)`

MANDATORY! the media asset id, to fetch the data for.

##### `setAssetReferenceType(@NonNull APIDefines.AssetReferenceType assetReferenceType)`

ESSENTIAL in EPG!! defines the playing  AssetReferenceType especially in case of epg. Default is `APIDefines.KalturaAssetType.Media`. Other values could be `APIDefines.KalturaAssetType.Epg`, `APIDefines.KalturaAssetType.Recording`.

##### `setAssetType(@NonNull APIDefines.KalturaAssetType assetType)`

ESSENTIAL!! defines the playing asset group type. Default is `APIDefines.KalturaAssetType.Media`. Other values could be `APIDefines.KalturaAssetType.Epg`, `APIDefines.KalturaAssetType.Recording`.

##### `setContextType(@NonNull APIDefines.PlaybackContextType contextType)`

ESSENTIAL!! defines the playing context: Trailer, Catchup, Playback etc. Default is `APIDefines.PlaybackContextType.Playback`. Other calues could be `APIDefines.PlaybackContextType.Trailer`, `APIDefines.PlaybackContextType.Catchup`, `APIDefines.PlaybackContextType.StartOver`.

##### `setReferrer(String referrer)`

NOT MANDATORY! The referrer url, to fetch the data for.

##### `setPKUrlType(@NonNull APIDefines.KalturaUrlType urlType)`

OPTIONAL! Values could be `APIDefines.KalturaUrlType.PlayManifest` or `APIDefines.KalturaUrlType.Direct`.

##### `setPKStreamerType(@NonNull APIDefines.KalturaStreamerType streamerType)`

OPTIONAL! Value could be `APIDefines.KalturaStreamerType.Applehttp`, `APIDefines.KalturaStreamerType.Mpegdash`, `APIDefines.KalturaStreamerType.Url`, `APIDefines.KalturaStreamerType.Smothstreaming`, `APIDefines.KalturaStreamerType.Multicast`, `APIDefines.KalturaStreamerType.None`.

##### `setProtocol(@NonNull @HttpProtocol String protocol)`

OPTIONAL! The desired protocol (http/https) for the playback sources. The default is `null`, which makes the provider filter by server protocol.

##### `setFormats(@NonNull String... formats)`

OPTIONAL! Defines which of the sources to consider on `PKMediaEntry` creation.
1 or more content format definition. can be: Hd, Sd, Download, Trailer etc like priority.

##### `setFileIds(@NonNull String... mediaFileIds)`

OPTIONAL! If not available all sources will be fetched. Provide a list of media files ids. will be used in the `getPlaybackContext` API request.
Pass the list of MediaFile ids to narrow sources fetching from API to the specific files.

##### `setAdapterData(@NonNull Map<String,String> adapterData)`

OPTIONAL! Provide a `Map<String,String>` for providers adapter data .

> ###### OVP Specific APIs

##### `setEntryId(String entryId)`

MANDATORY! if `referenceId` not set -  the `entryId`, to fetch the data for. Basically this is the id for a playable media.

##### `setReferenceId(String referenceId)`

MANDATORY! if `entryId` not set - the `referenceId`, to fetch the data for.
Basically this is the id for a playable media.

##### `setReferrer(String referrer)`

NOT MANDATORY! The referrer url, to fetch the data for.

##### `setRedirectFromEntryId(boolean redirectFromEntryId)`

NOT MANDATORY! The redirectFromEntryId. Application filter by redirectFromEntryId of EntryId default is `true`.

## Phoenix Analytics

Let's suppose you want to track how many concurrent users are logged-in or playing the media. Means Stream level concurrency, here you can take benefit of this feature. 

For this first you need to register the plugin,

`PlayKitManager.registerPlugins(this, PhoenixAnalyticsPlugin.factory);`

After this, simply setup the Phoenix Analytics,

```java
String ks = "Kaltura_Session_Token";
PhoenixAnalyticsConfig phoenixAnalyticsConfig = new PhoenixAnalyticsConfig(INT_PARTNER_ID, "BASE_URL", ks, INT_timerInterval);
config.setPluginConfig(PhoenixAnalyticsPlugin.factory.getName(), phoenixAnalyticsConfig);
``` 

Here `timerInterval` field is the frequency of "Hit" or "Ping" going to the server for tracking. Value should be passed in **'Seconds'**.

> ##### Listen to the PhoenixAnalyticsEvents

After using the analytics, you can listen to the events which will tell when the concurrency is exceeded, if there is a bookmmark event error. 

```
player?.addListener(this, PhoenixAnalyticsEvent.concurrencyError) { event ->
	// Event payload has errorCode and errorMessage
}
player?.addListener(this, PhoenixAnalyticsEvent.bookmarkError) { event ->
	// Event payload has errorCode and errorMessage
}
player?.addListener(this, PhoenixAnalyticsEvent.error) { event -> 
	// Event payload has errorCode and errorMessage
}
player?.addListener(this, PhoenixAnalyticsEvent.reportSent) { event -> 
	// Event payload has the reported event name
}

```

### Error Code handling

For more info, please connect with your CSM. 

### Samples

> Kaltura Player Samples

[OTT Samples](https://github.com/kaltura/kaltura-player-android-samples/tree/develop/OTTSamples)

[OVP Samples](https://github.com/kaltura/kaltura-player-android-samples/tree/develop/OVPSamples)

> Playkit Samples

Please take our [FullDemo Sample](https://github.com/kaltura/playkit-android-samples/tree/develop/FullDemo). It has module [playkitdemo](https://github.com/kaltura/playkit-android-samples/tree/develop/FullDemo/playkitdemo) which can be imported in Android Studio. It has examples for OVP and OTT media samples. It can be a quick starter code.

[OVP Sample](https://github.com/kaltura/playkit-android-samples/tree/develop/OVPStarter) 

### BE API Tools

[OVP API Tester](https://kaltura.github.io/playkit/tools/gpc)

[OTT API Tester](https://kaltura.github.io/playkit/tools/gpc-ott)
