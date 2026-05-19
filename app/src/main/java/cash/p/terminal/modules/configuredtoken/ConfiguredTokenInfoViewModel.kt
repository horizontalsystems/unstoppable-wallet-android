package cash.p.terminal.modules.configuredtoken

import androidx.lifecycle.ViewModel
import cash.p.terminal.core.assetUrl
import cash.p.terminal.core.eip20TokenUrl
import cash.p.terminal.core.iconPlaceholder
import cash.p.terminal.core.jettonUrl
import cash.p.terminal.core.managers.RestoreSettingsManager
import cash.p.terminal.modules.configuredtoken.ConfiguredTokenInfoType.Bips
import cash.p.terminal.modules.configuredtoken.ConfiguredTokenInfoType.BirthdayHeight
import cash.p.terminal.modules.configuredtoken.ConfiguredTokenInfoType.Contract
import cash.p.terminal.ui_compose.components.ImageSource
import cash.p.terminal.wallet.IAccountManager
import cash.p.terminal.wallet.Token
import cash.p.terminal.wallet.alternativeImageUrl
import cash.p.terminal.wallet.entities.TokenType
import cash.p.terminal.wallet.imageUrl
import io.horizontalsystems.core.entities.BlockchainType
import io.horizontalsystems.core.imageUrl

class ConfiguredTokenInfoViewModel(
    token: Token,
    private val accountManager: IAccountManager,
    private val restoreSettingsManager: RestoreSettingsManager
) : ViewModel() {

    val uiState: ConfiguredTokenInfoUiState

    init {
        val type = when (val type = token.type) {
            is TokenType.Eip20 -> {
                Contract(
                    type.address,
                    token.blockchain.type.imageUrl,
                    token.blockchain.eip20TokenUrl(type.address)
                )
            }

            is TokenType.Spl -> {
                Contract(
                    type.address,
                    token.blockchain.type.imageUrl,
                    token.blockchain.eip20TokenUrl(type.address)
                )
            }

            is TokenType.Jetton -> {
                Contract(
                    type.address,
                    token.blockchain.type.imageUrl,
                    token.blockchain.jettonUrl(type.address)
                )
            }

            is TokenType.Asset -> {
                Contract(
                    "${type.code}:${type.issuer}",
                    token.blockchain.type.imageUrl,
                    token.blockchain.assetUrl(type.code, type.issuer)
                )
            }

            is TokenType.Derived -> {
                Bips(token.blockchain.name)
            }

            is TokenType.AddressTyped -> {
                ConfiguredTokenInfoType.Bch
            }

            TokenType.Mweb -> {
                BirthdayHeight(getBirthdayHeight(token))
            }

            TokenType.Native -> null
            is TokenType.AddressSpecTyped -> when (token.blockchainType) {
                BlockchainType.Zcash -> {
                    BirthdayHeight(getBirthdayHeight(token))
                }

                else -> null
            }

            is TokenType.Unsupported -> null
        }

        uiState = ConfiguredTokenInfoUiState(
            iconSource = ImageSource.Remote(
                token.coin.imageUrl,
                token.iconPlaceholder,
                token.coin.alternativeImageUrl
            ),
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

    data class Bips(val blockchainName: String) : ConfiguredTokenInfoType()
    object Bch : ConfiguredTokenInfoType()
    data class BirthdayHeight(val height: Long?) : ConfiguredTokenInfoType()
}
