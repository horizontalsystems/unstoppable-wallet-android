package io.horizontalsystems.bankwallet.modules.balance.views

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryDefault
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellow
import io.horizontalsystems.bankwallet.ui.extensions.BaseComposableBottomSheetFragment
import io.horizontalsystems.bankwallet.ui.extensions.BottomSheetHeader

class SyncErrorDialog(
    private val listener: Listener,
    private val coinName: String,
    private val sourceChangeable: Boolean
) : BaseComposableBottomSheetFragment() {

    interface Listener {
        fun onClickRetry()
        fun onClickChangeSource()
        fun onClickReport()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnLifecycleDestroyed(viewLifecycleOwner)
            )
            setContent {
                ComposeAppTheme {
                    BottomSheetScreen()
                }
            }
        }
    }

    @Composable
    private fun BottomSheetScreen() {
        BottomSheetHeader(
            iconPainter = painterResource(R.drawable.ic_attention_red_24),
            title = stringResource(R.string.BalanceSyncError_Title),
            subtitle = coinName,
            onCloseClick = { close() }
        ) {
            Divider(
                modifier = Modifier.fillMaxWidth(),
                thickness = 1.dp,
                color = ComposeAppTheme.colors.steel10
            )
            Text(
                text = stringResource(R.string.BalanceSyncError_ReportButtonExplanation),
                style = ComposeAppTheme.typography.subhead2,
                color = ComposeAppTheme.colors.grey,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
            )
            Divider(
                modifier = Modifier.fillMaxWidth(),
                thickness = 1.dp,
                color = ComposeAppTheme.colors.steel10
            )
            Spacer(Modifier.height(16.dp))
            ButtonPrimaryYellow(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                title = getString(R.string.BalanceSyncError_ButtonRetry),
                onClick = {
                    listener.onClickRetry()
                    dismiss()
                }
            )
            if (sourceChangeable) {
                Spacer(Modifier.height(16.dp))
                ButtonPrimaryDefault(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    title = getString(R.string.BalanceSyncError_ButtonChangeSource),
                    onClick = {
                        listener.onClickChangeSource()
                        dismiss()
                    }
                )
            }
            Spacer(Modifier.height(16.dp))
            ButtonPrimaryDefault(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                title = getString(R.string.BalanceSyncError_ButtonReport),
                onClick = {
                    listener.onClickReport()
                    dismiss()
                }
            )
            Spacer(Modifier.height(16.dp))
        }
    }

    companion object {
        fun show(
            activity: FragmentActivity,
            coinName: String,
            sourceChangeable: Boolean,
            listener: Listener
        ) {
            val fragment = SyncErrorDialog(listener, coinName, sourceChangeable)
            val transaction = activity.supportFragmentManager.beginTransaction()

            transaction.add(fragment, "bottom_sync_error")
            transaction.commitAllowingStateLoss()
        }
    }
}
