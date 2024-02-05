package io.horizontalsystems.bankwallet.modules.swapxxx

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.entities.Address
import io.horizontalsystems.bankwallet.modules.swap.settings.ui.RecipientAddress
import io.horizontalsystems.marketkit.models.BlockchainType

interface SwapSettingField {
    val id: String

    @Composable
    fun GetContent(
        navController: NavController,
        initial: Any?,
        onError: (Throwable?) -> Unit,
        onValueChange: (Any?) -> Unit
    )
}

data class SwapSettingFieldRecipient(val blockchainType: BlockchainType) : SwapSettingField {
    override val id = "recipient"

    @Composable
    override fun GetContent(
        navController: NavController,
        initial: Any?,
        onError: (Throwable?) -> Unit,
        onValueChange: (Any?) -> Unit
    ) {
        RecipientAddress(
            blockchainType = blockchainType,
            navController = navController,
            initial = initial as? Address,
            onError = onError,
            onValueChange = onValueChange
        )
    }
}