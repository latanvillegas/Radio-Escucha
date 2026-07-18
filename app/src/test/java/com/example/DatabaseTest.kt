package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.data.RadioDatabase
import com.example.data.RadioRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric

@RunWith(AndroidJUnit4::class)
class DatabaseTest {
    private lateinit var db: RadioDatabase
    private lateinit var repo: RadioRepository

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, RadioDatabase::class.java).allowMainThreadQueries().build()
        repo = RadioRepository(db.radioDao())
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun testPrepopulate() = runBlocking {
        repo.checkAndPrepopulate()
        val stations = db.radioDao().getAllStations().first()
        println("STATION COUNT: ${stations.size}")
        assertTrue(stations.isNotEmpty())
    }
}
