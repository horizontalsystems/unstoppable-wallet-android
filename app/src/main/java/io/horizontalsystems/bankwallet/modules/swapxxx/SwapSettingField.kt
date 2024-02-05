package cash.p.terminal.modules.swapxxx

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import cash.p.terminal.entities.Address
import cash.p.terminal.modules.swap.settings.ui.RecipientAddress
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