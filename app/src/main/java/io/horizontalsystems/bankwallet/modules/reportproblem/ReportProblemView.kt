package io.horizontalsystems.bankwallet.modules.reportproblem

import androidx.lifecycle.MutableLiveData

class ReportProblemView : ReportProblemModule.IView {
    val emailLiveData = MutableLiveData<String>()
    val telegramGroupLiveData = MutableLiveData<String>()

    override fun setEmail(email: String) {
        emailLiveData.postValue(email)
    }

    override fun setTelegramGroup(group: String) {
        telegramGroupLiveData.postValue(group)
    }
}
