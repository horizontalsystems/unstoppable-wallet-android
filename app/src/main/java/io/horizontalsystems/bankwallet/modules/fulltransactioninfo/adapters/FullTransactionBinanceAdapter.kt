package io.horizontalsystems.bankwallet.modules.fulltransactioninfo.adapters

import com.google.gson.JsonObject
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.entities.*
import io.horizontalsystems.bankwallet.modules.fulltransactioninfo.FullTransactionInfoModule
import java.math.BigDecimal

class FullTransactionBinanceAdapter(val provider: FullTransactionInfoModule.BinanceProvider, val coin: Coin)
    : FullTransactionInfoModule.Adapter {

    override fun convert(json: JsonObject): FullTransactionRecord {
        val data = provider.convert(json)
        val sections = mutableListOf<FullTransactionSection>()

        mutableListOf<FullTransactionItem>().let { section ->
            section.add(FullTransactionItem(R.string.FullInfo_Block, value = data.blockHeight, icon = FullTransactionIcon.BLOCK))
            sections.add(FullTransactionSection(section))
        }

        mutableListOf<FullTransactionItem>().let { section ->

            val amount = data.value.divide(BigDecimal.TEN.pow(8))

            section.add(FullTransactionItem(R.string.FullInfo_Fee, value = App.numberFormatter.format(CoinValue("BNB", data.fee))))
            section.add(FullTransactionItem(R.string.FullInfoEth_Amount, value = App.numberFormatter.format(CoinValue(coin.code, amount))))
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
