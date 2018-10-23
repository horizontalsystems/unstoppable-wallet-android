package bitcoin.wallet.core

import io.horizontalsystems.bitcoinkit.network.MainNet
import io.reactivex.subjects.PublishSubject

object AdapterManager {
    var adapters: MutableList<IAdapter> = mutableListOf()
    var subject: PublishSubject<Any> = PublishSubject.create<Any>()

    fun initAdapters(words: List<String>) {
        clear()
        adapters.clear()
        adapters.add(BitcoinAdapter(words, network = MainNet()))
//        adapters.add(BitcoinAdapter(words, network = TestNet()))

        start()
    }

    fun start() {
        for (adapter in adapters) {
            try {
                adapter.start()
            } catch (error: Exception) {
                print("Could not start ${adapter.coin.name}: $error")
            }
        }
    }

    fun clear() {
        for (adapter in adapters) {
            try {
                adapter.clear()
            } catch (error: Exception) {
                print("Could not clear ${adapter.coin.name}: $error")
            }
        }
        adapters.clear()
    }
}
