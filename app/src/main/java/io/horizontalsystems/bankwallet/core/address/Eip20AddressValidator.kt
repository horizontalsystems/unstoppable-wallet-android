package io.horizontalsystems.bankwallet.core.address

import io.horizontalsystems.bankwallet.core.managers.EvmBlockchainManager
import io.horizontalsystems.bankwallet.core.managers.EvmSyncSourceManager
import io.horizontalsystems.bankwallet.entities.Address
import io.horizontalsystems.ethereumkit.contracts.ContractMethod
import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.marketkit.models.TokenType
import kotlinx.coroutines.rx2.await
import io.horizontalsystems.ethereumkit.models.Address as EvmAddress

class Eip20AddressValidator(val evmSyncSourceManager: EvmSyncSourceManager) {
    suspend fun check(address: Address, token: Token): AddressCheckResult {
        val syncSource = evmSyncSourceManager.defaultSyncSources(token.blockchainType).first()
        val contractAddress = (token.type as? TokenType.Eip20)?.address?.let { EvmAddress(it) }
            ?: return AddressCheckResult.NotSupported

        val method = method(address, contractAddress)
            ?: return AddressCheckResult.NotSupported

        val response: ByteArray =
            EthereumKit.call(syncSource.rpcSource, contractAddress, method.encodedABI()).await()

        return if (response.contains(1.toByte()))
            AddressCheckResult.Detected
        else
            AddressCheckResult.Clear
    }

    fun supports(token: Token): Boolean {
        if (!EvmBlockchainManager.blockchainTypes.contains(token.blockchainType)) return false
        val contractAddress = (token.type as? TokenType.Eip20)?.address?.let { EvmAddress(it) }
            ?: return false

        return method(Address(""), contractAddress) != null
    }

    companion object {
        fun method(address: Address, contractAddress: EvmAddress): ContractMethod? {
            val evmAddress = try {
                EvmAddress(address.hex)
            } catch (e: Throwable) {
                return null
            }

            if (IsBlacklistedMethodUSDT.contractAddresses.contains(contractAddress.eip55)) {
                return IsBlacklistedMethodUSDT(evmAddress)
            }

            if (IsBlacklistedMethodUSDC.contractAddresses.contains(contractAddress.eip55)) {
                return IsBlacklistedMethodUSDC(evmAddress)
            }

            if (IsFrozenMethodPYUSD.contractAddresses.contains(contractAddress.eip55)) {
                return IsFrozenMethodPYUSD(evmAddress)
            }

            return null
        }
    }

    class IsBlacklistedMethodUSDT(val address: EvmAddress) : ContractMethod() {
        override val methodSignature: String
            get() = "isBlackListed(address)"

        override fun getArguments(): List<Any> {
            return listOf(address)
        }

        companion object {
            val contractAddresses = listOf("0xdAC17F958D2ee523a2206206994597C13D831ec7")
        }
    }

    class IsBlacklistedMethodUSDC(val address: EvmAddress) : ContractMethod() {
        override val methodSignature: String
            get() = "isBlacklisted(address)"

        override fun getArguments(): List<Any> {
            return listOf(address)
        }

        companion object {
            val contractAddresses = listOf("0xA0b86991c6218b36c1d19D4a2e9Eb0cE3606eB48")
        }
    }

    class IsFrozenMethodPYUSD(val address: EvmAddress) : ContractMethod() {
        override val methodSignature: String
            get() = "isFrozen(address)"

        override fun getArguments(): List<Any> {
            return listOf(address)
        }

        companion object {
            val contractAddresses = listOf("0x6c3ea9036406852006290770BEdFcAbA0e23A0e8")
        }
    }
}
