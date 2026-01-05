package com.tofiq.peekdetector.feature.panic.ui

import org.junit.Assert.*
import org.junit.Test

/**
 * Property-based tests for SlideToAlertComponent logic.
 * Tests the pure functions exposed for testing purposes.
 */
class SlideToAlertPropertyTest {

    @Test
    fun `calculateSwipeProgress returns 0 when maxDragDistance is 0`() {
        val result = calculateSwipeProgress(100f, 0f)
        assertEquals(0f, result, 0.001f)
    }

    @Test
    fun `calculateSwipeProgress returns 0 when maxDragDistance is negative`() {
        val result = calculateSwipeProgress(100f, -50f)
        assertEquals(0f, result, 0.001f)
    }

    @Test
    fun `calculateSwipeProgress returns correct ratio`() {
        val result = calculateSwipeProgress(50f, 100f)
        assertEquals(0.5f, result, 0.001f)
    }

    @Test
    fun `calculateSwipeProgress clamps to 1 when dragOffset exceeds maxDragDistance`() {
        val result = calculateSwipeProgress(150f, 100f)
        assertEquals(1f, result, 0.001f)
    }

    @Test
    fun `calculateSwipeProgress clamps to 0 when dragOffset is negative`() {
        val result = calculateSwipeProgress(-50f, 100f)
        assertEquals(0f, result, 0.001f)
    }

    @Test
    fun `shouldTriggerAlert returns true when progress is at threshold`() {
        val result = shouldTriggerAlert(0.8f)
        assertTrue(result)
    }

    @Test
    fun `shouldTriggerAlert returns true when progress exceeds threshold`() {
        val result = shouldTriggerAlert(0.9f)
        assertTrue(result)
    }

    @Test
    fun `shouldTriggerAlert returns false when progress is below threshold`() {
        val result = shouldTriggerAlert(0.79f)
        assertFalse(result)
    }

    @Test
    fun `shouldResetSlider returns true when progress is below threshold`() {
        val result = shouldResetSlider(0.5f)
        assertTrue(result)
    }

    @Test
    fun `shouldResetSlider returns false when progress is at threshold`() {
        val result = shouldResetSlider(0.8f)
        assertFalse(result)
    }

    @Test
    fun `shouldResetSlider returns false when progress exceeds threshold`() {
        val result = shouldResetSlider(0.9f)
        assertFalse(result)
    }
}
