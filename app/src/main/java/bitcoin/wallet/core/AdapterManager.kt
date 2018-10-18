package bitcoin.wallet.core

import io.horizontalsystems.bitcoinkit.network.MainNet
import io.horizontalsystems.ethereumkit.network.NetworkType
import io.reactivex.subjects.PublishSubject

class AdapterManager(private val wordsManager: IWordsManager): IAdapterManager {

    override var adapters: MutableList<IAdapter> = mutableListOf()
    override var subject: PublishSubject<Any> = PublishSubject.create<Any>()


    override fun start() {
        wordsManager.words?.let { words ->
            adapters.add(BitcoinAdapter(words, network = MainNet()))
            adapters.add(EthereumAdapter(listOf("subway", "plate", "brick", "pattern", "inform", "used", "oblige", "identify", "cherry", "drop", "flush", "balance"), network = NetworkType.Kovan))

            for (adapter in adapters) {
                try {
                    adapter.start()
                } catch (error: Exception) {
                    print("Could not start ${adapter.coin.name}: $error")
                }
            }
        }
    }


    override fun refresh() {
        adapters.forEach { adapter ->
            adapter.refresh()
        }
    }

    override fun clear() {
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
