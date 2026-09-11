package com.example

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.service.PlaybackService
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric

@RunWith(AndroidJUnit4::class)
class PlaybackTest {
    @Test
    fun testPlaybackServiceLifecycle() {
        val serviceController = Robolectric.buildService(PlaybackService::class.java)
        val service = serviceController.create().get()
        assertNotNull(service)
        serviceController.destroy()
    }
}
