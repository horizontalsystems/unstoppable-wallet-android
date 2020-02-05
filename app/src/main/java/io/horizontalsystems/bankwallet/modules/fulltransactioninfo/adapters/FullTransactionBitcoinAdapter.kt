package io.horizontalsystems.bankwallet.modules.fulltransactioninfo.adapters

import com.google.gson.JsonObject
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.entities.*
import io.horizontalsystems.bankwallet.modules.fulltransactioninfo.FullTransactionInfoModule
import io.horizontalsystems.core.helpers.DateHelper

class FullTransactionBitcoinAdapter(val provider: FullTransactionInfoModule.BitcoinForksProvider, val coin: Coin, val unitName: String)
    : FullTransactionInfoModule.Adapter {

    override fun convert(json: JsonObject): FullTransactionRecord {
        val data = provider.convert(json)
        val sections = mutableListOf<FullTransactionSection>()

        val blockItems = mutableListOf(
                FullTransactionItem(R.string.FullInfo_Time, value = data.date?.let { DateHelper.getFullDate(it) }, icon = FullTransactionIcon.TIME),
                FullTransactionItem(R.string.FullInfo_Block, value = data.height.toString(), icon = FullTransactionIcon.BLOCK)
        )

        data.confirmations?.let {
            blockItems.add(FullTransactionItem(R.string.FullInfo_Confirmations, value = data.confirmations, icon = FullTransactionIcon.CHECK))
        }

        sections.add(FullTransactionSection(items = blockItems))

        val transactionItems = mutableListOf(FullTransactionItem(R.string.FullInfo_Fee, value = "${App.numberFormatter.format(data.fee)} ${coin.code}"))

        data.size?.let {
            transactionItems.add(FullTransactionItem(R.string.FullInfo_Size, value = "$it (bytes)", dimmed = true))
        }

        data.feePerByte?.let { feePerByte ->
            transactionItems.add(FullTransactionItem(R.string.FullInfo_Rate, value = "${App.numberFormatter.format(feePerByte)} ($unitName)", dimmed = true))
        }

        sections.add(FullTransactionSection(items = transactionItems))

        if (data.inputs.isNotEmpty()) {
            val totalInput = App.numberFormatter.format(data.inputs.sumByDouble { it.value })
            val inputs = mutableListOf(FullTransactionItem(R.string.FullInfo_SubtitleInputs, value = "$totalInput ${coin.code}"))
            data.inputs.map {
                val amount = App.numberFormatter.format(it.value)
                inputs.add(FullTransactionItem(title = "$amount ${coin.code}", value = it.address, clickable = true, icon = FullTransactionIcon.PERSON))
            }

            sections.add(FullTransactionSection(inputs))
        }

        if (data.outputs.isNotEmpty()) {
            val totalOutput = App.numberFormatter.format(data.outputs.sumByDouble { it.value })
            val outputs = mutableListOf(FullTransactionItem(R.string.FullInfo_SubtitleOutputs, value = "$totalOutput ${coin.code}"))

            data.outputs.map {
                val amount = App.numberFormatter.format(it.value)
                outputs.add(FullTransactionItem(title = "$amount ${coin.code}", value = it.address, clickable = true, icon = FullTransactionIcon.PERSON))
            }

            sections.add(FullTransactionSection(outputs))
        }

        return FullTransactionRecord(provider.name, sections)
    }

}
