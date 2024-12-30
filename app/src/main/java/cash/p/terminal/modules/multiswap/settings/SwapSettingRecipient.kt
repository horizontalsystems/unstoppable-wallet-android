package cash.p.terminal.modules.multiswap.settings

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import cash.p.terminal.entities.Address
import cash.p.terminal.modules.multiswap.settings.ui.RecipientAddress
import io.horizontalsystems.core.entities.BlockchainType

data class SwapSettingRecipient(
    val settings: Map<String, Any?>,
    val blockchainType: BlockchainType
) : ISwapSetting {
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

    fun getEthereumKitAddress(): io.horizontalsystems.ethereumkit.models.Address? {
        val hex = value?.hex ?: return null

        return try {
            io.horizontalsystems.ethereumkit.models.Address(hex)
        } catch (err: Exception) {
            null
        }
    }
}
