package io.horizontalsystems.bankwallet.lib

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.modules.balance.BalanceSortType


class BalanceSortDialogFragment : DialogFragment() {

    interface Listener {
        fun onSortItemSelect(sortType: BalanceSortType)
    }

    private val sortTypes = listOf(BalanceSortType.Balance, BalanceSortType.Az, BalanceSortType.Default)

    private var listener: Listener? = null

    override fun onCreateDialog(bundle: Bundle?): Dialog {
        val builder = activity?.let { AlertDialog.Builder(it, R.style.ListAlertDialog) }

        val items = sortTypes.map { getString(it.getTitleRes()) }.toTypedArray()

        builder?.setItems(items) { _, which ->
            listener?.onSortItemSelect(sortTypes[which])
            dismiss()
        }

        val mDialog = builder?.create()

        return mDialog as Dialog
    }

    companion object {
        fun newInstance(listener: Listener? = null): BalanceSortDialogFragment {
            val dialog = BalanceSortDialogFragment()
            dialog.listener = listener
            return dialog
        }
    }

}
