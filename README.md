[![CI Status](https://api.travis-ci.org/kaltura/playkit-android-providers.svg?branch=develop)](https://travis-ci.org/github/kaltura/playkit-android-providers)
[![Download](https://img.shields.io/maven-central/v/com.kaltura.playkit/playkitproviders?label=Download)](https://search.maven.org/artifact/com.kaltura.playkit/playkitproviders)
[![License](https://img.shields.io/badge/license-AGPLv3-black.svg)](https://github.com/kaltura/playkit-android-kava/blob/master/LICENSE)
![Android](https://img.shields.io/badge/platform-android-green.svg)


# playkit-android-providers

This plugin gives seemless access of your medias ingested in Kaltura backend.
By using this plugin, developers don't need to care about Network calls, Thread Management etc. Simply pass the media Id and respective KS (Kaltura Session Token) and you are good to go for the playback.

Providers are designed for Kaltura OVP or Kaltura OTT customers. One can have a question what is the difference between OVP and OTT !
 
OVP BE takes care of storage, transcoding, delivery, packaging and distribution. It is media preparation part.

OTT BE takes care of Auth, Subscription and other distribution related services.

For more info, please connect with Kaltura CSM.

### Setup

Add Provider plugin dependency to `build.gradle`. In android, we keep all plugins aligned with same version.

`implementation 'com.kaltura.playkit:playkitproviders:x.x.x'`

**You can find the latest version here:**

[Latest Release](https://github.com/kaltura/playkit-android-providers/releases)

Moving ahead about the plugin,

## OTT Provider

`PhoenixMediaProvider` class handles the API calls for the OTT. 

`PhoenixMediaProvider(final String baseUrl, final int partnerId, final String ks)`

`baseURL`: URL for the BE calls. You can get it from **OPC or TVM**.

`partnerID`: Unique partner id is generated after the account creation.

`ks`: Kaltura Session Token. It can be user level auth token.

#### APIs

##### `setAssetId(@NonNull String assetId)`

MANDATORY! the media asset id, to fetch the data for.

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


### How to create a OTT Provider (PhoenixMediaProvider) request and get the response for the media playback.

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

## OVP Provider

`KalturaOvpMediaProvider` class handles the API calls for the OTT. 

`KalturaOvpMediaProvider(final String baseUrl, final int partnerId, final String ks)`

`baseURL`: URL for the BE calls. You can get it from **KMC**.

`partnerID`: Unique partner id is generated after the account creation.

`ks`: Kaltura Session Token. It can be user level auth token.

#### APIs

`setSessionProvider(SessionProvider sessionProvider)`

MANDATORY! provides the baseUrl and the session token(ks) for the API calls.

Create object using `SimpleSessionProvider(String baseUrl, int partnerId, String ks)`
Build an OVP `SessionProvider` with the specified parameters. 

`baseUrl` Kaltura Server URL, such as "https://cdnapisec.kaltura.com".

`partnerId` Kaltura partner id

`ks` Kaltura Session token

##### `setEntryId(String entryId)`

MANDATORY! if `referenceId` not set -  the `entryId`, to fetch the data for. Basically this is the id for a playable media.

##### `setReferenceId(String referenceId)`

MANDATORY! if `entryId` not set - the `referenceId`, to fetch the data for.
Basically this is the id for a playable media.

##### `setReferrer(String referrer)`

NOT MANDATORY! The referrer url, to fetch the data for.

##### `setRedirectFromEntryId(boolean redirectFromEntryId)`

NOT MANDATORY! The redirectFromEntryId. Application filter by redirectFromEntryId of EntryId default is `true`.

### How to create a OTT Provider (PhoenixMediaProvider) request and get the response for the media playback.

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

### Error Code handling

For more info, please connect with our integration team via your CSM. 

### Samples

[OVP Sample](https://github.com/kaltura/playkit-android-samples/tree/develop/OVPStarter) 

Please take our [FullDemo Sample](https://github.com/kaltura/playkit-android-samples/tree/develop/FullDemo). It has module [playkitdemo](https://github.com/kaltura/playkit-android-samples/tree/develop/FullDemo/playkitdemo) which can be imported in Android Studio. It has examples for OVP and OTT media samples. It can be a quick starter code.

### BE API Tools,

[OVP API Tester](https://kaltura.github.io/playkit/tools/gpc)

[OTT API Tester](https://kaltura.github.io/playkit/tools/gpc-ott)
