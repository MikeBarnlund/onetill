package com.onetill.shared

import kotlin.test.Test
import kotlin.test.assertTrue

class SmokeTest {
    @Test
    fun sharedModuleLoads() {
        assertTrue(platformName().isNotEmpty(), "Platform name should not be empty")
    }
}
