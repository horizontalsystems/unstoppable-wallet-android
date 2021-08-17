package io.horizontalsystems.bankwallet.modules.enablecoins

import android.os.Bundle
import android.view.View
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellow
import io.horizontalsystems.bankwallet.ui.extensions.BaseBottomSheetDialogFragment
import kotlinx.android.synthetic.main.fragment_enable_coins.*

class EnableCoinsDialog(private val listener: Listener, private val tokenType: String) : BaseBottomSheetDialogFragment() {

    interface Listener {
        fun onClickEnable()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setContentView(R.layout.fragment_enable_coins)

        setTitle(activity?.getString(R.string.EnalbeToken_Title))
        setSubtitle(tokenType)
        alertText.text = getString(R.string.EnalbeToken_Description, tokenType)

        val icon = getIcon(tokenType)

        setHeaderIcon(icon)

        buttonEnableCompose.setViewCompositionStrategy(
            ViewCompositionStrategy.DisposeOnLifecycleDestroyed(viewLifecycleOwner)
        )

        buttonEnableCompose.setContent {
            ComposeAppTheme {
                ButtonPrimaryYellow(
                    modifier = Modifier.padding(
                        start = 16.dp,
                        top = 29.dp,
                        end = 16.dp,
                        bottom = 16.dp
                    ),
                    title = getString(R.string.EnalbeToken_EnableKey),
                    onClick = {
                        listener.onClickEnable()
                        dismiss()
                    }
                )
            }
        }
    }

    private fun getIcon(tokenType: String): Int {
        return when(tokenType){
            "BEP2" -> R.drawable.bep2
            "BEP20" -> R.drawable.bep20
            else -> R.drawable.erc20
        }
    }

    companion object {
        fun show(activity: FragmentActivity, tokenType: String, listener: Listener) {
            val fragment = EnableCoinsDialog(listener, tokenType)
            val transaction = activity.supportFragmentManager.beginTransaction()

            transaction.add(fragment, "bottom_enable_token_dialog")
            transaction.commitAllowingStateLoss()
        }
    }
}