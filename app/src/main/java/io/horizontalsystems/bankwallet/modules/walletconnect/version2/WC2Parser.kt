package io.horizontalsystems.bankwallet.modules.walletconnect.version2

import io.horizontalsystems.bankwallet.modules.walletconnect.list.WalletConnectListModule

object WC2Parser {

    fun getChainId(chain: String): Int? {
        val splitted = chain.split(":")
        if (splitted.size >= 2) {
            return splitted[1].toIntOrNull()
        }
        return null
    }

    fun getChainName(chain: String): String? {
        val chainId = getChainId(chain) ?: return null
        return WalletConnectListModule.Chain.values().firstOrNull { it.value == chainId }?.title
    }
}
