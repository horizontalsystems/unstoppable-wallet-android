package io.horizontalsystems.bankwallet.modules.coin.audits

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.IRateManager
import io.horizontalsystems.bankwallet.core.managers.Auditor
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.marketkit.models.CoinType
import io.horizontalsystems.views.ListPosition
import io.reactivex.disposables.Disposable
import java.util.*

class CoinAuditsViewModel(private val xRateManager: IRateManager, private val coinType: CoinType) : ViewModel() {

    val coinAudits = MutableLiveData<List<CoinAuditItem>>()
    val loadingLiveData = MutableLiveData(true)
    val coinInfoErrorLiveData = MutableLiveData<String>()
    val showPoweredByLiveData = MutableLiveData(false)

    private var disposable: Disposable? = null

    init {
        getCoinList()
    }

    override fun onCleared() {
        disposable?.dispose()
    }

    private fun getCoinList() {
        coinAudits.postValue(emptyList())
        loadingLiveData.postValue(true)
        showPoweredByLiveData.postValue(false)

        xRateManager.getAuditsAsync(coinType)
            .subscribeIO({ audits ->
                syncViewItems(audits)
            }, {
                loadingLiveData.postValue(false)
                coinInfoErrorLiveData.postValue(Translator.getString(R.string.CoinPage_Audits_FetchError))
            }).let {
                disposable = it
            }
    }

    private fun syncViewItems(audits: List<Auditor>) {
        loadingLiveData.postValue(false)
        coinAudits.postValue(getViewItem(audits))
        showPoweredByLiveData.postValue(audits.isNotEmpty())

        if (audits.isEmpty()) {
            coinInfoErrorLiveData.postValue(Translator.getString(R.string.CoinPage_Audits_Empty))
        }
    }

    private fun getViewItem(audits: List<Auditor>): List<CoinAuditItem> {
        if (audits.isEmpty()) {
            return emptyList()
        }

        val list = mutableListOf<CoinAuditItem>()
        audits.forEach { auditor ->
            list.add(CoinAuditItem.Header(auditor.name))

            auditor.reports.forEachIndexed { index, report ->
                list.add(
                    CoinAuditItem.Report(
                        report.name,
                        Date(report.timestamp * 1000),
                        report.issues,
                        report.link,
                        ListPosition.Companion.getListPosition(auditor.reports.size, index)
                    )
                )
            }
        }

        return list
    }
}
