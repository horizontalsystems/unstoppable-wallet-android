package io.horizontalsystems.bankwallet.modules.nav3

import io.horizontalsystems.bankwallet.modules.main.MainActivityViewModel
import kotlinx.serialization.Serializable

@Serializable
data object MainScreen : HSScreen() {
    // TODO("Nav3: need to find other solution. There should not be mainActivityViewModel")
    lateinit var mainActivityViewModel: MainActivityViewModel

//    @Composable
//    override fun GetContent(backStack: NavBackStack<HSScreen>) {
//        MainScreenWithRootedDeviceCheck(
//            mainActivityViewModel = mainActivityViewModel,
//            backStack = backStack,
//        )
//    }
}
