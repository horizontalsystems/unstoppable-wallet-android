package io.horizontalsystems.walletkit.chain.litecoinmweb

import io.horizontalsystems.bitcoincore.utils.AddressConverterChain
import io.horizontalsystems.bitcoincore.utils.Base58AddressConverter
import io.horizontalsystems.bitcoincore.utils.SegwitAddressConverter
import io.horizontalsystems.litecoinkit.MainNetLitecoin
import io.horizontalsystems.litecoinmwebkit.MwebAddress
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.walletkit.R
import io.horizontalsystems.walletkit.core.IAdapterManager
import io.horizontalsystems.walletkit.core.ISendMwebAdapter
import io.horizontalsystems.walletkit.core.providers.Translator
import io.horizontalsystems.walletkit.entities.Address
import io.horizontalsystems.walletkit.modules.send.address.AddressValidationError
import io.horizontalsystems.walletkit.modules.send.address.EnterAddressValidator
import kotlinx.coroutines.CancellationException

/**
 * A Litecoin MWEB wallet sends to MWEB addresses and, as a peg-out, to any Litecoin address.
 */
class LitecoinMwebAddressValidator(
    private val token: Token,
    private val adapterManager: IAdapterManager,
) : EnterAddressValidator {

    override suspend fun validate(address: Address) {
        try {
            val adapter = adapterManager.getAdapterForToken<ISendMwebAdapter>(token)
            if (adapter != null) {
                adapter.validate(address.hex)
            } else if (!MwebAddress.isValid(address.hex, MwebAddress.MAINNET_HRP)) {
                litecoinConverter.convert(address.hex)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            throw AddressValidationError.InvalidAddress(Translator.getString(R.string.Send_Address_Error_InvalidAddress))
        }
    }

    companion object {
        // The same converters LitecoinKit builds for mainnet, usable before a wallet exists.
        private val litecoinConverter by lazy {
            val network = MainNetLitecoin()
            AddressConverterChain().apply {
                prependConverter(SegwitAddressConverter(network.addressSegwitHrp))
                prependConverter(Base58AddressConverter(network.addressVersion, network.addressScriptVersion))
            }
        }
    }
}
