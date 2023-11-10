package io.horizontalsystems.bankwallet.modules.address

import io.horizontalsystems.bankwallet.entities.Address

class AddressParserChain {
    val handlers = mutableListOf<IAddressHandler>()

    fun addHandlers(handlers: List<IAddressHandler>) {
        this.handlers.addAll(handlers)
    }

    fun handlers(address: String): List<IAddressHandler> {
        return handlers.filter {
            try {
                it.isSupported(address)
            } catch (t: Throwable) {
                false
            }
        }
    }

    fun handle(address: String): Address? {
        val handler = handlers(address).firstOrNull()
        return handler?.parseAddress(address)
    }
}