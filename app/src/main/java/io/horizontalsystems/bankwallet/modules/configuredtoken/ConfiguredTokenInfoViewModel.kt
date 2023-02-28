package io.horizontalsystems.bankwallet.modules.configuredtoken

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.IAccountManager
import io.horizontalsystems.bankwallet.core.imageUrl
import io.horizontalsystems.bankwallet.core.managers.RestoreSettingsManager
import io.horizontalsystems.bankwallet.entities.ConfiguredToken
import io.horizontalsystems.bankwallet.modules.address.*
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.marketkit.models.TokenType

class ConfiguredTokenInfoViewModel(
    private val configuredToken: ConfiguredToken,
    private val accountManager: IAccountManager,
    private val restoreSettingsManager: RestoreSettingsManager
) : ViewModel() {

    val type: ConfiguredTokenInfoType?

    init {
        val token = configuredToken.token
        type = when (val type = token.type) {
            is TokenType.Eip20 -> {
                ConfiguredTokenInfoType.Contract(type.address, token.blockchain.type.imageUrl, token.blockchain.explorerUrl?.replace("\$ref", type.address))
            }
            is TokenType.Bep2 -> {
                ConfiguredTokenInfoType.Contract(type.symbol, token.blockchain.type.imageUrl, token.blockchain.explorerUrl?.replace("\$ref", type.symbol))
            }
            is TokenType.Spl -> {
                ConfiguredTokenInfoType.Contract(type.address, token.blockchain.type.imageUrl, token.blockchain.explorerUrl?.replace("\$ref", type.address))
            }
            TokenType.Native -> when (token.blockchainType) {
                BlockchainType.Bitcoin,
                BlockchainType.Litecoin -> {
                    ConfiguredTokenInfoType.Bips
                }
                BlockchainType.BitcoinCash -> {
                    ConfiguredTokenInfoType.Bch
                }
                BlockchainType.Zcash -> {
                    ConfiguredTokenInfoType.BirthdayHeight(getBirthdayHeight(token))
                }
                else -> null
            }
            is TokenType.Unsupported -> null
        }
    }

    fun getBirthdayHeight(token: Token): Long? {
        val account = accountManager.activeAccount ?: return null
        val restoreSettings = restoreSettingsManager.settings(account, token.blockchainType)

        return restoreSettings.birthdayHeight
    }

    class Factory(private val configuredToken: ConfiguredToken) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ConfiguredTokenInfoViewModel(
                configuredToken,
                App.accountManager,
                App.restoreSettingsManager
            ) as T
        }
    }

}
