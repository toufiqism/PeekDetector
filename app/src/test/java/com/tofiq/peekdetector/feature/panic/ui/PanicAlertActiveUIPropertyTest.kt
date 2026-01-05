package com.tofiq.peekdetector.feature.panic.ui

import org.junit.Assert.*
import org.junit.Test

/**
 * Property-based tests for PanicAlertActiveUI logic.
 * Tests the pure functions exposed for testing purposes.
 */
class PanicAlertActiveUIPropertyTest {

    @Test
    fun `getUIVisibility returns slider visible and stop hidden when alert inactive`() {
        val (sliderVisible, stopVisible) = getUIVisibility(false)
        assertTrue(sliderVisible)
        assertFalse(stopVisible)
    }

    @Test
    fun `getUIVisibility returns slider hidden and stop visible when alert active`() {
        val (sliderVisible, stopVisible) = getUIVisibility(true)
        assertFalse(sliderVisible)
        assertTrue(stopVisible)
    }

    @Test
    fun `isSliderVisible returns true when alert inactive`() {
        val result = isSliderVisible(false)
        assertTrue(result)
    }

    @Test
    fun `isSliderVisible returns false when alert active`() {
        val result = isSliderVisible(true)
        assertFalse(result)
    }

    @Test
    fun `isStopButtonVisible returns false when alert inactive`() {
        val result = isStopButtonVisible(false)
        assertFalse(result)
    }

    @Test
    fun `isStopButtonVisible returns true when alert active`() {
        val result = isStopButtonVisible(true)
        assertTrue(result)
    }
}
