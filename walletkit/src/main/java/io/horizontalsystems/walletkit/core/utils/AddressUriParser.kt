package io.horizontalsystems.walletkit.core.utils

import android.net.Uri
import io.horizontalsystems.walletkit.core.IAddressParser
import io.horizontalsystems.walletkit.core.blockchainTypeFromChainId
import io.horizontalsystems.walletkit.core.factories.removeScheme
import io.horizontalsystems.walletkit.core.factories.uriScheme
import io.horizontalsystems.walletkit.core.supported
import io.horizontalsystems.walletkit.entities.AddressUri
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.TokenType
import java.net.URI


class AddressUriParser(private val blockchainType: BlockchainType?, private val tokenType: TokenType?) : IAddressParser {
    private fun pair(type: BlockchainType, s2: String?): String {
        val prefix = if (type.removeScheme) null else type.uriScheme
        return listOfNotNull(prefix, s2).joinToString(separator = ":")
    }

    private fun fullAddress(scheme: String, address: String, uriBlockchainUid: String? = null): String {
        // there is no explicit indication of the blockchain in the uri. We use the rules of the blockchain parser
        uriBlockchainUid ?: run {
            // if has blockchainType check if needed prefix
            blockchainType?.let {
                return pair(it, address)
            }

            // if there is no any blockchainTypes supported, try to determine
            BlockchainType.supported.firstOrNull { it.uriScheme == scheme }?.let {
                return pair(it, address)
            }

            return address
        }

        // There is a blockchain Uid in the uri. We use it to create an address
        return pair(BlockchainType.fromUid(uriBlockchainUid), address)
    }

    override fun parse(addressUri: String): AddressUriResult {
        val uri = try {
            URI(addressUri)
        } catch (e: Throwable) {
            null
        } ?: return AddressUriResult.WrongUri

        val schemeSpecificPart = uri.schemeSpecificPart

        val pathEndIndex = schemeSpecificPart.indexOf('?').let { if (it != -1) it else schemeSpecificPart.length }
        val path = schemeSpecificPart.take(pathEndIndex)

        val scheme = uri.scheme ?: return AddressUriResult.NoUri

        blockchainType?.uriScheme?.let { blockchainTypeScheme ->
            if (scheme != blockchainTypeScheme) {
                return AddressUriResult.InvalidBlockchainType
            }
        }

        val parsedUri = AddressUri(scheme = scheme)

        // EIP-681 path: [pay-]<address>[@<chainId>][/<function>]
        var rawPath = path
        var function: String? = null

        if (scheme == "ethereum") {
            val stripped = path.removePrefix("pay-")
            val addressEnd = stripped.indexOfFirst { it == '@' || it == '/' }.let { if (it == -1) stripped.length else it }
            rawPath = stripped.take(addressEnd)
            var rest = stripped.substring(addressEnd)

            if (rest.startsWith("@")) {
                val chainEnd = rest.indexOf('/').let { if (it == -1) rest.length else it }
                val chainPart = rest.substring(1, chainEnd)
                rest = rest.substring(chainEnd)
                chainPart.toLongOrNull()?.let { chainId ->
                    // Reject URIs targeting EVM chains we don't support: a silent fall-through
                    // would mis-render as a chain-agnostic native EVM URI in the UI
                    val uid = chainId.blockchainTypeFromChainId?.uid
                        ?: return AddressUriResult.InvalidBlockchainType
                    parsedUri.parameters[AddressUri.Field.BlockchainUid] = uid
                }
            }

            if (rest.startsWith("/")) {
                function = rest.substring(1).ifEmpty { null }
            }
        }

        val queryStartIndex = schemeSpecificPart.indexOf('?').let { if (it != -1) it + 1 else schemeSpecificPart.length }
        val query = schemeSpecificPart.substring(queryStartIndex)

        val parameters = parseQueryParameters(query)

        // amounts stay in the units the uri declared them in (`value` = base units);
        // conversion to a readable amount happens where the token is known
        for (parameter in parameters) {
            val (key, value) = parameter
            AddressUri.Field.entries.firstOrNull { it.value == key }?.let { field ->
                parsedUri.parameters[field] = value
            }
        }

        if (function != null) {
            // Only ERC-20 transfer(address,uint256) is supported. Anything else is rejected —
            // a silent fallback into a "native send to contract address" interpretation would
            // let a crafted URI phish a token transfer disguised as a plain coin transfer
            if (function != "transfer") return AddressUriResult.WrongUri
            if (parameters.containsKey(AddressUri.Field.Value.value)) return AddressUriResult.WrongUri

            val contract = rawPath
            val recipient = parameters["address"] ?: return AddressUriResult.WrongUri
            val rawAmount = parameters["uint256"] ?: return AddressUriResult.WrongUri
            if (!isEvmAddress(contract) || !isEvmAddress(recipient)) return AddressUriResult.WrongUri
            if (rawAmount.toBigIntegerOrNull() == null) return AddressUriResult.WrongUri

            parsedUri.parameters[AddressUri.Field.TokenUid] = "eip20:${contract.lowercase()}"
            parsedUri.parameters[AddressUri.Field.Value] = rawAmount
            rawPath = recipient
        }

        parsedUri.value<String>(AddressUri.Field.BlockchainUid)?.let { uid ->
            if (blockchainType?.uid != null && blockchainType.uid != uid) {
                return AddressUriResult.InvalidBlockchainType
            }
        }

        parsedUri.value<String>(AddressUri.Field.TokenUid)?.let { uid ->
            if (tokenType?.id != null && tokenType.id.lowercase() != uid.lowercase()) {
                return AddressUriResult.InvalidTokenType
            }
        }

        parsedUri.address = fullAddress(scheme, rawPath, parsedUri.value(AddressUri.Field.BlockchainUid))
        return AddressUriResult.Uri(parsedUri)
    }

