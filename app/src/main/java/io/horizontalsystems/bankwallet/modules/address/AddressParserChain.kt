package io.horizontalsystems.bankwallet.modules.address

import io.horizontalsystems.bankwallet.entities.Address

class AddressParserChain(
    handlers: List<IAddressHandler> = emptyList(),
    domainHandlers: List<IAddressHandler> = emptyList()
) {

    private val domainHandlers = domainHandlers.toMutableList()
    private val addressHandlers = handlers.toMutableList()

    fun supportedAddressHandlers(address: String): List<IAddressHandler> {
        return addressHandlers.filter {
            try {
                it.isSupported(address)
            } catch (t: Throwable) {
                false
            }
        }
    }

    fun handle(address: String): Address? {
        return supportedAddressHandlers(address).firstOrNull()?.parseAddress(address  )
    }

    fun getAddressFromDomain(address: String): Address? {
        return domainHandlers.firstOrNull { it.isSupported(address) }?.parseAddress(address)
    }

}