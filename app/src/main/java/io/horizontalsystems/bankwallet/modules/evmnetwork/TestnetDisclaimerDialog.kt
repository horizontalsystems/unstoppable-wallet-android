package io.horizontalsystems.bankwallet.modules.evmnetwork

import android.app.Dialog
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.setOnSingleClickListener

class TestnetDisclaimerDialog : DialogFragment() {

    var onConfirm: (() -> Unit)? = null

    override fun onCreateDialog(bundle: Bundle?): Dialog {
        val rootView = View.inflate(context, R.layout.fragment_disclaimer, null) as ViewGroup

        rootView.findViewById<Button>(R.id.buttonConfirm)?.setOnSingleClickListener {
            onConfirm?.invoke()
        }
        rootView.findViewById<TextView>(R.id.text)?.text = getString(R.string.NetworkSettings_TestNetDisclaimer)

        val builder = activity?.let {
            AlertDialog.Builder(
                it,
                io.horizontalsystems.pin.R.style.AlertDialog
            )
        }
        builder?.setView(rootView)
        val mDialog = builder?.create()
        mDialog?.setCanceledOnTouchOutside(true)

        return mDialog as Dialog
    }
}
