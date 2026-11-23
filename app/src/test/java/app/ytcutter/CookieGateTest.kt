package app.ytcutter

import app.ytcutter.data.CookieGate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.File

class CookieGateTest {

    @Test
    fun disabledNeverSendsCookies() {
        assertNull(CookieGate.path(File("/files/cookies.txt"), enabled = false))
    }

    @Test
    fun enabledReturnsExistingFile() {
        val file = File.createTempFile("cookies", ".txt").apply { writeText("x") }
        file.deleteOnExit()
        assertEquals(file.absolutePath, CookieGate.path(file, enabled = true))
    }

    @Test
    fun missingFileStaysNull() {
        assertNull(CookieGate.path(null, enabled = true))
    }
}