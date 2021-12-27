package io.horizontalsystems.bankwallet.modules.swap.settings

import io.horizontalsystems.bankwallet.core.Clearable
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.entities.Address
import io.horizontalsystems.marketkit.models.CoinType
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import java.util.*

class AddressResolutionService(val coinCode: String, val isResolutionEnabled: Boolean = true) : Clearable {

    val isResolvingAsync = BehaviorSubject.createDefault(false)
    val resolveFinishedAsync = BehaviorSubject.createDefault<Optional<Address>>(Optional.empty())

    private val provider = AddressResolutionProvider()
    private val disposables = CompositeDisposable()

    var isResolving: Boolean = false
        private set(value) {
            if (field != value) {
                isResolvingAsync.onNext(value)
            }
            field = value
        }

    fun setText(text: String?) {
        if (!isResolutionEnabled) {
            return
        }

        disposables.clear()

        if (text == null || !text.contains(".")) {
            isResolving = false
            return
        }

        isResolving = true

        provider.isValidAsync(text)
            .subscribeIO {
                resolve(it, text)
            }
            .let {
                disposables.add(it)
            }

    }

    private fun resolve(valid: Boolean, domain: String) {
        if (!valid) {
            isResolving = false
            return
        }

        provider.resolveAsync(domain = domain, ticker = coinCode)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ addressResponse ->
                isResolving = false
                resolveFinishedAsync.onNext(Optional.of(Address(hex = addressResponse, domain = domain)))
            }, {
                isResolving = false
                resolveFinishedAsync.onNext(Optional.empty())
            })
            .let {
                disposables.add(it)
            }
    }

    override fun clear() {
        disposables.clear()
    }

    companion object {
        fun getChainCoinCode(coinType: CoinType): String? = when (coinType) {
            CoinType.Ethereum -> "ETH"
            is CoinType.Erc20 -> "ETH"
            CoinType.Bitcoin -> "BTC"
            else -> null
        }
    }
}