package com.tofiq.peekdetector.data

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.enum
import io.kotest.property.checkAll

/**
 * Property-based tests for SensitivityLevel frame skip mapping.
 * 
 * Feature: settings-screen, Property 2: Sensitivity Level Frame Skip Mapping
 * Validates: Requirements 2.2, 2.3, 2.4
 */
class SensitivityLevelPropertyTest : FunSpec({

    /**
     * Property 2: Sensitivity Level Frame Skip Mapping
     * For any sensitivity level selection, the frame skip value used by the analyzer 
     * SHALL match the defined mapping (LOW→5, MEDIUM→3, HIGH→1).
     * 
     * Validates: Requirements 2.2, 2.3, 2.4
     */
    test("Property 2 - All sensitivity levels map to correct frame skip values") {
        checkAll(100, Arb.enum<SensitivityLevel>()) { level ->
            val expectedFrameSkip = when (level) {
                SensitivityLevel.LOW -> 5      // Requirement 2.2: Process every 5th frame
                SensitivityLevel.MEDIUM -> 3   // Requirement 2.3: Process every 3rd frame
                SensitivityLevel.HIGH -> 1     // Requirement 2.4: Process every frame
            }
            level.frameSkip shouldBe expectedFrameSkip
        }
    }

    test("Property 2 - LOW sensitivity processes every 5th frame") {
        SensitivityLevel.LOW.frameSkip shouldBe 5
    }

    test("Property 2 - MEDIUM sensitivity processes every 3rd frame") {
        SensitivityLevel.MEDIUM.frameSkip shouldBe 3
    }

    test("Property 2 - HIGH sensitivity processes every frame") {
        SensitivityLevel.HIGH.frameSkip shouldBe 1
    }

    test("Property 2 - Frame skip values are positive") {
        checkAll(100, Arb.enum<SensitivityLevel>()) { level ->
            (level.frameSkip > 0) shouldBe true
        }
    }

    test("Property 2 - Higher sensitivity means lower frame skip") {
        // LOW has highest frame skip (least sensitive)
        // HIGH has lowest frame skip (most sensitive)
        (SensitivityLevel.LOW.frameSkip > SensitivityLevel.MEDIUM.frameSkip) shouldBe true
        (SensitivityLevel.MEDIUM.frameSkip > SensitivityLevel.HIGH.frameSkip) shouldBe true
    }
})
