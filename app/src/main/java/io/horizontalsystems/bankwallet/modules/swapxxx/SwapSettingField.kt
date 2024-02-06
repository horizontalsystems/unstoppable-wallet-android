package cash.p.terminal.modules.swapxxx

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import cash.p.terminal.entities.Address
import cash.p.terminal.modules.swap.settings.ui.RecipientAddress
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.ethereumkit.models.Address as EthereumKitAddress

interface SwapSettingField {
    val id: String

    @Composable
    fun GetContent(
        navController: NavController,
        onError: (Throwable?) -> Unit,
        onValueChange: (Any?) -> Unit
    )
}

data class SwapSettingFieldRecipient(
    val blockchainType: BlockchainType,
    val settings: Map<String, Any?>
) : SwapSettingField {
    override val id = "recipient"

    val value = settings[id] as? Address

    @Composable
    override fun GetContent(
        navController: NavController,
        onError: (Throwable?) -> Unit,
        onValueChange: (Any?) -> Unit
    ) {
        RecipientAddress(
            blockchainType = blockchainType,
            navController = navController,
            initial = value,
            onError = onError,
            onValueChange = onValueChange
        )
    }

    fun getEthereumKitAddress(): EthereumKitAddress? {
        val hex = value?.hex ?: return null

        return try {
            EthereumKitAddress(hex)
        } catch (err: Exception) {
            null
        }
    }
}