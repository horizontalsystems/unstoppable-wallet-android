package io.horizontalsystems.bankwallet.core.address

import io.horizontalsystems.bankwallet.core.managers.EvmSyncSourceManager
import io.horizontalsystems.bankwallet.entities.Address
import io.horizontalsystems.ethereumkit.contracts.ContractMethod
import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.marketkit.models.TokenType
import kotlinx.coroutines.rx2.await
import io.horizontalsystems.ethereumkit.models.Address as EvmAddress

class Eip20AddressValidator(val evmSyncSourceManager: EvmSyncSourceManager) {

    suspend fun isClear(address: Address, token: Token): Boolean {
        val contractAddress = (token.type as? TokenType.Eip20)?.address
            ?: throw TokenError.InvalidTokenType

        return isClear(address, token.coin.uid, token.blockchainType, contractAddress)
    }

    suspend fun isClear(
        address: Address,
        coinUid: String,
        blockchainType: BlockchainType,
        contractAddress: String
    ): Boolean {
        val evmAddress = try {
            EvmAddress(address.hex)
        } catch (e: Throwable) {
            throw TokenError.InvalidAddress
        }

        val validContractAddress = try {
            EvmAddress(contractAddress)
        } catch (e: Throwable) {
            throw TokenError.InvalidContractAddress
        }

        val syncSource = evmSyncSourceManager.defaultSyncSources(blockchainType).firstOrNull()
            ?: throw TokenError.NoSyncSource

        val method = method(coinUid, blockchainType)
            ?: throw TokenError.NoMethod

        val response: ByteArray =
            EthereumKit.call(
                syncSource.rpcSource,
                validContractAddress,
                method.contractMethod(evmAddress).encodedABI()
            )
                .await()

        return !response.contains(1.toByte())
    }

    fun supports(token: Token): Boolean {
        return method(token.coin.uid, token.blockchainType) != null
    }

    companion object {
        fun method(coinUid: String, blockchainType: BlockchainType): Method? {
            return when (coinUid) {
                "tether" -> {
                    when (blockchainType) {
                        BlockchainType.Ethereum -> Method.BlacklistedMethodUSDT
                        else -> null
                    }
                }

                "usd-coin" -> {
                    when (blockchainType) {
                        BlockchainType.Ethereum,
                        BlockchainType.Optimism,
                        BlockchainType.Avalanche,
                        BlockchainType.ArbitrumOne,
                        BlockchainType.Polygon,
                        BlockchainType.ZkSync,
                        BlockchainType.Base -> Method.BlacklistedMethodUSDC

                        else -> null
                    }
                }

                "paypal-usd" -> {
                    when (blockchainType) {
                        BlockchainType.Ethereum -> Method.FrozenMethodPYUSD
                        else -> null
                    }
                }

                else -> null
            }
        }
    }

    class IsBlacklistedMethodUSDT(val address: EvmAddress) : ContractMethod() {
        override val methodSignature: String
            get() = "isBlackListed(address)"

        override fun getArguments(): List<Any> {
            return listOf(address)
        }
    }

    class IsBlacklistedMethodUSDC(val address: EvmAddress) : ContractMethod() {
        override val methodSignature: String
            get() = "isBlacklisted(address)"

        override fun getArguments(): List<Any> {
            return listOf(address)
        }
    }

    class IsFrozenMethodPYUSD(val address: EvmAddress) : ContractMethod() {
        override val methodSignature: String
            get() = "isFrozen(address)"

        override fun getArguments(): List<Any> {
            return listOf(address)
        }
    }
}

sealed class Method {
    object BlacklistedMethodUSDT : Method()
    object BlacklistedMethodUSDC : Method()
    object FrozenMethodPYUSD : Method()

    fun contractMethod(evmAddress: EvmAddress): ContractMethod {
        return when (this) {
            is BlacklistedMethodUSDT -> Eip20AddressValidator.IsBlacklistedMethodUSDT(evmAddress)
            is BlacklistedMethodUSDC -> Eip20AddressValidator.IsBlacklistedMethodUSDC(evmAddress)
            is FrozenMethodPYUSD -> Eip20AddressValidator.IsFrozenMethodPYUSD(evmAddress)
        }
    }
}

sealed class TokenError : Exception() {
    object InvalidTokenType : TokenError()
    object InvalidAddress : TokenError()
    object InvalidContractAddress : TokenError()
    object NoSyncSource : TokenError()
    object NoMethod : TokenError()
    object NetworkError : TokenError()
    object ContractError : TokenError()
}