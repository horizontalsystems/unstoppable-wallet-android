package io.horizontalsystems.bankwallet.modules.balance.views

import android.os.Bundle
import android.view.View
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryDefault
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellow
import io.horizontalsystems.bankwallet.ui.extensions.BaseBottomSheetDialogFragment
import kotlinx.android.synthetic.main.fragment_bottom_sync_error.*

class SyncErrorDialog(
    private val listener: Listener,
    private val coinName: String,
    private val sourceChangeable: Boolean
) : BaseBottomSheetDialogFragment() {

    interface Listener {
        fun onClickRetry()
        fun onClickChangeSource()
        fun onClickReport()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setContentView(R.layout.fragment_bottom_sync_error)

        setTitle(activity?.getString(R.string.BalanceSyncError_Title))
        setSubtitle(coinName)
        setHeaderIcon(R.drawable.ic_attention_red_24)

        buttonsCompose.setViewCompositionStrategy(
            ViewCompositionStrategy.DisposeOnLifecycleDestroyed(viewLifecycleOwner)
        )

        setButtons()
    }

    private fun setButtons() {
        buttonsCompose.setContent {
            ComposeAppTheme {
                Column(modifier = Modifier.width(IntrinsicSize.Max)) {
                    ButtonPrimaryYellow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, top = 16.dp, end = 16.dp),
                        title = getString(R.string.BalanceSyncError_ButtonRetry),
                        onClick = {
                            listener.onClickRetry()
                            dismiss()
                        }
                    )
                    if (sourceChangeable) {
                        ButtonPrimaryDefault(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp, top = 16.dp, end = 16.dp),
                            title = getString(R.string.BalanceSyncError_ButtonChangeSource),
                            onClick = {
                                listener.onClickChangeSource()
                                dismiss()
                            }
                        )
                    }
                    ButtonPrimaryDefault(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, top = 16.dp, end = 16.dp),
                        title = getString(R.string.BalanceSyncError_ButtonReport),
                        onClick = {
                            listener.onClickReport()
                            dismiss()
                        }
                    )
                }
            }
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
