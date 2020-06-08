package io.horizontalsystems.bankwallet.modules.fulltransactioninfo.adapters

import com.google.gson.JsonObject
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.providers.FeeCoinProvider
import io.horizontalsystems.bankwallet.entities.*
import io.horizontalsystems.bankwallet.modules.fulltransactioninfo.FullTransactionInfoModule
import io.horizontalsystems.bankwallet.modules.fulltransactioninfo.providers.EthereumResponse
import io.horizontalsystems.core.helpers.DateHelper
import java.math.BigDecimal
import java.math.BigInteger

class FullTransactionEthereumAdapter(private val provider: FullTransactionInfoModule.EthereumForksProvider,
                                     private val feeCoinProvider: FeeCoinProvider,
                                     private val coin: Coin)
    : FullTransactionInfoModule.Adapter {

    override fun convert(json: JsonObject): FullTransactionRecord {
        val data = provider.convert(json)
        val sections = mutableListOf<FullTransactionSection>()

        mutableListOf<FullTransactionItem>().let { section ->
            data.date?.let {
                section.add(FullTransactionItem(R.string.FullInfo_Time, value = DateHelper.getFullDate(it), icon = FullTransactionIcon.TIME))
            }
            section.add(FullTransactionItem(R.string.FullInfo_Block, value = data.height, icon = FullTransactionIcon.BLOCK))
            data.confirmations?.let {
                section.add(FullTransactionItem(R.string.FullInfo_Confirmations, value = it.toString(), icon = FullTransactionIcon.CHECK))
            }

            val amount = if (coin.type is CoinType.Erc20) {
                data.value.divide(BigInteger.TEN.pow(coin.decimal)).toDouble()
            } else {
                data.value.toDouble() / EthereumResponse.ethRate
            }

            section.add(FullTransactionItem(R.string.FullInfoEth_Amount, value = App.numberFormatter.formatCoin(amount, coin.code, 0, 8)))

            data.nonce?.let {
                section.add(FullTransactionItem(R.string.FullInfoEth_Nonce, value = it, dimmed = true))
            }

            sections.add(FullTransactionSection(section))
        }

        mutableListOf<FullTransactionItem>().let { section ->
            data.fee?.let {
                val feeCoin = feeCoinProvider.feeCoinData(coin)?.first ?: coin
                section.add(FullTransactionItem(R.string.FullInfo_Fee, value = App.numberFormatter.formatCoin(BigDecimal(it), feeCoin.code, 0, 8)))
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
