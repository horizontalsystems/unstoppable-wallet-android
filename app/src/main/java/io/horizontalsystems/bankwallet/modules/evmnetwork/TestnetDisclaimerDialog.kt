package io.horizontalsystems.bankwallet.modules.evmnetwork

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
import androidx.fragment.app.DialogFragment
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.databinding.FragmentDisclaimerBinding
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellow

class TestnetDisclaimerDialog : DialogFragment() {

    var onConfirm: (() -> Unit)? = null

    private var _binding: FragmentDisclaimerBinding? = null
    private val binding get() = _binding!!

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onCreateDialog(bundle: Bundle?): Dialog {
        _binding = FragmentDisclaimerBinding.inflate(LayoutInflater.from(context))

        binding.text.text = getString(R.string.NetworkSettings_TestNetDisclaimer)

        val builder = activity?.let {
            AlertDialog.Builder(
                it,
                io.horizontalsystems.pin.R.style.AlertDialog
            )
        }
        builder?.setView(binding.root)
        val mDialog = builder?.create()
        mDialog?.setCanceledOnTouchOutside(true)

        binding.buttonConfirmCompose.setContent {
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

        return mDialog as Dialog
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.buttonConfirmCompose.setViewCompositionStrategy(
            ViewCompositionStrategy.DisposeOnLifecycleDestroyed(viewLifecycleOwner)
        )
    }
}
