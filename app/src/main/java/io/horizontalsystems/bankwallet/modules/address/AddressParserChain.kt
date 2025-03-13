package io.horizontalsystems.bankwallet.modules.address

import io.horizontalsystems.bankwallet.entities.Address

class AddressParserChain(
    handlers: List<IAddressHandler> = emptyList(),
    domainHandlers: List<IAddressHandler> = emptyList(),
) {
    private val domainHandlers = domainHandlers.toMutableList()
    private val addressHandlers = handlers.toMutableList()

    fun supportedAddressHandlers(address: String): List<IAddressHandler> =
        addressHandlers.filter {
            try {
                it.isSupported(address)
            } catch (t: Throwable) {
                false
            }
        }

    fun supportedHandler(address: String): IAddressHandler? =
        (addressHandlers + domainHandlers).firstOrNull {
            try {
                it.isSupported(address)
            } catch (t: Throwable) {
                false
            }
        }

    fun addHandler(handler: IAddressHandler) {
        addressHandlers.add(handler)
    }

    fun getAddressFromDomain(address: String): Address? = domainHandlers.firstOrNull { it.isSupported(address) }?.parseAddress(address)
}
