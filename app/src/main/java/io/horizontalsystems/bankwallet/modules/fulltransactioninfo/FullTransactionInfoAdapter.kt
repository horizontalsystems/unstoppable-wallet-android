package io.horizontalsystems.bankwallet.modules.fulltransactioninfo

import com.google.gson.JsonObject
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.entities.*
import io.horizontalsystems.bankwallet.viewHelpers.DateHelper
import java.math.BigInteger

class FullTransactionBitcoinAdapter(val provider: FullTransactionInfoModule.BitcoinForksProvider, val coin: Coin, val unitName: String)
    : FullTransactionInfoModule.Adapter {

    override fun convert(json: JsonObject): FullTransactionRecord {
        val data = provider.convert(json)
        val sections = mutableListOf<FullTransactionSection>()

        val blockItems = mutableListOf(
                FullTransactionItem(R.string.FullInfo_Time, value = DateHelper.getFullDateWithShortMonth(data.date), icon = FullTransactionIcon.TIME),
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

class FullTransactionEthereumAdapter(val provider: FullTransactionInfoModule.EthereumForksProvider, val coin: Coin)
    : FullTransactionInfoModule.Adapter {

    override fun convert(json: JsonObject): FullTransactionRecord {
        val data = provider.convert(json)
        val sections = mutableListOf<FullTransactionSection>()

        mutableListOf<FullTransactionItem>().let { section ->
            data.date?.let {
                section.add(FullTransactionItem(R.string.FullInfo_Time, value = DateHelper.getFullDateWithShortMonth(it), icon = FullTransactionIcon.TIME))
            }
            section.add(FullTransactionItem(R.string.FullInfo_Block, value = data.height, icon = FullTransactionIcon.BLOCK))
            data.confirmations?.let {
                section.add(FullTransactionItem(R.string.FullInfo_Confirmations, value = it.toString(), icon = FullTransactionIcon.CHECK))
            }

            val amount = if (coin.type is CoinType.Erc20) {
                data.value.divide(BigInteger.TEN.pow(coin.type.decimal)).toDouble()
            } else {
                data.value.toDouble() / EthereumResponse.ethRate
            }

            section.add(FullTransactionItem(R.string.FullInfoEth_Amount, value = "${App.numberFormatter.format(amount)} ${coin.code}"))

            data.nonce?.let {
                section.add(FullTransactionItem(R.string.FullInfoEth_Nonce, value = it, dimmed = true))
            }

            sections.add(FullTransactionSection(section))
        }

        mutableListOf<FullTransactionItem>().let { section ->
            data.fee?.let {
                section.add(FullTransactionItem(R.string.FullInfo_Fee, value = "${App.numberFormatter.format(it.toDouble())} ETH"))
            }
            if (data.size != null) {
                section.add(FullTransactionItem(R.string.FullInfo_Size, value = "${data.size} (bytes)", dimmed = true))
            }
            section.add(FullTransactionItem(R.string.FullInfo_GasLimit, value = data.gasLimit, dimmed = true))
            data.gasUsed?.let {
                section.add(FullTransactionItem(R.string.FullInfo_GasUsed, value = data.gasUsed, dimmed = true))
            }
            data.gasPrice?.let {
                section.add(FullTransactionItem(R.string.FullInfo_GasPrice, value = "$it GWei", dimmed = true))
            }

            sections.add(FullTransactionSection(section))
        }

        mutableListOf<FullTransactionItem>().let { section ->
            data.contractAddress?.let {
                section.add(FullTransactionItem(R.string.FullInfo_Contract, value = data.contractAddress, clickable = true, icon = FullTransactionIcon.TOKEN))
            }

            section.add(FullTransactionItem(R.string.FullInfo_From, value = data.from, clickable = true, icon = FullTransactionIcon.PERSON))
            section.add(FullTransactionItem(R.string.FullInfo_To, value = data.to, clickable = true, icon = FullTransactionIcon.PERSON))

            sections.add(FullTransactionSection(section))
        }

        return FullTransactionRecord(provider.name, sections)
    }

}
