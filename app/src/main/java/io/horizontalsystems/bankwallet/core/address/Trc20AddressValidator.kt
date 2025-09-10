package io.horizontalsystems.bankwallet.core.address

import io.horizontalsystems.bankwallet.entities.Address
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.marketkit.models.TokenType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class Trc20AddressValidator {

    private val logger = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(logger)
        .build()

    suspend fun isClear(address: Address, token: Token): Boolean {
        val contractAddress = (token.type as? TokenType.Eip20)?.address
            ?: throw TokenError.InvalidTokenType

        return isClear(address, token.coin.uid, contractAddress)
    }

    suspend fun isClear(
        address: Address,
        coinUid: String,
        contractAddress: String
    ): Boolean {
        val tronAddressHex = address.hex

        if (!isValidTronAddress(tronAddressHex)) {
            throw TokenError.InvalidAddress
        }

        if (!isValidTronAddress(contractAddress)) {
            throw TokenError.InvalidContractAddress
        }

        val method = method(coinUid) ?: throw TokenError.NoMethod

        return when (method) {
            is TronMethod.BlacklistedMethodUSDT -> {
                checkBlacklistStatus(tronAddressHex, contractAddress, "isBlackListed")
            }
        }
    }

    private fun isValidTronAddress(tronAddressHex: String): Boolean {
        try {
            tronAddress(tronAddressHex)
        } catch (e: Exception) {
            return false
        }
        return true
    }

    fun supports(token: Token): Boolean {
        return token.blockchainType == BlockchainType.Tron && method(token.coin.uid) != null
    }

    private suspend fun checkBlacklistStatus(
        addressHex: String,
        contractAddress: String,
        methodName: String
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val tronGridUrl = "https://api.trongrid.io/wallet/triggerconstantcontract"

            val requestBody = JSONObject().apply {
                put("owner_address", addressHex)
                put("contract_address", contractAddress)
                put("function_selector", "$methodName(address)")
                put("parameter", encodeAddressForContract(addressHex))
                put("visible", true)
            }

            val request = Request.Builder()
                .url(tronGridUrl)
                .post(requestBody.toString().toRequestBody("application/json".toMediaType()))
                .addHeader("Content-Type", "application/json")
                .build()

            val response = httpClient.newCall(request).execute()
            val responseBody = response.body?.string()

            if (!response.isSuccessful || responseBody == null) {
                throw TokenError.NetworkError
            }

            val jsonResponse = JSONObject(responseBody)

            if (jsonResponse.has("Error")) {
                throw TokenError.ContractError
            }

            val constantResult = jsonResponse.optJSONArray("constant_result")
            if (constantResult != null && constantResult.length() > 0) {
                val result = constantResult.getString(0)
                // If result contains "01" at the end, the address is blacklisted
                return@withContext !result.endsWith("01")
            }

            // If we can't determine the status, assume it's not clear (blacklisted)
            return@withContext false
        } catch (e: Exception) {
            when (e) {
                is TokenError -> throw e
                else -> throw TokenError.NetworkError
            }
        }
    }

    private fun encodeAddressForContract(base58Address: String): String {
        // Convert base58 to hex address for ABI encoding
        // TRON addresses when converted to hex start with 41, we need to remove it and pad to 32 bytes
        val hexAddress = base58ToHex(base58Address)
        val addressWithout41 = hexAddress.removePrefix("41")
        return "000000000000000000000000$addressWithout41"
    }

    private fun base58ToHex(base58: String): String {
        val tronAddress = tronAddress(base58)
        return tronAddress.hex
    }

    private fun tronAddress(address: String): io.horizontalsystems.tronkit.models.Address {
        return io.horizontalsystems.tronkit.models.Address.fromBase58(address)
    }

    companion object {
        fun method(coinUid: String): TronMethod? {
            return when (coinUid) {
                "tether" -> TronMethod.BlacklistedMethodUSDT
                else -> null
            }
        }
    }
}

sealed class TronMethod {
    object BlacklistedMethodUSDT : TronMethod()
}
