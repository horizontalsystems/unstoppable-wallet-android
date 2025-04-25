package cash.p.terminal.modules.address

import cash.p.terminal.core.App
import cash.p.terminal.entities.Address
import cash.p.terminal.wallet.Wallet
import cash.p.terminal.wallet.entities.TokenQuery
import io.horizontalsystems.core.entities.BlockchainType

class AddressParserChain(
    handlers: List<IAddressHandler> = emptyList(),
    domainHandlers: List<IAddressHandler> = emptyList()
) {

    companion object {
        fun build(wallet: Wallet): AddressParserChain = build(
            tokenQuery = TokenQuery(
                blockchainType = wallet.token.blockchainType,
                tokenType = wallet.token.type
            ),
            coinCode = wallet.coin.code
        )

        fun build(tokenQuery: TokenQuery, coinCode: String?): AddressParserChain {
            val ensHandler =
                AddressHandlerEns(tokenQuery.blockchainType, EnsResolverHolder.resolver)
            val udnHandler =
                AddressHandlerUdn(tokenQuery, coinCode, App.appConfigProvider.udnApiKey)
            val addressParserChain =
                AddressParserChain(domainHandlers = listOf(ensHandler, udnHandler))

            when (tokenQuery.blockchainType) {
                BlockchainType.Bitcoin,
                BlockchainType.BitcoinCash,
                BlockchainType.ECash,
                BlockchainType.Litecoin,
                BlockchainType.Dogecoin,
                BlockchainType.Cosanta,
                BlockchainType.Dash,
                BlockchainType.Zcash,
                BlockchainType.BinanceChain -> {
                    addressParserChain.addHandler(AddressHandlerPure(tokenQuery.blockchainType))
                }

                BlockchainType.Ethereum,
                BlockchainType.BinanceSmartChain,
                BlockchainType.Polygon,
                BlockchainType.Avalanche,
                BlockchainType.Optimism,
                BlockchainType.Base,
                BlockchainType.ZkSync,
                BlockchainType.Gnosis,
                BlockchainType.Fantom,
                BlockchainType.ArbitrumOne -> {
                    addressParserChain.addHandler(AddressHandlerEvm(tokenQuery.blockchainType))
                }

                BlockchainType.Solana -> {
                    addressParserChain.addHandler(AddressHandlerSolana())
                }

                BlockchainType.Tron -> {
                    addressParserChain.addHandler(AddressHandlerTron())
                }

                BlockchainType.Ton -> {
                    addressParserChain.addHandler(AddressHandlerTon())
                }

                is BlockchainType.Unsupported -> Unit
            }

            return addressParserChain
        }
    }

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

    fun supportedHandler(address: String): IAddressHandler? {
        return (addressHandlers + domainHandlers).firstOrNull {
            try {
                it.isSupported(address)
            } catch (t: Throwable) {
                false
            }
        }
    }

    fun addHandler(handler: IAddressHandler) {
        addressHandlers.add(handler)
    }

    fun getAddressFromDomain(address: String): Address? {
        return domainHandlers.firstOrNull { it.isSupported(address) }?.parseAddress(address)
    }

}