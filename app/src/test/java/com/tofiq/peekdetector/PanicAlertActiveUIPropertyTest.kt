package com.tofiq.peekdetector

import com.tofiq.peekdetector.ui.getUIVisibility
import com.tofiq.peekdetector.ui.isSliderVisible
import com.tofiq.peekdetector.ui.isStopButtonVisible
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.boolean
import io.kotest.property.checkAll

/**
 * Property-based tests for PanicAlertActiveUI.
 * 
 * Feature: slide-to-panic-alert, Property 5: Alert State Controls UI Visibility
 * Validates: Requirements 3.1, 4.3
 */
class PanicAlertActiveUIPropertyTest : FunSpec({

    /**
     * Property 5: Alert State Controls UI Visibility
     * For any alert state, when active the stop button shall be visible and the slider 
     * shall be hidden, and when inactive the slider shall be visible and the stop button 
     * shall be hidden.
     * 
     * Validates: Requirements 3.1, 4.3
     */
    test("Property 5 - When alert is active, stop button is visible and slider is hidden") {
        val isActive = true
        isStopButtonVisible(isActive) shouldBe true
        isSliderVisible(isActive) shouldBe false
    }

    test("Property 5 - When alert is inactive, slider is visible and stop button is hidden") {
        val isActive = false
        isSliderVisible(isActive) shouldBe true
        isStopButtonVisible(isActive) shouldBe false
    }

    test("Property 5 - For any alert state, slider and stop button visibility are mutually exclusive") {
        checkAll(100, Arb.boolean()) { isAlertActive ->
            val sliderVisible = isSliderVisible(isAlertActive)
            val stopButtonVisible = isStopButtonVisible(isAlertActive)
            
            // Exactly one should be visible at any time
            (sliderVisible xor stopButtonVisible) shouldBe true
        }
    }

    test("Property 5 - getUIVisibility returns correct pair for active state") {
        val (sliderVisible, stopButtonVisible) = getUIVisibility(true)
        sliderVisible shouldBe false
        stopButtonVisible shouldBe true
    }

    test("Property 5 - getUIVisibility returns correct pair for inactive state") {
        val (sliderVisible, stopButtonVisible) = getUIVisibility(false)
        sliderVisible shouldBe true
        stopButtonVisible shouldBe false
    }

    test("Property 5 - For any alert state, getUIVisibility is consistent with individual functions") {
        checkAll(100, Arb.boolean()) { isAlertActive ->
            val (sliderVisible, stopButtonVisible) = getUIVisibility(isAlertActive)
            
            sliderVisible shouldBe isSliderVisible(isAlertActive)
            stopButtonVisible shouldBe isStopButtonVisible(isAlertActive)
        }
    }

    test("Property 5 - Active state always shows stop button (Requirement 3.1)") {
        // Requirement 3.1: WHILE the Alert_State is active, THE Panic_Alert_System SHALL display a prominent stop button
        isStopButtonVisible(true) shouldBe true
    }

    test("Property 5 - Active state always hides slider (Requirement 4.3)") {
        // Requirement 4.3: WHILE the Alert_State is active, THE Slide_To_Alert_Component SHALL be hidden or disabled
        isSliderVisible(true) shouldBe false
    }
})
