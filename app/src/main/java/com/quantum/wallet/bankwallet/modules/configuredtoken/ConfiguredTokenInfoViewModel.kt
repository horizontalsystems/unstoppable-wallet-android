package com.quantum.wallet.bankwallet.modules.configuredtoken

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.quantum.wallet.bankwallet.core.App
import com.quantum.wallet.bankwallet.core.IAccountManager
import com.quantum.wallet.bankwallet.core.alternativeImageUrl
import com.quantum.wallet.bankwallet.core.assetUrl
import com.quantum.wallet.bankwallet.core.eip20TokenUrl
import com.quantum.wallet.bankwallet.core.iconPlaceholder
import com.quantum.wallet.bankwallet.core.imageUrl
import com.quantum.wallet.bankwallet.core.jettonUrl
import com.quantum.wallet.bankwallet.core.managers.RestoreSettingsManager
import com.quantum.wallet.bankwallet.modules.market.ImageSource
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.marketkit.models.TokenType

class ConfiguredTokenInfoViewModel(
    private val token: Token,
    private val accountManager: IAccountManager,
    private val restoreSettingsManager: RestoreSettingsManager
) : ViewModel() {

    val uiState: ConfiguredTokenInfoUiState

    init {
        val type = when (val type = token.type) {
            is TokenType.Eip20 -> {
                ConfiguredTokenInfoType.Contract(type.address, token.blockchain.type.imageUrl, token.blockchain.eip20TokenUrl(type.address))
            }
            is TokenType.Spl -> {
                ConfiguredTokenInfoType.Contract(type.address, token.blockchain.type.imageUrl, token.blockchain.eip20TokenUrl(type.address))
            }
            is TokenType.Jetton -> {
                ConfiguredTokenInfoType.Contract(type.address, token.blockchain.type.imageUrl, token.blockchain.jettonUrl(type.address))
            }
            is TokenType.Asset -> {
                ConfiguredTokenInfoType.Contract("${type.code}-${type.issuer}", token.blockchain.type.imageUrl, token.blockchain.assetUrl(type.code, type.issuer))
            }
            is TokenType.Derived -> {
                ConfiguredTokenInfoType.Bips(token.blockchain.name)
            }
            is TokenType.AddressTyped -> {
                ConfiguredTokenInfoType.Bch
            }
            TokenType.Native -> when (token.blockchainType) {
                BlockchainType.Monero,
                BlockchainType.Zcash -> {
                    ConfiguredTokenInfoType.BirthdayHeight(getBirthdayHeight(token))
                }
                else -> null
            }
            is TokenType.Unsupported -> null
        }

        uiState = ConfiguredTokenInfoUiState(
            iconSource = ImageSource.Remote(token.coin.imageUrl, token.iconPlaceholder, token.coin.alternativeImageUrl),
            title = token.coin.code,
            subtitle = token.coin.name,
            tokenInfoType = type
        )
    }

    private fun getBirthdayHeight(token: Token): Long? {
        val account = accountManager.activeAccount ?: return null
        val restoreSettings = restoreSettingsManager.settings(account, token.blockchainType)

        return restoreSettings.birthdayHeight
    }

    class Factory(private val token: Token) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ConfiguredTokenInfoViewModel(
                token,
                App.accountManager,
                App.restoreSettingsManager
            ) as T
        }
    }

}

data class ConfiguredTokenInfoUiState(
    val iconSource: ImageSource,
    val title: String,
    val subtitle: String,
    val tokenInfoType: ConfiguredTokenInfoType?
)

sealed class ConfiguredTokenInfoType {
    data class Contract(
        val reference: String,
        val platformImageUrl: String,
        val explorerUrl: String?
    ) : ConfiguredTokenInfoType()

    data class Bips(val blockchainName: String): ConfiguredTokenInfoType()
    object Bch: ConfiguredTokenInfoType()
    data class BirthdayHeight(val height: Long?): ConfiguredTokenInfoType()
}