package io.horizontalsystems.bankwallet.modules.fulltransactioninfo

import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.entities.FullTransactionIcon
import io.horizontalsystems.bankwallet.entities.FullTransactionItem
import io.horizontalsystems.bankwallet.entities.FullTransactionRecord
import io.horizontalsystems.bankwallet.entities.FullTransactionSection
import io.horizontalsystems.bankwallet.modules.transactions.CoinCode
import io.horizontalsystems.bankwallet.viewHelpers.DateHelper
import io.horizontalsystems.bankwallet.viewHelpers.ValueFormatter

class FullTransactionBitcoinAdapter(val coinCode: CoinCode) : FullTransactionInfoModule.Adapter {

    override fun convert(response: FullTransactionResponse): FullTransactionRecord? {
        val data = response as BitcoinResponse
        val sections = mutableListOf<FullTransactionSection>()

        sections.add(FullTransactionSection(items = listOf(
                FullTransactionItem(R.string.FullInfo_Id, value = data.hash, clickable = true, icon = FullTransactionIcon.HASH)
        )))

        val blockItems = mutableListOf(
                FullTransactionItem(R.string.FullInfo_Time, value = DateHelper.getFullDateWithShortMonth(data.date)),
                FullTransactionItem(R.string.FullInfo_Block, value = data.height.toString())
        )

        data.confirmations?.let {
            blockItems.add(FullTransactionItem(R.string.FullInfo_Confirmations, value = data.confirmations))
        }

        sections.add(FullTransactionSection(items = blockItems))

        val totalInput = ValueFormatter.format(data.inputs.sumByDouble { it.value })
        val totalOutput = ValueFormatter.format(data.outputs.sumByDouble { it.value })
        val transactionItems = mutableListOf(
                FullTransactionItem(R.string.FullInfo_TotalInput, value = "$totalInput $coinCode"),
                FullTransactionItem(R.string.FullInfo_TotalOutput, value = "$totalOutput $coinCode")
        )

        data.feePerByte?.let { feePerByte ->
            transactionItems.add(FullTransactionItem(R.string.FullInfo_FeePerByte, value = ValueFormatter.format(feePerByte), valueUnit = R.string.FullInfo_SatByte))
        }

        data.size?.let {
            transactionItems.add(FullTransactionItem(R.string.FullInfo_Size, value = it.toString(), valueUnit = R.string.FullInfo_Bytes))
        }

        transactionItems.add(FullTransactionItem(R.string.FullInfo_Fee, value = "${ValueFormatter.format(data.fee)} $coinCode"))
        sections.add(FullTransactionSection(items = transactionItems))

        if (data.inputs.isNotEmpty()) {
            sections.add(FullTransactionSection(R.string.FullInfo_SubtitleInputs, data.inputs.map {
                val amount = ValueFormatter.format(it.value)
                FullTransactionItem(title = "$amount $coinCode", value = it.address, clickable = true, icon = FullTransactionIcon.PERSON)
            }))
        }

        if (data.outputs.isNotEmpty()) {
            sections.add(FullTransactionSection(R.string.FullInfo_SubtitleOutputs, data.outputs.map {
                val amount = ValueFormatter.format(it.value)
                FullTransactionItem(title = "$amount $coinCode", value = it.address, clickable = true, icon = FullTransactionIcon.PERSON)
            }))
        }

        return FullTransactionRecord(sections)
    }

}

class FullTransactionEthereumAdapter(val coinCode: CoinCode) : FullTransactionInfoModule.Adapter {
    override fun convert(response: FullTransactionResponse): FullTransactionRecord? {
        val data = response as EthereumResponse
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
            transactionItems.add(FullTransactionItem(R.string.FullInfo_Size, value = data.size.toString(), valueUnit = R.string.FullInfo_Bytes))
        }

        blockItems.add(FullTransactionItem(R.string.FullInfoEth_GasLimit, value = "${data.gasLimit} GWei"))
        blockItems.add(FullTransactionItem(R.string.FullInfoEth_GasPrice, value = "${data.gasPrice} GWei"))

        data.gasUsed?.let {
            transactionItems.add(FullTransactionItem(R.string.FullInfoEth_GasUsed, value = "${data.gasUsed} GWei"))
        }

        data.fee?.let {
            transactionItems.add(FullTransactionItem(R.string.FullInfo_Fee, value = "${ValueFormatter.format(it.toDouble())} $coinCode"))
        }

        sections.add(FullTransactionSection(items = transactionItems))
        sections.add(FullTransactionSection(items = listOf(
                FullTransactionItem(R.string.FullInfoEth_Nonce, value = data.nonce),
                FullTransactionItem(R.string.FullInfoEth_Value, value = "${data.value} $coinCode"),
                FullTransactionItem(R.string.FullInfoEth_From, value = data.from, clickable = true, icon = FullTransactionIcon.PERSON),
                FullTransactionItem(R.string.FullInfoEth_To, value = data.to, clickable = true, icon = FullTransactionIcon.PERSON)
        )))

        return FullTransactionRecord(sections)
    }

}
