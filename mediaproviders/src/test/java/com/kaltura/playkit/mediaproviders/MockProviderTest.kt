package com.kaltura.playkit.mediaproviders

import com.kaltura.netkit.connect.response.ResultElement
import com.kaltura.netkit.utils.ErrorElement
import com.kaltura.playkit.PKMediaEntry
import com.kaltura.playkit.providers.mock.MockMediaProvider
import junit.framework.Assert.assertFalse
import junit.framework.Assert.assertTrue
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MockProviderTest {
    val InputFile = "assets/mock/entries.playkit.json"

    @Test
    fun testMockProvider() {
        val mockMediaProvider = MockMediaProvider(InputFile, null, "0_rccv43zr")
        mockMediaProvider.load { response: ResultElement<PKMediaEntry> ->
            if (response.isSuccess) {
                val mediaEntry = response.response
                println("got some response. id = " + mediaEntry.id)
            } else {
                assertFalse(response.error == null)
                println("got error on json load: " + response.error.message)
            }
            mockMediaProvider.id("1_1h1vsv3z").load { response ->
                assertTrue(response.isSuccess)
                assertTrue(response.error == null)
                val mediaEntry = response.response
                assertTrue(mediaEntry.id == "1_1h1vsv3z")
                assertTrue(mediaEntry.sources[0].id == "1_ude4l5pb")
                mockMediaProvider.id("notexists").load { response ->
                    assertTrue(!response.isSuccess)
                    assertTrue(response.error != null)
                    assertTrue(response.error == ErrorElement.NotFound)
                }
            }
        }
    }
}