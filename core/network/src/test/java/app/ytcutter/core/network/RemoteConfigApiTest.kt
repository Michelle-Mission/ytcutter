package app.ytcutter.core.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RemoteConfigApiTest {

    private val api = RemoteConfigApi("nurfilardi", "ytcutter")

    @Test
    fun `parses announcement and flags`() {
        val json = """
        {
          "schemaVersion": 1,
          "maintenance": {"enabled": false, "message": "d"},
          "announcement": {"id": "a1", "title": "Uji", "message": "Halo", "url": "https://x"},
          "features": {"downloader": false, "player": true},
          "minVersionCode": 30
        }
        """.trimIndent()
        val config = api.parseConfig(json)
        assertNotNull(config)
        config!!
        assertEquals("a1", config.announcement?.id)
        assertEquals("Halo", config.announcement?.message)
        assertFalse(config.features.downloader)
        assertTrue(config.features.player)
        assertEquals(30L, config.minVersionCode)
        assertFalse(config.maintenance.enabled)
    }

    @Test
    fun `empty announcement becomes null`() {
        val config = api.parseConfig("""{"announcement":{"id":"","message":""}}""")
        assertNotNull(config)
        assertNull(config!!.announcement)
    }

    @Test
    fun `parses changelog entries`() {
        val entries = api.parseChangelog("""{"entries":[{"label":"1.0.0","items":["a","b"]}]}""")
        assertEquals(1, entries?.size)
        assertEquals(listOf("a", "b"), entries?.first()?.items)
    }
}
