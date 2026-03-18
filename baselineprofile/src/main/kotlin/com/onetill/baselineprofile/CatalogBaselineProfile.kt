package com.onetill.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Generates a baseline profile for the catalog scrolling path.
 *
 * Run on a connected device or emulator:
 *   ./gradlew :baselineprofile:pixel6Api31BenchmarkAndroidTest -Pandroid.testInstrumentationRunnerArguments.androidx.benchmark.enabledRules=BaselineProfile
 *
 * The generated profile is automatically copied into android-app/src/main/baseline-prof.txt
 * and bundled into the release APK for AOT compilation at install time.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class CatalogBaselineProfile {

    @get:Rule
    val baselineProfileRule = BaselineProfileRule()

    @Test
    fun catalogScrollProfile() {
        baselineProfileRule.collect(
            packageName = "com.onetill",
        ) {
            // Cold start the app — this captures startup + initial composition
            pressHome()
            startActivityAndWait()

            // Wait for the product grid to appear (catalog loads after splash)
            device.wait(Until.hasObject(By.desc("Search products")), 10_000)

            // Scroll the product grid to capture LazyVerticalGrid + Coil image decode paths
            val grid = device.findObject(By.scrollable(true))
            if (grid != null) {
                repeat(3) {
                    grid.fling(Direction.DOWN)
                    device.waitForIdle()
                }
                repeat(3) {
                    grid.fling(Direction.UP)
                    device.waitForIdle()
                }
            }
        }
    }
}
