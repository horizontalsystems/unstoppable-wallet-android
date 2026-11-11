package io.horizontalsystems.walletkit.modules.settings.appearance

import io.horizontalsystems.walletkit.ui.compose.TranslatableString
import org.junit.Assert.assertEquals
import org.junit.Test

class AppIconTest {

    private fun shownTitle(icon: AppIcon) = (icon.title as TranslatableString.PlainString).text

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
    fun otherIcons_areShownUnderTheirStoredTitle() {
        AppIcon.entries
            .filter { it != AppIcon.Dark }
            .forEach { assertEquals(it.titleText, shownTitle(it)) }
    }
}
