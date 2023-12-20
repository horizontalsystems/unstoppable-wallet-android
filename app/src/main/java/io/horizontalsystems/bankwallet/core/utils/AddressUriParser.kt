package io.horizontalsystems.bankwallet.core.utils

import android.net.Uri
import io.horizontalsystems.bankwallet.core.IAddressParser
import io.horizontalsystems.bankwallet.core.factories.removeScheme
import io.horizontalsystems.bankwallet.core.factories.uriScheme
import io.horizontalsystems.bankwallet.core.supported
import io.horizontalsystems.bankwallet.entities.AddressUri
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
        val uri = URI(addressUri)
        val schemeSpecificPart = uri.schemeSpecificPart

        val pathEndIndex = schemeSpecificPart.indexOf('?').let { if (it != -1) it else schemeSpecificPart.length }
        val path = schemeSpecificPart.substring(0, pathEndIndex)

        val scheme = uri.scheme ?: return AddressUriResult.NoUri

        blockchainType?.uriScheme?.let { blockchainTypeScheme ->
            if (scheme != blockchainTypeScheme) {
                return AddressUriResult.InvalidBlockchainType
            }
        }

        val parsedUri = AddressUri(scheme = scheme)

        val queryStartIndex = schemeSpecificPart.indexOf('?').let { if (it != -1) it + 1 else schemeSpecificPart.length }
        val query = schemeSpecificPart.substring(queryStartIndex)

        val parameters = parseQueryParameters(query)
        if (parameters.isEmpty()) {
            parsedUri.address = fullAddress(scheme, path)
            return AddressUriResult.Uri(parsedUri)
        }

        for (parameter in parameters) {
            val (key, value) = parameter
            AddressUri.Field.values().firstOrNull { it.value == key }?.let { field ->
                parsedUri.parameters[field] = value
            }
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

        parsedUri.address = fullAddress(scheme, path, parsedUri.value(AddressUri.Field.BlockchainUid))
        return AddressUriResult.Uri(parsedUri)
    }

    private fun parseQueryParameters(query: String?): Map<String, String> {
        val parameters = mutableMapOf<String, String>()

        if (!query.isNullOrBlank()) {
            val keyValuePairs = query.split("&")
            for (pair in keyValuePairs) {
                val (key, value) = pair.split("=")
                parameters[key] = value
            }
        }

        return parameters
    }

    fun uri(addressUri: AddressUri): String {
        val uriBuilder = Uri.Builder()
            .scheme(blockchainType?.uriScheme)
            .path(addressUri.address.removePrefix(blockchainType?.uriScheme ?: "").removePrefix(":"))

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
        fun hasUriPrefix(text: String): Boolean {
            return text.split(":").size > 1
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
