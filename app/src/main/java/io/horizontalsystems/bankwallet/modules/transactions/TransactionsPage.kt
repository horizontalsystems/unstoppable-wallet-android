package io.horizontalsystems.bankwallet.modules.transactions

import androidx.compose.runtime.Composable
import io.horizontalsystems.bankwallet.modules.nav3.EntryPage
import io.horizontalsystems.bankwallet.modules.nav3.HSNavigation
import io.horizontalsystems.bankwallet.modules.nav3.HSPage
import io.horizontalsystems.bankwallet.modules.nav3.viewModelForScreen
import kotlinx.serialization.Serializable

@Serializable
data object TransactionsPage : HSPage() {
    @Composable
    override fun GetContent(navController: HSNavigation) {
        val viewModel = viewModelForScreen<TransactionsViewModel>(EntryPage::class)
        TransactionsScreen(navController, viewModel)
    }
}
