package com.kaltura.playkit.mediaproviders

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.kaltura.netkit.utils.ErrorElement
import com.kaltura.playkit.providers.mock.MockMediaProvider
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MockProviderTest {
    val InputFile = "mock/entries.playkit.json"

    @Test
    fun testMockProvider() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val mockMediaProvider = MockMediaProvider(InputFile, context, "harold")
        mockMediaProvider.load { response ->

            if (response.isSuccess) {
                val mediaEntry = response.response
                println("MockProvider response success entryId = " + mediaEntry.id)
            } else {
                assertFalse(response.error == null)
                println("MockProvider response error message " + response.error.message)
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