package io.horizontalsystems.bankwallet.modules.fulltransactioninfo.providers

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName
import io.horizontalsystems.bankwallet.core.utils.EthInputParser
import io.horizontalsystems.bankwallet.modules.fulltransactioninfo.EthereumResponse
import io.horizontalsystems.bankwallet.modules.fulltransactioninfo.FullTransactionInfoModule
import java.math.BigInteger
import java.util.*

class EtherscanEthereumProvider : FullTransactionInfoModule.EthereumForksProvider {
    override val name: String = "Etherscan.io"

    override fun url(hash: String): String {
        return "https://etherscan.io/tx/$hash"
    }

    override fun apiUrl(hash: String): String {
        return "https://api.etherscan.io/api?module=proxy&action=eth_getTransactionByHash&txhash=$hash"
    }

    override fun convert(json: JsonObject): EthereumResponse {
        return Gson().fromJson(json["result"], EtherscanResponse::class.java)
    }
}

class EtherscanResponse(
        @SerializedName("hash") override val hash: String,
        @SerializedName("from") override val from: String,
        @SerializedName("to") val receiver: String,
        @SerializedName("nonce") val gNonce: String,
        @SerializedName("value") val amount: String,
        @SerializedName("input") val input: String,
        @SerializedName("gas") val gLimit: String,
        @SerializedName("gasPrice") val price: String,
        @SerializedName("blockNumber") val blockNumber: String) : EthereumResponse() {

    override val date: Date? get() = null
    override val confirmations: Int? get() = null
    override val gasUsed: String? get() = null
    override val fee: String? = null
    override val size: Int? get() = null
    override val contractAddress: String?
        get() {
            return if (input != "0x") {
                receiver
            } else {
                null
            }
        }

    override val to: String
        get() {
            if (input != "0x") {
                EthInputParser.parse(input)?.let {
                    return "0x${it.to}"
                }
            }

            return receiver
        }

    override val height: String
        get() = Integer.parseInt(blockNumber.substring(2), 16).toString()

    override val value: BigInteger
        get() {
            var amountData = amount.substring(2)
            if (input != "0x") {
                EthInputParser.parse(input)?.let {
                    amountData = it.value
                }
            }

            return BigInteger(amountData, 16)
        }

    override val nonce: String
        get() = Integer.parseInt(gNonce.substring(2), 16).toString()

    override val gasLimit: String
        get() = BigInteger(gLimit.substring(2), 16).toString()

    override val gasPrice: String
        get() = (BigInteger(price.substring(2), 16).toDouble() / gweiRate).toInt().toString()

}
