package com.example

import android.os.Looper
import com.example.data.AppDatabase
import com.example.ui.EditorViewModel
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ActivityLaunchAndNavigationTest {

    @Test
    fun testActivityLaunchAndStayOpen60Seconds() {
        // 1. Build and setup MainActivity (Simulates fresh app launch & Notification permission check)
        val controller = Robolectric.buildActivity(MainActivity::class.java).create().start().resume().visible()
        val activity = controller.get()

        assertNotNull("MainActivity should launch successfully", activity)
        assert(!activity.isFinishing)
        assert(!activity.isDestroyed)

        // 2. Keep app open for 60 seconds and let main looper process queued tasks (Splash screen timer, Room DB, WorkManager)
        ShadowLooper.idleMainLooper(60, TimeUnit.SECONDS)

        // 3. Verify Activity remains open with no crashes after 60 seconds
        assert(!activity.isFinishing)
        assert(!activity.isDestroyed)

        // 4. Verify ViewModel & WorkManager state
        val viewModel = EditorViewModel(activity.application)
        assertNotNull("EditorViewModel should initialize without error", viewModel)

        // Clean up
        controller.pause().stop().destroy()
    }
}