    private fun isEvmAddress(address: String): Boolean {
        return evmAddressRegex.matches(address)
    }

    private fun parseQueryParameters(query: String?): Map<String, String> {
        val parameters = mutableMapOf<String, String>()

        if (!query.isNullOrBlank()) {
            val keyValuePairs = query.split("&")
            for (pair in keyValuePairs) {
                val parts = pair.split("=")
                if (parts.size == 2) {
                    parameters[parts[0]] = parts[1]
                }
            }
        }

        return parameters
    }

    fun uri(addressUri: AddressUri): String {
        val path = addressUri.address.removePrefix(blockchainType?.uriScheme ?: "").removePrefix(":")
        val uriBuilder = Uri.Builder()
            .scheme(blockchainType?.uriScheme)
            .encodedPath(path)

        for ((key, value) in addressUri.parameters) {
            uriBuilder.appendQueryParameter(key.value, value)
        }

        for ((key, value) in addressUri.unhandledParameters) {
            uriBuilder.appendQueryParameter(key, value)
        }

        return uriBuilder
            .build()
            .toString()
            .replace("/", "")
            .replace("%3A", ":")
    }

    companion object {
        private val evmAddressRegex = Regex("^0x[0-9a-fA-F]{40}$")

        fun hasUriPrefix(text: String): Boolean {
            return text.split(":").size > 1
        }

        fun addressUri(text: String): AddressUri? {
            if (hasUriPrefix(text)) {
                val abstractUriParse = AddressUriParser(null, null)
                return when (val result = abstractUriParse.parse(text)) {
                    is AddressUriResult.Uri -> {
                        if (BlockchainType.supported.map { it.uriScheme }
                                .contains(result.addressUri.scheme))
                            result.addressUri
                        else
                            null
                    }

                    else -> null
                }
            }
            return null
        }
    }
}

sealed class AddressUriResult {
    object WrongUri : AddressUriResult()
    object InvalidBlockchainType : AddressUriResult()
    object InvalidTokenType : AddressUriResult()
    object NoUri : AddressUriResult()
    class Uri(val addressUri: AddressUri) : AddressUriResult()
}
