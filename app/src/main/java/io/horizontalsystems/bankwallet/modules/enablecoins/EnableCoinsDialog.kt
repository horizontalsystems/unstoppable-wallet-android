package io.horizontalsystems.bankwallet.modules.enablecoins

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Divider
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
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellow
import io.horizontalsystems.bankwallet.ui.compose.components.TextImportant
import io.horizontalsystems.bankwallet.ui.extensions.BaseComposableBottomSheetFragment
import io.horizontalsystems.bankwallet.ui.extensions.BottomSheetHeader

class EnableCoinsDialog(
    private val listener: Listener,
    private val tokenType: String
) : BaseComposableBottomSheetFragment() {

    interface Listener {
        fun onClickEnable()
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
            iconPainter = painterResource(getIcon(tokenType)),
            title = stringResource(R.string.EnalbeToken_Title),
            subtitle = tokenType,
            onCloseClick = { close() }
        ) {
            Divider(
                modifier = Modifier.fillMaxWidth(),
                thickness = 1.dp,
                color = ComposeAppTheme.colors.steel10
            )
            Box(
                modifier = Modifier.padding(horizontal = 21.dp, vertical = 12.dp)
            ) {
                TextImportant(stringResource(R.string.EnalbeToken_Description, tokenType))
            }
            Divider(
                modifier = Modifier.fillMaxWidth(),
                thickness = 1.dp,
                color = ComposeAppTheme.colors.steel10
            )
            ButtonPrimaryYellow(
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                title = getString(R.string.EnalbeToken_EnableKey),
                onClick = {
                    listener.onClickEnable()
                    dismiss()
                }
            )
        }
    }

    private fun getIcon(tokenType: String): Int {
        return when (tokenType) {
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
