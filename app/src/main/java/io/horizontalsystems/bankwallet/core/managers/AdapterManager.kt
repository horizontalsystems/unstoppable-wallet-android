package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.*
import io.horizontalsystems.bitcoinkit.BitcoinKit
import io.horizontalsystems.ethereumkit.EthereumKit
import io.reactivex.subjects.PublishSubject

class AdapterManager(private val wordsManager: IWordsManager) : IAdapterManager {

    override var adapters: MutableList<IAdapter> = mutableListOf()
    override var subject: PublishSubject<Boolean> = PublishSubject.create<Boolean>()


    override fun start() {
        wordsManager.words?.let { words ->
            adapters.add(BitcoinAdapter(words, BitcoinKit.NetworkType.MainNet))
            adapters.add(BitcoinAdapter(words, BitcoinKit.NetworkType.MainNetBitCash))
            adapters.add(EthereumAdapter(words, EthereumKit.NetworkType.MainNet))

            for (adapter in adapters) {
                try {
                    adapter.start()
                } catch (error: Exception) {
                    print("Could not start ${adapter.coin.name}: $error")
                }
            }
            subject.onNext(true)
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
