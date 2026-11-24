package app.ytcutter.core.data.update

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class BsPatchTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private fun resource(name: String) =
        tmp.newFile(name).also { file ->
            javaClass.classLoader!!.getResourceAsStream("bspatch/$name")!!.use { input ->
                file.outputStream().use { input.copyTo(it) }
            }
        }

    @Test
    fun `applies bsdiff patch`() {
        val old = resource("old.bin")
        val patch = resource("patch.bsdiff")
        val out = tmp.newFile("out.bin")

        BsPatch.apply(old, patch, out)

        val expected = resource("new.bin").readBytes()
        assertArrayEquals(expected, out.readBytes())
        assertTrue(out.length() > 0)
    }
}