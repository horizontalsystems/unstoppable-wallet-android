package io.horizontalsystems.bankwallet.modules.manageaccount.dialogs

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.IAccountManager
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString

class UnlinkConfirmationDialogViewModel(
    private val account: Account,
    private val accountManager: IAccountManager
) : ViewModel() {
    val accountName: String = account.name
    val message: TranslatableString?

    private val confirmationList: List<TranslatableString>

    init {
        if (account.isWatchAccount) {
            confirmationList = listOf()
            message = TranslatableString.ResString(R.string.ManageAccount_DeleteWarning)
        } else {
            confirmationList = listOf(
                TranslatableString.ResString(R.string.ManageAccount_Delete_ConfirmationRemove),
                TranslatableString.ResString(R.string.ManageAccount_Delete_ConfirmationDisable),
                TranslatableString.ResString(R.string.ManageAccount_Delete_ConfirmationLose)
            )
            message = null
        }
    }

    val items = mutableListOf<CheckBoxItem>().apply {
        addAll(confirmationList.map { CheckBoxItem(it) })
    }
    val itemsLiveData = MutableLiveData(items.toList())
    val buttonEnabledLiveData = MutableLiveData(items.isEmpty())

    fun updateItem(index: Int, item: CheckBoxItem, checked: Boolean) {
        items.removeAt(index)
        items.add(index, CheckBoxItem(item.text, checked))
        itemsLiveData.postValue(items.toList())
        buttonEnabledLiveData.postValue(items.all { it.checked })
    }

    fun onUnlinkConfirm() {
        accountManager.delete(account.id)
    }

}

data class CheckBoxItem(val text: TranslatableString, val checked: Boolean = false)