package bitcoin.wallet.core

import bitcoin.wallet.kit.network.MainNet

object AdapterManager {
    var adapters: MutableList<IAdapter> = mutableListOf()

    fun initAdapters(words: List<String>) {
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
    }
}
