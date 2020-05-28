package io.horizontalsystems.bankwallet.modules.fulltransactioninfo.adapters

import com.google.gson.JsonObject
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.entities.*
import io.horizontalsystems.bankwallet.modules.fulltransactioninfo.FullTransactionInfoModule
import io.horizontalsystems.core.helpers.DateHelper
import java.math.BigDecimal

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

        val transactionItems = mutableListOf(FullTransactionItem(R.string.FullInfo_Fee, value = App.numberFormatter.formatCoin(data.fee, coin.code, 0, 8)))

        data.size?.let {
            transactionItems.add(FullTransactionItem(R.string.FullInfo_Size, value = "$it (bytes)", dimmed = true))
        }

        data.feePerByte?.let { feePerByte ->
            transactionItems.add(FullTransactionItem(R.string.FullInfo_Rate, value = "${App.numberFormatter.formatSimple(feePerByte, 0, 0)} ($unitName)", dimmed = true))
        }

        sections.add(FullTransactionSection(items = transactionItems))

        if (data.inputs.isNotEmpty()) {
            val totalInput = App.numberFormatter.formatCoin(data.inputs.sumByDouble { it.value }, coin.code, 0, 8)
            val inputs = mutableListOf(FullTransactionItem(R.string.FullInfo_SubtitleInputs, value = totalInput))
            data.inputs.map {
                val amount = App.numberFormatter.formatCoin(it.value, coin.code, 0, 8)
                inputs.add(FullTransactionItem(title = amount, value = it.address, clickable = true, icon = FullTransactionIcon.PERSON))
            }

            sections.add(FullTransactionSection(inputs))
        }

        if (data.outputs.isNotEmpty()) {
            val totalOutput = App.numberFormatter.formatCoin(data.outputs.sumByDouble { it.value }, coin.code, 0, 8)
            val outputs = mutableListOf(FullTransactionItem(R.string.FullInfo_SubtitleOutputs, value = totalOutput))

            data.outputs.map {
                val amount = App.numberFormatter.formatCoin(it.value, coin.code, 0, 8)
                outputs.add(FullTransactionItem(title = amount, value = it.address, clickable = true, icon = FullTransactionIcon.PERSON))
            }

            sections.add(FullTransactionSection(outputs))
        }

        return FullTransactionRecord(provider.name, sections)
    }

}
