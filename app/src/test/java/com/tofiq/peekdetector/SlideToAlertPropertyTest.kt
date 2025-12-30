package com.tofiq.peekdetector

import com.tofiq.peekdetector.ui.calculateSwipeProgress
import com.tofiq.peekdetector.ui.shouldTriggerAlert
import com.tofiq.peekdetector.ui.shouldResetSlider
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.float
import io.kotest.property.checkAll

/**
 * Property-based tests for SlideToAlertComponent.
 * 
 * Feature: slide-to-panic-alert, Property 1: Swipe Above Threshold Activates Alert
 * Validates: Requirements 1.2
 * 
 * Feature: slide-to-panic-alert, Property 2: Swipe Below Threshold Resets Slider
 * Validates: Requirements 1.3
 */
class SlideToAlertPropertyTest : FunSpec({

    /**
     * Property 1: Swipe Above Threshold Activates Alert
     * For any swipe gesture where the drag distance exceeds 80% of the slider width,
     * the panic alert system shall transition to the active state.
     * 
     * Validates: Requirements 1.2
     */
    test("Property 1 - Swipe above threshold (>= 0.8) should trigger alert") {
        // Test with progress values at or above threshold
        checkAll(100, Arb.float(PanicAlertConstants.SWIPE_THRESHOLD, 1.0f)) { progress ->
            shouldTriggerAlert(progress) shouldBe true
        }
    }

    test("Property 1 - Swipe exactly at threshold (0.8) should trigger alert") {
        shouldTriggerAlert(PanicAlertConstants.SWIPE_THRESHOLD) shouldBe true
    }

    test("Property 1 - Swipe at 100% should trigger alert") {
        shouldTriggerAlert(1.0f) shouldBe true
    }

    test("Property 1 - Progress calculation is correct for any valid drag offset") {
        checkAll(100, Arb.float(0f, 1000f), Arb.float(100f, 1000f)) { dragOffset, maxDrag ->
            val progress = calculateSwipeProgress(dragOffset, maxDrag)
            // Progress should always be between 0 and 1
            (progress >= 0f && progress <= 1f) shouldBe true
        }
    }

    test("Property 1 - Full drag distance results in 100% progress") {
        val maxDrag = 500f
        val progress = calculateSwipeProgress(maxDrag, maxDrag)
        progress shouldBe 1.0f
    }

    /**
     * Property 2: Swipe Below Threshold Resets Slider
     * For any swipe gesture where the drag distance is less than 80% of the slider width
     * and the gesture ends, the slider position shall return to 0 (starting position).
     * 
     * Validates: Requirements 1.3
     */
    test("Property 2 - Swipe below threshold (< 0.8) should reset slider") {
        // Test with progress values below threshold
        checkAll(100, Arb.float(0f, PanicAlertConstants.SWIPE_THRESHOLD - 0.001f)) { progress ->
            shouldResetSlider(progress) shouldBe true
        }
    }

    test("Property 2 - Swipe at 0% should reset slider") {
        shouldResetSlider(0f) shouldBe true
    }

    test("Property 2 - Swipe just below threshold should reset slider") {
        shouldResetSlider(0.79f) shouldBe true
    }

    test("Property 2 - Swipe at threshold should NOT reset slider") {
        shouldResetSlider(PanicAlertConstants.SWIPE_THRESHOLD) shouldBe false
    }

    test("Property 2 - Swipe above threshold should NOT reset slider") {
        checkAll(100, Arb.float(PanicAlertConstants.SWIPE_THRESHOLD, 1.0f)) { progress ->
            shouldResetSlider(progress) shouldBe false
        }
    }

    test("Property 2 - Zero max drag distance results in zero progress") {
        val progress = calculateSwipeProgress(100f, 0f)
        progress shouldBe 0f
    }

    test("Property 2 - Negative max drag distance results in zero progress") {
        val progress = calculateSwipeProgress(100f, -50f)
        progress shouldBe 0f
    }

    // Mutual exclusivity property: trigger and reset are mutually exclusive
    test("Trigger and reset are mutually exclusive for any progress value") {
        checkAll(100, Arb.float(0f, 1.0f)) { progress ->
            val shouldTrigger = shouldTriggerAlert(progress)
            val shouldReset = shouldResetSlider(progress)
            // Exactly one should be true, never both or neither
            (shouldTrigger xor shouldReset) shouldBe true
        }
    }
})
