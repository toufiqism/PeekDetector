package com.tofiq.peekdetector.data.model

import org.junit.Assert.*
import org.junit.Test

/**
 * Property-based tests for ThemeMode enum.
 */
class ThemeModePropertyTest {

    @Test
    fun `default returns SYSTEM`() {
        assertEquals(ThemeMode.SYSTEM, ThemeMode.default())
    }

    @Test
    fun `fromString returns correct mode for valid input`() {
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromString("SYSTEM"))
        assertEquals(ThemeMode.LIGHT, ThemeMode.fromString("LIGHT"))
        assertEquals(ThemeMode.DARK, ThemeMode.fromString("DARK"))
    }

    @Test
    fun `fromString returns default for invalid input`() {
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromString("INVALID"))
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromString(null))
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromString(""))
    }

    @Test
    fun `all entries are present`() {
        val entries = ThemeMode.entries
        assertEquals(3, entries.size)
        assertTrue(entries.contains(ThemeMode.SYSTEM))
        assertTrue(entries.contains(ThemeMode.LIGHT))
        assertTrue(entries.contains(ThemeMode.DARK))
    }
}
