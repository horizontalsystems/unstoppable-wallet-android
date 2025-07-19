package cash.p.terminal.modules.send.address

import cash.p.terminal.R
import cash.p.terminal.core.App
import cash.p.terminal.core.ISendBitcoinAdapter
import cash.p.terminal.core.ISendStellarAdapter
import cash.p.terminal.core.ISendTronAdapter
import cash.p.terminal.core.ISendZcashAdapter
import cash.p.terminal.core.adapters.zcash.ZcashAdapter
import cash.p.terminal.entities.Address
import cash.p.terminal.strings.helpers.Translator
import cash.p.terminal.wallet.IAdapterManager
import cash.p.terminal.wallet.Token
import cash.p.terminal.wallet.entities.TokenType
import com.m2049r.xmrwallet.model.Wallet
import io.horizontalsystems.ethereumkit.core.AddressValidator
import io.horizontalsystems.tonkit.FriendlyAddress

interface EnterAddressValidator {
    @Throws
    suspend fun validate(address: Address)
}


class BitcoinAddressValidator(
    private val token: Token,
    private val adapterManager: IAdapterManager
) : EnterAddressValidator {
    private val sendAdapter by lazy { adapterManager.getAdapterForToken<ISendBitcoinAdapter>(token) }

    override suspend fun validate(address: Address) {
        val adapter = sendAdapter ?: throw AddressValidationError.NoAdapter()

        adapter.validate(address.hex, null)
    }
}

class EvmAddressValidator :
    EnterAddressValidator {
    override suspend fun validate(address: Address) {
        AddressValidator.validate(address.hex)
        io.horizontalsystems.ethereumkit.models.Address(address.hex)
    }
}

class SolanaAddressValidator :
    EnterAddressValidator {
    override suspend fun validate(address: Address) {
        io.horizontalsystems.solanakit.models.Address(address.hex)
    }
}

class TonAddressValidator :
    EnterAddressValidator {
    override suspend fun validate(address: Address) {
        FriendlyAddress.parse(address.hex)
    }
}

class StellarAddressValidator(private val token: Token) :
    EnterAddressValidator {
    private val sendAdapter by lazy {
        App.adapterManager.getAdapterForToken<ISendStellarAdapter>(
            token
        )
    }

    override suspend fun validate(address: Address) {
        val adapter = sendAdapter ?: throw AddressValidationError.NoAdapter()
        adapter.validate(address.hex)
    }
}

class MoneroAddressValidator: EnterAddressValidator {
    override suspend fun validate(address: Address) {
        Wallet.isAddressValid(address.hex)
    }
}

class TronAddressValidator(
    private val token: Token,
    private val adapterManager: IAdapterManager
) : EnterAddressValidator {
    private val sendAdapter by lazy { adapterManager.getAdapterForToken<ISendTronAdapter>(token) }
    override suspend fun validate(address: Address) {
        val adapter = sendAdapter ?: throw AddressValidationError.NoAdapter()

        val validAddress = io.horizontalsystems.tronkit.models.Address.fromBase58(address.hex)
        if (token.type == TokenType.Native && adapter.isOwnAddress(validAddress)) {
            throw AddressValidationError.SendToSelfForbidden(
                Translator.getString(R.string.Send_Error_SendToSelf, "TRX")
            )
        }
    }
}

class ZcashAddressValidator(
    private val token: Token,
    private val adapterManager: IAdapterManager,
) : EnterAddressValidator {
    private val sendAdapter by lazy { adapterManager.getAdapterForToken<ISendZcashAdapter>(token) }

    override suspend fun validate(address: Address) {
        val adapter = sendAdapter ?: throw AddressValidationError.NoAdapter()

        try {
            adapter.validate(address.hex)
        } catch (e: ZcashAdapter.ZcashError.SendToSelfNotAllowed) {
            throw AddressValidationError.SendToSelfForbidden(
                Translator.getString(R.string.Send_Error_SendToSelf, "ZEC")
            )
        }
    }
}

sealed class AddressValidationError : Throwable() {
    class NoAdapter : AddressValidationError() {
        override val message = "Send adapter is not found"
    }

    class SendToSelfForbidden(override val message: String) : AddressValidationError()
}