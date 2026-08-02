package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDatabase
import com.example.ui.EditorViewModel
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DatabaseTest {

    @Test
    fun testDatabaseInitialization() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = AppDatabase.getInstance(context)
        assertNotNull(db)
        assertNotNull(db.settingsDao())
    }

    @Test
    fun testViewModelInitialization() {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val viewModel = EditorViewModel(app)
        assertNotNull(viewModel)
    }
}
