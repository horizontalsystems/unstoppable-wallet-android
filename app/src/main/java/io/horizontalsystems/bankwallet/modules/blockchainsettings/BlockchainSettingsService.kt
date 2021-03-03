package io.horizontalsystems.bankwallet.modules.blockchainsettings

import io.horizontalsystems.bankwallet.core.IDerivationSettingsManager
import io.horizontalsystems.bankwallet.core.managers.BitcoinCashCoinTypeManager
import io.horizontalsystems.bankwallet.entities.*
import io.horizontalsystems.bankwallet.entities.AccountType.Derivation
import io.horizontalsystems.coinkit.models.Coin
import io.horizontalsystems.coinkit.models.CoinType
import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject

class BlockchainSettingsService(
        private val derivationSettingsManager: IDerivationSettingsManager,
        private val bitcoinCashCoinTypeManager: BitcoinCashCoinTypeManager
) {

    private val approveEnableCoinSubject = BehaviorSubject.create<Coin>()
    private val rejectEnableCoinSubject = BehaviorSubject.create<Coin>()
    private val requestSubject = BehaviorSubject.create<BlockchainSettingsModule.Request>()

    val approveEnableCoinAsync: Observable<Coin> = approveEnableCoinSubject
    val rejectEnableCoinAsync: Observable<Coin> = rejectEnableCoinSubject
    val requestAsync: Observable<BlockchainSettingsModule.Request> = requestSubject

    fun approveEnable(coin: Coin, accountOrigin: AccountOrigin) {
        val setting = derivationSettingsManager.setting(coin.type)
        if (accountOrigin == AccountOrigin.Restored && setting != null) {
            val request = BlockchainSettingsModule.Request(
                    coin,
                    BlockchainSettingsModule.RequestType.DerivationType(Derivation.values().asList(), setting.derivation)
            )
            requestSubject.onNext(request)
            return
        }

        if (accountOrigin == AccountOrigin.Restored && coin.type == CoinType.BitcoinCash) {
            val request = BlockchainSettingsModule.Request(
                    coin,
                    BlockchainSettingsModule.RequestType.BitcoinCashType(BitcoinCashCoinType.values().asList(), bitcoinCashCoinTypeManager.bitcoinCashCoinType)
            )
            requestSubject.onNext(request)
            return
        }

        approveEnableCoinSubject.onNext(coin)
    }

    fun select(derivation: Derivation, coin: Coin) {
        val setting = DerivationSetting(coin.type, derivation)
        derivationSettingsManager.save(setting)

        approveEnableCoinSubject.onNext(coin)
    }

    fun select(bitcoinCashCoinType: BitcoinCashCoinType, coin: Coin) {
        bitcoinCashCoinTypeManager.save(bitcoinCashCoinType)

        approveEnableCoinSubject.onNext(coin)
    }

    fun cancel(coin: Coin) {
        rejectEnableCoinSubject.onNext(coin)
    }
}
