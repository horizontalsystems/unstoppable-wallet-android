package bitcoin.wallet.modules.backup

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v4.app.FragmentActivity
import android.support.v7.app.AlertDialog
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.CheckBox
import bitcoin.wallet.R
import org.jetbrains.anko.childrenSequence

class BackupConfirmAlert : DialogFragment() {

    interface Listener {
        fun backupConfirmationSuccess()
    }

    private var mDialog: Dialog? = null
    private lateinit var rootView: View
    private lateinit var btnConfirm: Button

    private lateinit var listener: Listener

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = activity?.let { AlertDialog.Builder(it, R.style.BottomDialog) }

        rootView = View.inflate(context, R.layout.fragment_bottom_sheet_backup_confirm, null) as ViewGroup
        btnConfirm = rootView.findViewById(R.id.btnConfirm)
        builder?.setView(rootView)

        mDialog = builder?.create()
        mDialog?.window?.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT)
        mDialog?.window?.setGravity(Gravity.BOTTOM)
        mDialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        rootView.childrenSequence().filter { it is CheckBox }.forEach {
            (it as CheckBox).setOnCheckedChangeListener { _, _ ->
                checkConfirmations()
            }
        }

        btnConfirm.setOnClickListener {
            listener.backupConfirmationSuccess()
            dismiss()
        }

        return mDialog as Dialog
    }

    private fun checkConfirmations() {
        val uncheckedCount = rootView.childrenSequence().filter { (it is CheckBox) && !it.isChecked }.count()
        btnConfirm.isEnabled = uncheckedCount == 0
    }

    companion object {
        fun show(activity: FragmentActivity, listener: Listener) {
            val fragment = BackupConfirmAlert()
            fragment.listener = listener
            val ft = activity.supportFragmentManager.beginTransaction()
            ft.add(fragment, "backup_confirm_alert")
            ft.commitAllowingStateLoss()
        }
    }

}
