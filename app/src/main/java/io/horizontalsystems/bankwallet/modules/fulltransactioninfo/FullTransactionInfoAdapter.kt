package io.horizontalsystems.bankwallet.modules.fulltransactioninfo

import com.google.gson.JsonObject
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.entities.FullTransactionIcon
import io.horizontalsystems.bankwallet.entities.FullTransactionItem
import io.horizontalsystems.bankwallet.entities.FullTransactionRecord
import io.horizontalsystems.bankwallet.entities.FullTransactionSection
import io.horizontalsystems.bankwallet.modules.transactions.CoinCode
import io.horizontalsystems.bankwallet.viewHelpers.DateHelper
import io.horizontalsystems.bankwallet.viewHelpers.ValueFormatter

class FullTransactionBitcoinAdapter(val provider: FullTransactionInfoModule.BitcoinForksProvider, val coinCode: CoinCode)
    : FullTransactionInfoModule.Adapter {

    override fun convert(json: JsonObject): FullTransactionRecord {
        val data = provider.convert(json)
        val sections = mutableListOf<FullTransactionSection>()

        sections.add(FullTransactionSection(items = listOf(
                FullTransactionItem(R.string.FullInfo_Id, value = data.hash, clickable = true, icon = FullTransactionIcon.HASH)
        )))

        val blockItems = mutableListOf(
                FullTransactionItem(R.string.FullInfo_Time, value = DateHelper.getFullDateWithShortMonth(data.date), icon = FullTransactionIcon.TIME),
                FullTransactionItem(R.string.FullInfo_Block, value = data.height.toString(), icon = FullTransactionIcon.BLOCK)
        )

        data.confirmations?.let {
            blockItems.add(FullTransactionItem(R.string.FullInfo_Confirmations, value = data.confirmations, icon = FullTransactionIcon.CHECK))
        }

        sections.add(FullTransactionSection(items = blockItems))

        val transactionItems = mutableListOf(FullTransactionItem(R.string.FullInfo_Fee, value = "${ValueFormatter.format(data.fee)} $coinCode"))

        data.size?.let {
            transactionItems.add(FullTransactionItem(R.string.FullInfo_Size, value = "$it (bytes)"))
        }

        data.feePerByte?.let { feePerByte ->
            transactionItems.add(FullTransactionItem(R.string.FullInfo_FeeRate, value = "${ValueFormatter.format(feePerByte)} (satoshi)"))
        }

        sections.add(FullTransactionSection(items = transactionItems))

        if (data.inputs.isNotEmpty()) {
            val totalInput = ValueFormatter.format(data.inputs.sumByDouble { it.value })
            val inputs = mutableListOf(FullTransactionItem(R.string.FullInfo_SubtitleInputs, value = "$totalInput $coinCode"))
            data.inputs.map {
                val amount = ValueFormatter.format(it.value)
                inputs.add(FullTransactionItem(title = "$amount $coinCode", value = it.address, clickable = true, icon = FullTransactionIcon.PERSON))
            }

            sections.add(FullTransactionSection(inputs))
        }

        if (data.outputs.isNotEmpty()) {
            val totalOutput = ValueFormatter.format(data.outputs.sumByDouble { it.value })
            val outputs = mutableListOf(FullTransactionItem(R.string.FullInfo_SubtitleOutputs, value = "$totalOutput $coinCode"))

            data.outputs.map {
                val amount = ValueFormatter.format(it.value)
                outputs.add(FullTransactionItem(title = "$amount $coinCode", value = it.address, clickable = true, icon = FullTransactionIcon.PERSON))
            }

            sections.add(FullTransactionSection(outputs))
        }

        return FullTransactionRecord(provider.name, sections)
    }

}

class FullTransactionEthereumAdapter(val provider: FullTransactionInfoModule.EthereumForksProvider, val coinCode: CoinCode)
    : FullTransactionInfoModule.Adapter {

    override fun convert(json: JsonObject): FullTransactionRecord {
        val data = provider.convert(json)
        val sections = mutableListOf<FullTransactionSection>()

        sections.add(FullTransactionSection(items = listOf(
                FullTransactionItem(R.string.FullInfo_Id, value = data.hash, clickable = true, icon = FullTransactionIcon.HASH)
        )))

        val blockItems = mutableListOf<FullTransactionItem>()
        data.date?.let { FullTransactionItem(R.string.FullInfo_Time, value = DateHelper.getFullDateWithShortMonth(it)) }
        blockItems.add(FullTransactionItem(R.string.FullInfo_Block, value = data.height))
        data.confirmations?.let { FullTransactionItem(R.string.FullInfo_Confirmations, value = it.toString()) }

        sections.add(FullTransactionSection(items = blockItems))

        val transactionItems = mutableListOf<FullTransactionItem>()
        if (data.size != null) {
            transactionItems.add(FullTransactionItem(R.string.FullInfo_Size, value = "${data.size} (bytes)"))
        }

        blockItems.add(FullTransactionItem(R.string.FullInfo_GasLimit, value = "${data.gasLimit} GWei"))
        blockItems.add(FullTransactionItem(R.string.FullInfo_GasPrice, value = "${data.gasPrice} GWei"))

        data.gasUsed?.let {
            transactionItems.add(FullTransactionItem(R.string.FullInfo_GasUsed, value = "${data.gasUsed} GWei"))
        }

        data.fee?.let {
            transactionItems.add(FullTransactionItem(R.string.FullInfo_Fee, value = "${ValueFormatter.format(it.toDouble())} $coinCode"))
        }

        sections.add(FullTransactionSection(items = transactionItems))
        sections.add(FullTransactionSection(items = listOf(
                FullTransactionItem(R.string.FullInfoEth_Nonce, value = data.nonce),
                FullTransactionItem(R.string.FullInfoEth_Value, value = "${data.value} $coinCode"),
                FullTransactionItem(R.string.FullInfo_From, value = data.from, clickable = true, icon = FullTransactionIcon.PERSON),
                FullTransactionItem(R.string.FullInfo_To, value = data.to, clickable = true, icon = FullTransactionIcon.PERSON)
        )))

        return FullTransactionRecord(provider.name, sections)
    }

}
