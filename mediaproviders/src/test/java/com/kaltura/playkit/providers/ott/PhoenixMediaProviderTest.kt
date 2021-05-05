package com.kaltura.playkit.providers.ott

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kaltura.netkit.connect.response.PrimitiveResult
import com.kaltura.netkit.utils.OnCompletion
import com.kaltura.netkit.utils.SessionProvider
import com.kaltura.playkit.providers.api.phoenix.APIDefines
import com.kaltura.playkit.providers.base.OnMediaLoadCompletion
import junit.framework.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PhoenixMediaProviderTest {
    
    lateinit var sessionProvider: SessionProvider
    var phoenixBaseUrl = ""
    var ks = ""
    var partnerId = 0
    var assetId = ""
    var format = ""
    
    @Before
    fun setSessionProvider() {
        sessionProvider = object: SessionProvider {
            override fun baseUrl(): String {
                return phoenixBaseUrl
            }

            override fun getSessionToken(completion: OnCompletion<PrimitiveResult>?) {
                completion?.onComplete(PrimitiveResult(ks))
            }

            override fun partnerId(): Int {
                return partnerId
            }
        }
    }

    @Test
    fun doPhoenixSuccessfulCall() {
        val phoenixMediaProvider = PhoenixMediaProvider()
                .setSessionProvider(sessionProvider)
                .setAssetId(assetId)
                .setFormats(format)
                .setProtocol(PhoenixMediaProvider.HttpProtocol.Https)
                .setContextType(APIDefines.PlaybackContextType.Playback)
                .setAssetType(APIDefines.KalturaAssetType.Media)

        phoenixMediaProvider.load(OnMediaLoadCompletion { response ->
            assertTrue(response.isSuccess)
            println(response.response.id)
        })
    }
}
