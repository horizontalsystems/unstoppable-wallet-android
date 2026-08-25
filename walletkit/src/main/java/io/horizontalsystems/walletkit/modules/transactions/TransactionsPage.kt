package io.horizontalsystems.walletkit.modules.transactions

import androidx.compose.runtime.Composable
import io.horizontalsystems.walletkit.modules.nav3.EntryPage
import io.horizontalsystems.walletkit.modules.nav3.HSNavigation
import io.horizontalsystems.walletkit.modules.nav3.HSPage
import kotlinx.serialization.Serializable

@Serializable
data object TransactionsPage : HSPage() {
    @Composable
    override fun GetContent(navigation: HSNavigation) {
        val viewModel = navigation.viewModelForScreen<TransactionsViewModel>(EntryPage::class, TransactionsModule.Factory())
        TransactionsScreen(navigation, viewModel)
    }
}
