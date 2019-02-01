package io.horizontalsystems.bankwallet.modules.fulltransactioninfo

import com.google.gson.JsonObject
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.entities.FullTransactionIcon
import io.horizontalsystems.bankwallet.entities.FullTransactionItem
import io.horizontalsystems.bankwallet.entities.FullTransactionRecord
import io.horizontalsystems.bankwallet.entities.FullTransactionSection
import io.horizontalsystems.bankwallet.modules.transactions.CoinCode
import io.horizontalsystems.bankwallet.viewHelpers.DateHelper

class FullTransactionBitcoinAdapter(val provider: FullTransactionInfoModule.BitcoinForksProvider, val coinCode: CoinCode)
    : FullTransactionInfoModule.Adapter {

    override fun convert(json: JsonObject): FullTransactionRecord {
        val data = provider.convert(json)
        val sections = mutableListOf<FullTransactionSection>()

        val blockItems = mutableListOf(
                FullTransactionItem(R.string.FullInfo_Id, value = data.hash, clickable = true, icon = FullTransactionIcon.HASH),
                FullTransactionItem(R.string.FullInfo_Time, value = DateHelper.getFullDateWithShortMonth(data.date), icon = FullTransactionIcon.TIME),
                FullTransactionItem(R.string.FullInfo_Block, value = data.height.toString(), icon = FullTransactionIcon.BLOCK)
        )

        data.confirmations?.let {
            blockItems.add(FullTransactionItem(R.string.FullInfo_Confirmations, value = data.confirmations, icon = FullTransactionIcon.CHECK))
        }

        sections.add(FullTransactionSection(items = blockItems))

        val transactionItems = mutableListOf(FullTransactionItem(R.string.FullInfo_Fee, value = "${App.appNumberFormatter.format(data.fee)} $coinCode"))

        data.size?.let {
            transactionItems.add(FullTransactionItem(R.string.FullInfo_Size, value = "$it (bytes)", dimmed = true))
        }

        data.feePerByte?.let { feePerByte ->
            transactionItems.add(FullTransactionItem(R.string.FullInfo_Rate, value = "${App.appNumberFormatter.format(feePerByte)} (satoshi)", dimmed = true))
        }

        sections.add(FullTransactionSection(items = transactionItems))

        if (data.inputs.isNotEmpty()) {
            val totalInput = App.appNumberFormatter.format(data.inputs.sumByDouble { it.value })
            val inputs = mutableListOf(FullTransactionItem(R.string.FullInfo_SubtitleInputs, value = "$totalInput $coinCode"))
            data.inputs.map {
                val amount = App.appNumberFormatter.format(it.value)
                inputs.add(FullTransactionItem(title = "$amount $coinCode", value = it.address, clickable = true, icon = FullTransactionIcon.PERSON))
            }

            sections.add(FullTransactionSection(inputs))
        }

        if (data.outputs.isNotEmpty()) {
            val totalOutput = App.appNumberFormatter.format(data.outputs.sumByDouble { it.value })
            val outputs = mutableListOf(FullTransactionItem(R.string.FullInfo_SubtitleOutputs, value = "$totalOutput $coinCode"))

            data.outputs.map {
                val amount = App.appNumberFormatter.format(it.value)
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

        mutableListOf<FullTransactionItem>().let { section ->
            section.add(FullTransactionItem(R.string.FullInfo_Id, value = data.hash, clickable = true, icon = FullTransactionIcon.HASH))
            data.date?.let {
                section.add(FullTransactionItem(R.string.FullInfo_Time, value = DateHelper.getFullDateWithShortMonth(it), icon = FullTransactionIcon.TIME))
            }
            section.add(FullTransactionItem(R.string.FullInfo_Block, value = data.height, icon = FullTransactionIcon.BLOCK))
            data.confirmations?.let {
                section.add(FullTransactionItem(R.string.FullInfo_Confirmations, value = it.toString(), icon = FullTransactionIcon.CHECK))
            }
            section.add(FullTransactionItem(R.string.FullInfoEth_Value, value = "${data.value} $coinCode"))
            section.add(FullTransactionItem(R.string.FullInfoEth_Nonce, value = data.nonce, dimmed = true))

            sections.add(FullTransactionSection(section))
        }

        mutableListOf<FullTransactionItem>().let { section ->
            data.fee?.let {
                section.add(FullTransactionItem(R.string.FullInfo_Fee, value = "${App.appNumberFormatter.format(it.toDouble())} $coinCode"))
            }
            if (data.size != null) {
                section.add(FullTransactionItem(R.string.FullInfo_Size, value = "${data.size} (bytes)", dimmed = true))
            }
            section.add(FullTransactionItem(R.string.FullInfo_GasLimit, value = "${data.gasLimit} GWei", dimmed = true))
            section.add(FullTransactionItem(R.string.FullInfo_GasPrice, value = "${data.gasPrice} GWei", dimmed = true))
            data.gasUsed?.let {
                section.add(FullTransactionItem(R.string.FullInfo_GasUsed, value = "${data.gasUsed} GWei", dimmed = true))
            }

            sections.add(FullTransactionSection(section))
        }

        mutableListOf<FullTransactionItem>().let { section ->
            section.add(FullTransactionItem(R.string.FullInfo_From, value = data.from, clickable = true, icon = FullTransactionIcon.PERSON))
            section.add(FullTransactionItem(R.string.FullInfo_To, value = data.to, clickable = true, icon = FullTransactionIcon.PERSON))

            sections.add(FullTransactionSection(section))
        }

        return FullTransactionRecord(provider.name, sections)
    }

}
