package io.horizontalsystems.bankwallet.modules.transactionInfo.options

import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.modules.transactionInfo.TransactionInfoOption

class TransactionSpeedUpCancelViewModel(
    optionType: TransactionInfoOption.Type,
    val isTransactionPending: Boolean
) : ViewModel() {

    val title: String = when (optionType) {
        TransactionInfoOption.Type.SpeedUp -> Translator.getString(R.string.TransactionInfoOptions_SpeedUp_Title)
        TransactionInfoOption.Type.Cancel -> Translator.getString(R.string.TransactionInfoOptions_Cancel_Title)
    }

    val description: String = when (optionType) {
        TransactionInfoOption.Type.SpeedUp -> Translator.getString(R.string.TransactionInfoOptions_SpeedUp_Description)
        TransactionInfoOption.Type.Cancel -> Translator.getString(R.string.TransactionInfoOptions_Cancel_Description)
    }

    val buttonTitle: String = when (optionType) {
        TransactionInfoOption.Type.SpeedUp -> Translator.getString(R.string.TransactionInfoOptions_SpeedUp_Button)
        TransactionInfoOption.Type.Cancel -> Translator.getString(R.string.TransactionInfoOptions_Cancel_Button)
    }

}
