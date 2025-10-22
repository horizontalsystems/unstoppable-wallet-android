package io.horizontalsystems.bankwallet.modules.send.address

import io.horizontalsystems.bankwallet.core.adapters.OxyraConfig
import io.horizontalsystems.bankwallet.entities.Address
import io.horizontalsystems.monerokit.MoneroKit
import android.util.Log

/**
 * OxyraAddressValidator - Validates Oxyra addresses
 * Since Oxyra is a fork of Monero, we can reuse Monero's address validation logic
 */
class OxyraAddressValidator : EnterAddressValidator {
    
    companion object {
        private const val TAG = "OXYRA_INTEGRATION"
    }

    override suspend fun validate(address: Address) {
        try {
            Log.d(TAG, "🔍 Validating Oxyra address: ${address.hex}")
            
            // For now, use MoneroKit validation as Oxyra is Monero fork
            // TODO: Implement Oxyra-specific address validation with OxyraConfig prefixes
            MoneroKit.validateAddress(address.hex)
            
            Log.i(TAG, "✅ Oxyra address validation successful")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Oxyra address validation failed", e)
            // Throw a generic exception for invalid address
            throw Exception("Invalid Oxyra address format")
        }
    }
}