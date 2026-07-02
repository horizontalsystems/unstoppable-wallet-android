package io.horizontalsystems.core.modules.transactions

import androidx.compose.runtime.Composable
import io.horizontalsystems.core.modules.nav3.EntryPage
import io.horizontalsystems.core.modules.nav3.HSNavigation
import io.horizontalsystems.core.modules.nav3.HSPage
import kotlinx.serialization.Serializable

@Serializable
data object TransactionsPage : HSPage() {
    @Composable
    override fun GetContent(navigation: HSNavigation) {
        val viewModel = navigation.viewModelForScreen<TransactionsViewModel>(EntryPage::class)
        TransactionsScreen(navigation, viewModel)
    }
}
