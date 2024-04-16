package io.horizontalsystems.bankwallet.modules.multiswap.settings

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.entities.Address
import io.horizontalsystems.bankwallet.modules.swap.settings.ui.RecipientAddress
import io.horizontalsystems.marketkit.models.BlockchainType

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
