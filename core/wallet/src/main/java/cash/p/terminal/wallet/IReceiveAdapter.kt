package cash.p.terminal.wallet

import cash.p.terminal.wallet.entities.UsedAddress


interface IReceiveAdapter {
    val receiveAddress: String
    val isMainNet: Boolean

    suspend fun isAddressActive(address: String): Boolean {
        return true
    }

    fun usedAddresses(change: Boolean): List<UsedAddress> {
        return listOf()
    }
}