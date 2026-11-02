package io.horizontalsystems.walletkit.modules.send.address

import io.horizontalsystems.walletkit.R
import io.horizontalsystems.walletkit.core.App
import io.horizontalsystems.walletkit.core.IAdapterManager
import io.horizontalsystems.walletkit.core.IPegInMwebAdapter
import io.horizontalsystems.walletkit.core.providers.Translator
import io.horizontalsystems.walletkit.modules.address.AddressHandlerMweb
import io.horizontalsystems.marketkit.models.TokenQuery
import io.horizontalsystems.marketkit.models.TokenType
import io.horizontalsystems.walletkit.core.ISendBitcoinAdapter
import io.horizontalsystems.walletkit.entities.Address
import io.horizontalsystems.bitcoincash.MainNetBitcoinCash
import io.horizontalsystems.bitcoincore.utils.AddressConverterChain
import io.horizontalsystems.bitcoincore.utils.Base58AddressConverter
import io.horizontalsystems.bitcoincore.utils.CashAddressConverter
import io.horizontalsystems.bitcoincore.utils.SegwitAddressConverter
import io.horizontalsystems.bitcoinkit.MainNet
import io.horizontalsystems.dashkit.MainNetDash
import io.horizontalsystems.ecash.MainNetECash
import io.horizontalsystems.litecoinkit.MainNetLitecoin
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.Token

class BitcoinAddressValidator(
    private val token: Token,
    private val adapterManager: IAdapterManager
) : EnterAddressValidator {
    private val sendAdapter by lazy { adapterManager.getAdapterForToken<ISendBitcoinAdapter>(token) }

    override suspend fun validate(address: Address) {
        if (token.blockchainType == BlockchainType.Litecoin && AddressHandlerMweb.isMwebAddress(address.hex)) {
            validatePegInDestination()
            return
        }

        val adapter = sendAdapter
        if (adapter != null) {
            adapter.validate(address.hex, null)
        } else {
            // no enabled wallet for this chain (e.g. a swap recipient on an account that
            // can't hold it) — parse with the chain's mainnet address converters instead
            staticAddressConverter(token.blockchainType).convert(address.hex)
        }
    }

    // The peg-in is built and broadcast by the MWEB daemon, which runs only for an enabled
    // Litecoin MWEB wallet of the account.
    private fun validatePegInDestination() {
        val mwebToken = App.coinManager.getToken(TokenQuery(BlockchainType.LitecoinMweb, TokenType.Native))
        val pegInAdapter = mwebToken?.let { adapterManager.getAdapterForToken<IPegInMwebAdapter>(it) }
        if (pegInAdapter == null) {
            throw AddressValidationError.InvalidAddress(Translator.getString(R.string.Send_Error_EnableMwebWallet))
        }
    }

    companion object {
        // Mirrors each kit's private parseAddress(): the same converters BitcoinCore
        // builds for the network, minus the wallet
        private fun staticAddressConverter(blockchainType: BlockchainType) = AddressConverterChain().apply {
            when (blockchainType) {
                BlockchainType.Bitcoin -> {
                    val network = MainNet()
                    prependConverter(SegwitAddressConverter(network.addressSegwitHrp))
                    prependConverter(Base58AddressConverter(network.addressVersion, network.addressScriptVersion))
                }

                BlockchainType.Litecoin -> {
                    val network = MainNetLitecoin()
                    prependConverter(SegwitAddressConverter(network.addressSegwitHrp))
                    prependConverter(Base58AddressConverter(network.addressVersion, network.addressScriptVersion))
                }

                BlockchainType.BitcoinCash -> {
                    val network = MainNetBitcoinCash()
                    prependConverter(CashAddressConverter(network.addressSegwitHrp))
                    prependConverter(Base58AddressConverter(network.addressVersion, network.addressScriptVersion))
                }

                BlockchainType.ECash -> {
                    val network = MainNetECash()
                    prependConverter(CashAddressConverter(network.addressSegwitHrp))
                    prependConverter(Base58AddressConverter(network.addressVersion, network.addressScriptVersion))
                }

                BlockchainType.Dash -> {
                    val network = MainNetDash()
                    prependConverter(Base58AddressConverter(network.addressVersion, network.addressScriptVersion))
                }

                else -> throw AddressValidationError.NoAdapter()
            }
        }
    }
}
