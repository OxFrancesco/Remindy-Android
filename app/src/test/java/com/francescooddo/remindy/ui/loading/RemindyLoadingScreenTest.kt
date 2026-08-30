package com.francescooddo.remindy.ui.loading

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RemindyLoadingScreenTest {

    @Test
    fun `pulse travels from the inner wave to the outer wave`() {
        val start = loadingFrame(0f)
        val finish = loadingFrame(1f)

        assertTrue(start.innerAlpha > start.outerAlpha)
        assertTrue(finish.outerAlpha > finish.innerAlpha)
        assertTrue(finish.dotScale < start.dotScale)
    }

    @Test
    fun `animation progress is clamped`() {
        assertEquals(loadingFrame(0f), loadingFrame(-1f))
        assertEquals(loadingFrame(1f), loadingFrame(2f))
    }
}
