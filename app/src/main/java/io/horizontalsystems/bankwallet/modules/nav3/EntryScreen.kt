package io.horizontalsystems.bankwallet.modules.nav3

import androidx.compose.animation.Crossfade
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.modules.intro.IntroScreen
import kotlinx.serialization.Serializable

@Serializable
data object EntryScreen : HSScreen() {
    @Composable
    override fun GetContent(navController: HSNavigation) {
        val mainShowedOnce by
            App.localStorage.mainShowedOnceFlow.collectAsStateWithLifecycle()

        Crossfade(mainShowedOnce) {
            val screen = if (it) {
                MainScreen
            } else {
                IntroScreen
            }

            screen.GetContent(navController)
        }
    }
}
