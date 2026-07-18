package com.example

import android.content.ComponentName
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.service.PlaybackService
import com.example.ui.RadioViewModel
import com.example.data.RadioRepository
import com.example.data.RadioDatabase
import com.google.common.util.concurrent.ListenableFuture
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class PlaybackTest {
    @Test
    fun testMediaController() {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val sessionToken = SessionToken(app, ComponentName(app, PlaybackService::class.java))
        val controllerFuture = MediaController.Builder(app, sessionToken).buildAsync()
        val controller = controllerFuture.get(10, TimeUnit.SECONDS)
        println("Controller connected: $controller")
    }
}
