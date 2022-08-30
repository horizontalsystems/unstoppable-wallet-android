package io.horizontalsystems.bankwallet.modules.rateapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryTransparent
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellow
import io.horizontalsystems.bankwallet.ui.compose.components.body_leah
import io.horizontalsystems.bankwallet.ui.compose.components.title3_leah

class RateAppDialogFragment(private var listener: Listener? = null) : DialogFragment() {

    interface Listener {
        fun onClickRateApp()
    }

    override fun getTheme(): Int {
        return R.style.AlertDialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        isCancelable = false
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnLifecycleDestroyed(viewLifecycleOwner)
            )
            setContent {
                RateApp(
                    onRateClick = {
                        listener?.onClickRateApp()
                        dismiss()
                    },
                    onCancelClick = { dismiss() }
                )
            }
        }
    }

    companion object {

        fun show(activity: FragmentActivity, listener: Listener? = null) {
            val fragmentManager: FragmentManager = activity.supportFragmentManager
            RateAppDialogFragment(listener).show(fragmentManager, "RateApp")
        }
    }
}

@Composable
private fun RateApp(
    onRateClick: () -> Unit,
    onCancelClick: () -> Unit,
) {
    ComposeAppTheme {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(color = ComposeAppTheme.colors.lawrence)
                .padding(horizontal = 24.dp, vertical = 20.dp)
        ) {
            title3_leah(text = stringResource(R.string.RateApp_Title_Rate_This_App))
            Spacer(Modifier.height(12.dp))
            body_leah(text = stringResource(R.string.RateApp_Description_Rate_This_App))
            Spacer(Modifier.height(32.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                ButtonPrimaryTransparent(
                    onClick = onCancelClick,
                    title = stringResource(R.string.RateApp_Button_NotNow)
                )

                Spacer(Modifier.width(8.dp))

                ButtonPrimaryYellow(
                    onClick = onRateClick,
                    title = stringResource(R.string.RateApp_Button_RateIt)
                )
            }
        }
    }
}

@Preview
@Composable
private fun Preview_RateApp() {
    RateApp({}, {})
}
