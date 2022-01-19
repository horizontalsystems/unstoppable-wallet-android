package io.horizontalsystems.bankwallet.modules.address

import androidx.lifecycle.ViewModel
import com.unstoppabledomains.exceptions.ns.NamingServiceException
import com.unstoppabledomains.resolution.Resolution
import io.horizontalsystems.bankwallet.entities.Address
import io.horizontalsystems.bankwallet.entities.DataState
import io.horizontalsystems.ethereumkit.core.AddressValidator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AddressViewModel(
    private val resolution: Resolution,
    private val coinCode: String
) : ViewModel() {

    suspend fun parseAddress(value: String): DataState<Address?> = withContext(Dispatchers.IO) {
        val vTrimmed = value.trim()
        if (vTrimmed.length < 40 && !vTrimmed.contains(".")) {
            DataState.Success(null)
        } else {
            try {
                var addressString: String = vTrimmed
                var addressDomain: String? = null

                if (vTrimmed.contains(".")) {
                    if (resolution.isSupported(vTrimmed)) {
                        addressString = resolution.getAddress(vTrimmed, coinCode)
                        addressDomain = vTrimmed
                    }
                }

                AddressValidator.validate(addressString)
                DataState.Success(Address(addressString, addressDomain))
            } catch (e: NamingServiceException) {
                DataState.Error(e)
            } catch (e: AddressValidator.AddressValidationException) {
                DataState.Error(e)
            }
        }
    }
}