package io.horizontalsystems.walletkit.modules.coin

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * The coin page renders whatever the market API returns in its links map. A row that isn't a
 * usable URL used to take the process down rather than the row.
 */
class WebsiteHostTest {

    @Test
    fun stripsWww() {
        assertEquals("example.com", websiteHost("https://www.example.com/path"))
    }

    @Test
    fun keepsHostWithoutWww() {
        assertEquals("example.com", websiteHost("https://example.com"))
    }

    @Test
    fun hostlessLinkIsNull() {
        // No authority, so URI.host is null — this is the NPE the crash came from.
        assertNull(websiteHost("not a url"))
        assertNull(websiteHost("mailto:someone@example.com"))
    }

    @Test
    fun malformedLinkIsNull() {
        assertNull(websiteHost("http://[malformed"))
    }
}
