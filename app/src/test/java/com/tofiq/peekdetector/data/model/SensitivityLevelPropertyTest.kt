package com.tofiq.peekdetector.data.model

import org.junit.Assert.*
import org.junit.Test

/**
 * Property-based tests for SensitivityLevel enum.
 */
class SensitivityLevelPropertyTest {

    @Test
    fun `LOW sensitivity has frameSkip of 5`() {
        assertEquals(5, SensitivityLevel.LOW.frameSkip)
    }

    @Test
    fun `MEDIUM sensitivity has frameSkip of 3`() {
        assertEquals(3, SensitivityLevel.MEDIUM.frameSkip)
    }

    @Test
    fun `HIGH sensitivity has frameSkip of 1`() {
        assertEquals(1, SensitivityLevel.HIGH.frameSkip)
    }

    @Test
    fun `default returns MEDIUM`() {
        assertEquals(SensitivityLevel.MEDIUM, SensitivityLevel.default())
    }

    @Test
    fun `fromString returns correct level for valid input`() {
        assertEquals(SensitivityLevel.LOW, SensitivityLevel.fromString("LOW"))
        assertEquals(SensitivityLevel.MEDIUM, SensitivityLevel.fromString("MEDIUM"))
        assertEquals(SensitivityLevel.HIGH, SensitivityLevel.fromString("HIGH"))
    }

    @Test
    fun `fromString returns default for invalid input`() {
        assertEquals(SensitivityLevel.MEDIUM, SensitivityLevel.fromString("INVALID"))
        assertEquals(SensitivityLevel.MEDIUM, SensitivityLevel.fromString(null))
        assertEquals(SensitivityLevel.MEDIUM, SensitivityLevel.fromString(""))
    }
}
