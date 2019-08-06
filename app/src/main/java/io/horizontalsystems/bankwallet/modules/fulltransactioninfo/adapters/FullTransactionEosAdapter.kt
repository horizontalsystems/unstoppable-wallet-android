package io.horizontalsystems.bankwallet.modules.fulltransactioninfo.adapters

import com.google.gson.JsonObject
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.entities.*
import io.horizontalsystems.bankwallet.modules.fulltransactioninfo.FullTransactionInfoModule
import io.horizontalsystems.bankwallet.viewHelpers.DateHelper
import java.util.*

class FullTransactionEosAdapter(val provider: FullTransactionInfoModule.EosProvider, val coin: Coin)
    : FullTransactionInfoModule.Adapter {

    override fun convert(json: JsonObject): FullTransactionRecord {
        val data = provider.convert(json)
        val sections = mutableListOf<FullTransactionSection>()

        mutableListOf<FullTransactionItem>().let { section ->
            section.add(FullTransactionItem(R.string.FullInfo_Time, value = DateHelper.getFullDateWithShortMonth(Date(data.blockTimeStamp)), icon = FullTransactionIcon.TIME))
            section.add(FullTransactionItem(R.string.FullInfo_Block, value = data.blockNumber, icon = FullTransactionIcon.BLOCK))
            section.add(FullTransactionItem(R.string.FullInfo_Status, value = data.status, icon = FullTransactionIcon.CHECK))

            sections.add(FullTransactionSection(section))
        }

        mutableListOf<FullTransactionItem>().let { section ->
            section.add(FullTransactionItem(R.string.FullInfo_Contract, value = data.account, clickable = true, icon = FullTransactionIcon.TOKEN))
            section.add(FullTransactionItem(R.string.FullInfoEth_Amount, value = data.amount))
            section.add(FullTransactionItem(R.string.FullInfo_From, value = data.from, clickable = true, icon = FullTransactionIcon.PERSON))
            section.add(FullTransactionItem(R.string.FullInfo_To, value = data.to, clickable = true, icon = FullTransactionIcon.PERSON))

            if (data.memo.isNotEmpty()) {
                section.add(FullTransactionItem(R.string.FullInfo_Memo, value = data.memo, clickable = true))
            }

            sections.add(FullTransactionSection(section))
        }

        mutableListOf<FullTransactionItem>().let { section ->
            section.add(FullTransactionItem(R.string.FullInfo_CpuUsage, value = "${data.cpuUsage} \u00B5s"))
            section.add(FullTransactionItem(R.string.FullInfo_NetUsage, value = "${data.netUsage * 8} Bytes"))

            sections.add(FullTransactionSection(section))
        }

        return FullTransactionRecord(provider.name, sections)
    }

}
