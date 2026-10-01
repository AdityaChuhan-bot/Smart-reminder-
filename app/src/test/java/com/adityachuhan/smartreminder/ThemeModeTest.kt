package com.adityachuhan.smartreminder

import com.adityachuhan.smartreminder.ui.theme.AppThemeMode
import org.junit.Assert.*
import org.junit.Test

class ThemeModeTest {

    @Test
    fun testAppThemeModeValues() {
        val modes = AppThemeMode.entries
        assertEquals(4, modes.size)
        assertTrue(modes.contains(AppThemeMode.SYSTEM))
        assertTrue(modes.contains(AppThemeMode.LIGHT))
        assertTrue(modes.contains(AppThemeMode.DARK))
        assertTrue(modes.contains(AppThemeMode.AMOLED))
    }

    @Test
    fun testAmoledModeAttributes() {
        val amoled = AppThemeMode.AMOLED
        assertEquals("AMOLED Black", amoled.title)
        assertTrue(amoled.subtitle.contains("000000"))
    }
}
