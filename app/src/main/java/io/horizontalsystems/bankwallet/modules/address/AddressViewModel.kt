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

    suspend fun parseAddress(v1: String): DataState<Address?> = withContext(Dispatchers.IO) {
        val v = v1.trim()
        if (v.length < 40 && !v.contains(".")) {
            DataState.Success(null)
        } else {
            try {
                var addressString: String = v
                var addressDomain: String? = null

                if (v.contains(".")) {
                    if (resolution.isSupported(v)) {
                        addressString = resolution.getAddress(v, coinCode)
                        addressDomain = v
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