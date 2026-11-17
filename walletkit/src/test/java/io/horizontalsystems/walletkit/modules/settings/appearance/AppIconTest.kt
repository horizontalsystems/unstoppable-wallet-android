package io.horizontalsystems.walletkit.modules.settings.appearance

import io.horizontalsystems.walletkit.ui.compose.TranslatableString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppIconTest {

    private val retiredIcons = listOf(
        AppIcon.Monero, AppIcon.ZCash, AppIcon.Pepe, AppIcon.Doge, AppIcon.Punk,
        AppIcon.Ape, AppIcon.Plflag, AppIcon.Sinwar, AppIcon.Yeschad, AppIcon.Gigachad,
    )

    private fun shownTitle(icon: AppIcon) = (icon.title as TranslatableString.PlainString).text

    @Test
    fun picker_offersMainOrangeMono8BallInThatOrder() {
        assertEquals(
            listOf(AppIcon.Main, AppIcon.Dark, AppIcon.Mono, AppIcon.Ball8),
            AppIcon.getActiveIcons()
        )
        assertEquals(
            listOf("Main", "Orange", "Mono", "8Ball"),
            AppIcon.getActiveIcons().map(::shownTitle)
        )
    }

    @Test
    fun everyOtherIcon_isRetired() {
        assertEquals(
            retiredIcons.toSet(),
            AppIcon.entries.filter { it.isDeprecated }.toSet()
        )
    }

    // A retired icon may still be the one stored on a device or written to a
    // backup: it has to be recognised to be replaced with the main one.
    @Test
    fun retiredIcons_areStillFoundByStoredNameAndBackupTitle() {
        retiredIcons.forEach {
            assertEquals(it, AppIcon.fromString(it.name))
            assertEquals(it, AppIcon.fromTitle(it.titleText))
        }
    }

    // An icon removed altogether is unknown; the app then applies the main one
    @Test
    fun removedIcons_areNotFound() {
        listOf("Leo", "Mustang", "Yak").forEach {
            assertNull(AppIcon.fromString(it))
            assertNull(AppIcon.fromTitle(it))
        }
    }

    @Test
    fun darkIcon_isShownAsOrange() {
        assertEquals("Orange", shownTitle(AppIcon.Dark))
    }

    @Test
    fun darkIcon_keepsTheTitleBackupsWereWrittenWith() {
        assertEquals("Dark", AppIcon.Dark.titleText)
        assertEquals(AppIcon.Dark, AppIcon.fromTitle("Dark"))
    }

    @Test
    fun darkIcon_keepsTheNameItIsStoredUnder() {
        assertEquals(AppIcon.Dark, AppIcon.fromString("Dark"))
    }

    @Test
    fun ball8Icon_keepsItsStoredNameAndBackupTitle() {
        assertEquals("8ball", AppIcon.Ball8.titleText)
        assertEquals(AppIcon.Ball8, AppIcon.fromTitle("8ball"))
        assertEquals(AppIcon.Ball8, AppIcon.fromString("Ball8"))
    }

    @Test
    fun otherIcons_areShownUnderTheirStoredTitle() {
        AppIcon.entries
            .filter { it != AppIcon.Dark && it != AppIcon.Ball8 }
            .forEach { assertEquals(it.titleText, shownTitle(it)) }
    }

    @Test
    fun titles_areUnique() {
        val titles = AppIcon.entries.map { it.titleText }
        assertTrue(titles.size == titles.toSet().size)
    }
}
