package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.IWalletManager
import io.horizontalsystems.bankwallet.core.coinSettingType
import io.horizontalsystems.bankwallet.core.defaultSettingsArray
import io.horizontalsystems.bankwallet.entities.*
import io.horizontalsystems.marketkit.models.TokenQuery

class WalletActivator(
    private val walletManager: IWalletManager,
    private val marketKit: MarketKitWrapper,
) {

    fun activateWallets(account: Account, tokenQueries: List<TokenQuery>) {
        val wallets = mutableListOf<Wallet>()

        for (tokenQuery in tokenQueries) {
            val token = marketKit.token(tokenQuery) ?: continue

            val defaultSettingsArray = token.blockchainType.defaultSettingsArray(account.type)

            if (defaultSettingsArray.isEmpty()) {
                wallets.add(Wallet(token, account))
            } else {
                defaultSettingsArray.forEach { coinSettings ->
                    val configuredToken = ConfiguredToken(token, coinSettings)
                    wallets.add(Wallet(configuredToken, account))
                }
            }
        }

        walletManager.save(wallets)
    }

    fun activateBtcWallets(mnemonicDerivation: AccountType.Derivation, account: Account, tokenQueries: List<TokenQuery>) {
        val wallets = mutableListOf<Wallet>()

        for (tokenQuery in tokenQueries) {
            val token = marketKit.token(tokenQuery) ?: continue

            when (tokenQuery.blockchainType.coinSettingType) {
                CoinSettingType.derivation -> {
                    val configuredToken = ConfiguredToken(token,
                        CoinSettings(mapOf(CoinSettingType.derivation to mnemonicDerivation.value)))
                    val wallet = Wallet(configuredToken, account)
                    wallets.add(wallet)
                }
                CoinSettingType.bitcoinCashCoinType -> {
                    val cashWallets = BitcoinCashCoinType.values().map { coinType ->
                        val configuredToken = ConfiguredToken(token, CoinSettings(mapOf(CoinSettingType.bitcoinCashCoinType to coinType.value)))
                        Wallet(configuredToken, account)
                    }
                    wallets.addAll(cashWallets)
                }
                else -> {
                    wallets.add(Wallet(token, account))
                }
            }
        }

        walletManager.save(wallets)
    }

}
