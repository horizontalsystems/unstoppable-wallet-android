package io.horizontalsystems.bankwallet.modules.address

import com.unstoppabledomains.resolution.Resolution
import io.horizontalsystems.bankwallet.entities.Address
import io.horizontalsystems.ethereumkit.core.AddressValidator

interface IAddressHandler {
    fun isSupported(value: String): Boolean
    fun parseAddress(value: String): Address
}

class AddressHandlerUdn(val coinCode: String) : IAddressHandler {
    private val resolution = Resolution()

    override fun isSupported(value: String) = resolution.isSupported(value)

    override fun parseAddress(value: String): Address {
        val addressString = resolution.getAddress(value, coinCode)
        val addressDomain = value

        return Address(addressString, addressDomain)
    }

}

class AddressHandlerEvm : IAddressHandler {
    override fun isSupported(value: String) = try {
        AddressValidator.validate(value)
        true
    } catch (e: AddressValidator.AddressValidationException) {
        false
    }

    override fun parseAddress(value: String): Address {
        val evmAddress = io.horizontalsystems.ethereumkit.models.Address(value)
        return Address(evmAddress.hex)
    }

}