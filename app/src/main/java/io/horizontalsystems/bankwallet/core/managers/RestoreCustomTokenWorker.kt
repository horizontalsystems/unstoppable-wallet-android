package io.horizontalsystems.bankwallet.core.managers

import android.content.Context
import androidx.work.RxWorker
import androidx.work.WorkerParameters
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.INetworkManager
import io.horizontalsystems.bankwallet.entities.CustomToken
import io.horizontalsystems.bankwallet.modules.addtoken.AddBep2TokenBlockchainService
import io.horizontalsystems.bankwallet.modules.addtoken.AddEvmTokenBlockchainService
import io.horizontalsystems.bankwallet.modules.addtoken.AddEvmTokenBlockchainService.Blockchain.BinanceSmartChain
import io.horizontalsystems.bankwallet.modules.addtoken.AddEvmTokenBlockchainService.Blockchain.Ethereum
import io.horizontalsystems.marketkit.models.CoinType
import io.reactivex.Single
import java.net.UnknownHostException
import java.util.*

class RestoreCustomTokenWorker(ctx: Context, params: WorkerParameters) :
    RxWorker(ctx, params) {

    override fun createWork(): Single<Result> {
        val localStorage = App.localStorage
        val coinManager = App.coinManager
        val walletManager = App.walletManager
        val storage = App.appDatabase
        val networkManager = App.networkManager

        val enabledWallets = storage.walletsDao().enabledCoins()
        val coinTypeIds = enabledWallets.map { it.coinId }
        val platformCoins = coinManager.getPlatformCoinsByCoinTypeIds(coinTypeIds)

        val existingCoinTypeIds = platformCoins.map { it.coinType.id }
        val missingCoinTypeIds = coinTypeIds.filter { !existingCoinTypeIds.contains(it) }

        if (missingCoinTypeIds.isEmpty()) {
            localStorage.customTokensRestoreCompleted = true
            return Single.just(Result.success())
        }

        val coinTypes = missingCoinTypeIds.map { CoinType.fromId(it) }

        return joinedCustomTokensSingle(coinTypes, networkManager)
            .map { customTokens ->
                coinManager.save(customTokens)
                walletManager.loadWallets()
                localStorage.customTokensRestoreCompleted = true
                Result.success()
            }
    }

    private fun customTokenSingle(coinType: CoinType, networkManager: INetworkManager) =
        when (coinType) {
            is CoinType.Erc20 -> {
                val service = AddEvmTokenBlockchainService(Ethereum, networkManager)
                service.customTokenAsync(coinType.address)
            }
            is CoinType.Bep20 -> {
                val service = AddEvmTokenBlockchainService(BinanceSmartChain, networkManager)
                service.customTokenAsync(coinType.address)
            }
            is CoinType.Bep2 -> {
                val service = AddBep2TokenBlockchainService(networkManager)
                service.customTokenAsync(coinType.symbol)
            }
            else -> null
        }

    private fun joinedCustomTokensSingle(
        coinTypes: List<CoinType>,
        networkManager: INetworkManager
    ): Single<List<CustomToken>> {
        val singles: List<Single<Optional<CustomToken>>> = coinTypes.mapNotNull { coinType ->
            customTokenSingle(coinType, networkManager)
                ?.map { Optional.of(it) }
                ?.onErrorReturn {
                    //throw error when there is no connection
                    if (it is UnknownHostException){
                        throw it
                    } else {
                        Optional.empty<CustomToken>()
                    }
                }
        }

        return Single.zip(singles) { array ->
            val customTokens = array.map { it as? Optional<CustomToken> }
            customTokens.mapNotNull { it?.orElse(null) }
        }
    }

}
