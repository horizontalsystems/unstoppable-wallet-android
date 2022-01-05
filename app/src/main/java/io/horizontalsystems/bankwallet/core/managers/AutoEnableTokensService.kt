package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.modules.enablecoins.EnableCoinsEip20Provider
import io.reactivex.disposables.CompositeDisposable

class AutoEnableTokensService(
    private val ethereumKitManager: EvmKitManager,
    private val binanceSmartChainKitManager: EvmKitManager,
    private val walletActivator: WalletActivator,
) {
    private val enableErc20Provider = EnableCoinsEip20Provider(
        App.networkManager,
        EnableCoinsEip20Provider.EnableCoinMode.Erc20
    )

    private val enableBep20Provider = EnableCoinsEip20Provider(
        App.networkManager,
        EnableCoinsEip20Provider.EnableCoinMode.Bep20
    )

    private val disposables = CompositeDisposable()

    fun start() {
        subscribeForKitStartedStatus(ethereumKitManager, enableErc20Provider)
        subscribeForKitStartedStatus(binanceSmartChainKitManager, enableBep20Provider)
    }

    private fun subscribeForKitStartedStatus(
        kitManager: EvmKitManager,
        enableCoinsEip20Provider: EnableCoinsEip20Provider,
    ) {
        kitManager.kitStartedObservable
            .subscribeIO { started ->
                if (started) {
                    enableTokensWithTx(kitManager, enableCoinsEip20Provider)
                }
            }
            .let {
                disposables.add(it)
            }
    }

    private fun enableTokensWithTx(
        kitManager: EvmKitManager,
        enableCoinsEip20Provider: EnableCoinsEip20Provider,
    ) {
        val account = kitManager.currentAccount ?: return
        val address = kitManager.evmKitWrapper?.evmKit?.receiveAddress?.hex ?: return
        enableCoinsEip20Provider.getCoinTypesAsync(address)
            .subscribeIO { coinTypes ->
                val notEnabled = coinTypes.filter { !walletActivator.isEnabled(account, it) }
                if (notEnabled.isNotEmpty()) {
                    walletActivator.activateWallets(account, notEnabled)
                }
            }
            .let {
                disposables.add(it)
            }
    }
}
