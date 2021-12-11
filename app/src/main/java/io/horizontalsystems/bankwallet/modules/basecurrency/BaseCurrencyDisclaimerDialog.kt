package io.horizontalsystems.bankwallet.modules.basecurrency

import android.app.Dialog
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
import androidx.fragment.app.DialogFragment
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellow

class BaseCurrencyDisclaimerDialog(private val popularCurrencies: String) : DialogFragment() {

    var onConfirm: (() -> Unit)? = null

    override fun onCreateDialog(bundle: Bundle?): Dialog {
        val rootView = View.inflate(context, R.layout.fragment_disclaimer, null) as ViewGroup

        rootView.findViewById<TextView>(R.id.text)?.text =
            getString(R.string.SettingsCurrency_DisclaimerText, popularCurrencies)

        val builder =
            activity?.let { AlertDialog.Builder(it, io.horizontalsystems.pin.R.style.AlertDialog) }
        builder?.setView(rootView)
        val mDialog = builder?.create()
        mDialog?.setCanceledOnTouchOutside(true)

        rootView.findViewById<ComposeView>(R.id.buttonConfirmCompose)?.let {
            it.setContent {
                ComposeAppTheme {
                    ButtonPrimaryYellow(
                        modifier = Modifier.padding(
                            start = 16.dp,
                            top = 30.dp,
                            end = 16.dp,
                            bottom = 16.dp
                        ),
                        title = getString(R.string.SettingsCurrency_Understand),
                        onClick = { onConfirm?.invoke() }
                    )
                }
            }
        }

        return mDialog as Dialog
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.findViewById<ComposeView>(R.id.buttonConfirmCompose)?.setViewCompositionStrategy(
            ViewCompositionStrategy.DisposeOnLifecycleDestroyed(viewLifecycleOwner)
        )
    }
}
