package io.horizontalsystems.bankwallet.modules.fulltransactioninfo.adapters

import com.google.gson.JsonObject
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.WrongAccountTypeForThisProvider
import io.horizontalsystems.bankwallet.entities.*
import io.horizontalsystems.bankwallet.modules.fulltransactioninfo.FullTransactionInfoModule
import io.horizontalsystems.bankwallet.viewHelpers.DateHelper
import java.util.*

class FullTransactionEosAdapter(val provider: FullTransactionInfoModule.EosProvider, val wallet: Wallet)
    : FullTransactionInfoModule.Adapter {

    override fun convert(json: JsonObject): FullTransactionRecord {
        val eosAccount = (wallet.account.type as? AccountType.Eos)?.account
                ?: throw WrongAccountTypeForThisProvider()
        val data = provider.convert(json, eosAccount)
        val sections = mutableListOf<FullTransactionSection>()

        mutableListOf<FullTransactionItem>().let { section ->
            section.add(FullTransactionItem(R.string.FullInfo_Time, value = DateHelper.getFullDate(Date(data.blockTimeStamp)), icon = FullTransactionIcon.TIME))
            section.add(FullTransactionItem(R.string.FullInfo_Block, value = data.blockNumber, icon = FullTransactionIcon.BLOCK))
            section.add(FullTransactionItem(R.string.FullInfo_Status, value = data.status, icon = FullTransactionIcon.CHECK))

            sections.add(FullTransactionSection(section))
        }

        data.actions.forEach { action ->
            mutableListOf<FullTransactionItem>().let { section ->
                section.add(FullTransactionItem(R.string.FullInfo_Contract, value = action.account, clickable = true, icon = FullTransactionIcon.TOKEN))
                section.add(FullTransactionItem(R.string.FullInfoEth_Amount, value = action.amount))
                section.add(FullTransactionItem(R.string.FullInfo_From, value = action.from, clickable = true, icon = FullTransactionIcon.PERSON))
                section.add(FullTransactionItem(R.string.FullInfo_To, value = action.to, clickable = true, icon = FullTransactionIcon.PERSON))

                if (action.memo.isNotBlank()) {
                    section.add(FullTransactionItem(R.string.FullInfo_Memo, value = action.memo, clickable = true))
                }

                sections.add(FullTransactionSection(section))
            }
        }

        mutableListOf<FullTransactionItem>().let { section ->
            section.add(FullTransactionItem(R.string.FullInfo_CpuUsage, value = "${data.cpuUsage} \u00B5s"))
            section.add(FullTransactionItem(R.string.FullInfo_NetUsage, value = "${data.netUsage * 8} Bytes"))

            sections.add(FullTransactionSection(section))
        }

        return FullTransactionRecord(provider.name, sections)
    }

}
