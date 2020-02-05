package io.horizontalsystems.bankwallet.modules.fulltransactioninfo.providers

import com.google.gson.JsonObject
import java.text.SimpleDateFormat
import java.util.*

class EosProviderResponse(json: JsonObject, eosAccount: String) : EosResponse() {

    override val txId: String

    override val status: String

    override val blockNumber: String

    override val blockTimeStamp: Long?

    override val actions: List<EosAction>

    override val cpuUsage: Int

    override val netUsage: Int

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    init {
        txId = json["id"].asString
        blockNumber = json["block_num"].asLong.toString()
        blockTimeStamp = dateFormat.parse(json["block_time"].asString)?.time

        val txReceipt = json["trx"].asJsonObject["receipt"].asJsonObject
        status = txReceipt["status"].asString
        cpuUsage = txReceipt["cpu_usage_us"].asInt
        netUsage = txReceipt["net_usage_words"].asInt

        val actions = mutableListOf<EosAction>()

        json["traces"].asJsonArray.forEach {
            val trace = it.asJsonObject
            val action = trace["act"].asJsonObject
            val receipt = trace["receipt"].asJsonObject
            val myTrace = action["name"].asString == "transfer" && receipt["receiver"].asString == eosAccount

            if (myTrace) {
                val account = action["account"].asString

                val actionData = action["data"].asJsonObject

                val amount = actionData["quantity"].asString
                val from = actionData["from"].asString
                val to = actionData["to"].asString
                val memo = actionData["memo"].asString

                actions.add(EosAction(account, from, to, amount, memo))
            }
        }

        this.actions = actions.toList()
    }

}
