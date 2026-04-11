package cash.p.terminal.core.notifications

import android.content.SharedPreferences
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NotificationDeduplicatorTest {

    private val store = mutableMapOf<String, Any?>()
    private lateinit var prefs: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor

    @Before
    fun setUp() {
        store.clear()

        editor = mockk(relaxed = true)
        every { editor.putLong(any(), any()) } answers {
            store[firstArg()] = secondArg<Long>()
            editor
        }
        every { editor.putStringSet(any(), any()) } answers {
            store[firstArg()] = secondArg<Set<String>?>()?.toSet()
            editor
        }
        every { editor.remove(any()) } answers {
            store.remove(firstArg())
            editor
        }
        every { editor.apply() } just Runs

        prefs = mockk(relaxed = true)
        every { prefs.edit() } returns editor
        every { prefs.all } answers { store.toMap() }
        every { prefs.getLong(any(), any()) } answers {
            (store[firstArg()] as? Long) ?: secondArg()
        }
        every { prefs.contains(any()) } answers { store.containsKey(firstArg()) }
    }

    @Test
    fun isNew_noLastCheckTime_returnsFalse() {
        val deduplicator = NotificationDeduplicator(prefs)

        assertFalse(deduplicator.isNew("uid-1", "bitcoin", 100L))
    }

    @Test
    fun isNew_sameUidSecondTime_returnsFalse() {
        val deduplicator = NotificationDeduplicator(prefs)
        deduplicator.markNotified("uid-1")

        assertFalse(deduplicator.isNew("uid-1", "bitcoin", 100L))
    }

    @Test
    fun isNew_differentUid_returnsTrue() {
        store["push_last_check_bitcoin"] = 50L
        val deduplicator = NotificationDeduplicator(prefs)
        deduplicator.markNotified("uid-1")

        assertTrue(deduplicator.isNew("uid-2", "bitcoin", 100L))
    }

    @Test
    fun isNew_timestampBelowLastCheck_returnsFalse() {
        store["push_last_check_bitcoin"] = 100L
        val deduplicator = NotificationDeduplicator(prefs)

        assertFalse(deduplicator.isNew("uid-1", "bitcoin", 50L))
    }

    @Test
    fun isNew_timestampAboveLastCheck_returnsTrue() {
        store["push_last_check_bitcoin"] = 100L
        val deduplicator = NotificationDeduplicator(prefs)

        assertTrue(deduplicator.isNew("uid-1", "bitcoin", 150L))
    }

    @Test
    fun isNew_sameTimestampDifferentUidAfterUpdateWithRecordedUids_returnsTrue() {
        store["push_last_check_bitcoin"] = 99L
        val deduplicator = NotificationDeduplicator(prefs)

        assertTrue(deduplicator.isNew("uid-1", "bitcoin", 100L))
        deduplicator.markNotified("uid-1")
        deduplicator.updateLastCheckTime("bitcoin", 100L, setOf("uid-1"))

        assertTrue(deduplicator.isNew("uid-2", "bitcoin", 100L))
    }

    @Test
    fun isNew_sameTimestampDifferentUidAfterRestore_returnsTrue() {
        store["push_last_check_bitcoin"] = 100L
        store["push_last_check_uids_bitcoin"] = setOf("uid-1")
        val deduplicator = NotificationDeduplicator(prefs)

        assertFalse(deduplicator.isNew("uid-1", "bitcoin", 100L))
        assertTrue(deduplicator.isNew("uid-2", "bitcoin", 100L))
    }

    @Test
    fun updateLastCheckTime_persistsToPreferences() {
        val deduplicator = NotificationDeduplicator(prefs)

        deduplicator.updateLastCheckTime("bitcoin", 123L, setOf("uid-1"))

        verify { editor.putLong("push_last_check_bitcoin", 123L) }
        verify { editor.putStringSet("push_last_check_uids_bitcoin", setOf("uid-1")) }
        verify { editor.apply() }
    }

    @Test
    fun reset_clearsNotifiedButKeepsLastCheckTimes() {
        store["push_last_check_bitcoin"] = 100L
        val deduplicator = NotificationDeduplicator(prefs)

        deduplicator.markNotified("uid-1")
        assertFalse(deduplicator.isNew("uid-1", "bitcoin", 150L))

        deduplicator.reset()

        // uid cleared -- now recognized as new again
        assertTrue(deduplicator.isNew("uid-1", "bitcoin", 150L))

        // lastCheckTime-based filtering still works
        assertFalse(deduplicator.isNew("uid-2", "bitcoin", 50L))
    }

    @Test
    fun updateLastCheckTime_smallerTimestamp_doesNotOverwriteCurrentValue() {
        val deduplicator = NotificationDeduplicator(prefs)

        deduplicator.updateLastCheckTime("bitcoin", 1000L)
        deduplicator.updateLastCheckTime("bitcoin", 500L)

        assertTrue(deduplicator.isNew("uid-1", "bitcoin", 1001L))
        assertFalse(deduplicator.isNew("uid-2", "bitcoin", 999L))
    }
}
