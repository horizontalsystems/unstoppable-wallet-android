package io.horizontalsystems.bankwallet.modules.fulltransactioninfo.providers

import com.google.gson.JsonObject
import java.text.SimpleDateFormat
import java.util.*

class EosProviderResponse(json: JsonObject) : EosResponse() {

    override val txId: String

    override val status: String

    override val blockNumber: String

    override val blockTimeStamp: Long

    override val amount: String

    override val from: String

    override val to: String

    override val memo: String

    override val account: String

    override val cpuUsage: Int

    override val netUsage: Int

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    init {
        txId = json["id"].asString
        blockNumber = json["block_num"].asLong.toString()
        blockTimeStamp = dateFormat.parse(json["block_time"].asString).time

        val txReceipt = json["trx"].asJsonObject["receipt"].asJsonObject
        status = txReceipt["status"].asString
        cpuUsage = txReceipt["cpu_usage_us"].asInt
        netUsage = txReceipt["net_usage_words"].asInt

        val trace = json["traces"].asJsonArray.first {
            val trace = it.asJsonObject
            val action = trace["act"].asJsonObject
            action["name"].asString == "transfer"
        }.asJsonObject

        val action = trace["act"].asJsonObject
        account = action["account"].asString

        val actionData = action["data"].asJsonObject
        amount = actionData["quantity"].asString
        from = actionData["from"].asString
        to = actionData["to"].asString
        memo = actionData["memo"].asString
    }

}
