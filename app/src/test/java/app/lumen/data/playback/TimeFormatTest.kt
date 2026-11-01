package app.lumen.data.playback

import org.junit.Assert.assertEquals
import org.junit.Test

class TimeFormatTest {
    @Test
    fun clock() {
        assertEquals("0:00", formatClock(0))
        assertEquals("0:09", formatClock(9_999))
        assertEquals("1:05", formatClock(65_000))
        assertEquals("1:02:03", formatClock(3_723_000))
        assertEquals("0:00", formatClock(-5_000))
    }

    @Test
    fun seekDelta() {
        assertEquals("+0:23", formatSeekDelta(23_000))
        assertEquals("\u22120:15", formatSeekDelta(-15_000))
        assertEquals("+1:00:00", formatSeekDelta(3_600_000))
    }
}
