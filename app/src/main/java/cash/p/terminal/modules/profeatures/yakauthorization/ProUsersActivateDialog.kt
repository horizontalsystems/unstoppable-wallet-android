package cash.p.terminal.modules.profeatures.yakauthorization

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.navGraphViewModels
import cash.p.terminal.R
import cash.p.terminal.ui.compose.ComposeAppTheme
import cash.p.terminal.ui.compose.components.ButtonPrimaryYellow
import cash.p.terminal.ui.compose.components.subhead2_grey
import cash.p.terminal.ui.extensions.BaseComposableBottomSheetFragment
import cash.p.terminal.ui.extensions.BottomSheetHeader
import io.horizontalsystems.core.findNavController

class ProUsersActivateDialog : BaseComposableBottomSheetFragment() {
    private val authorizationViewModel by navGraphViewModels<YakAuthorizationViewModel>(R.id.coinFragment)

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
                    ProUsersActivateScreen(
                        { findNavController().popBackStack() },
                        { authorizationViewModel.onActivateClick() }
                    )
                }
            }
        }
    }

}

@Composable
private fun ProUsersActivateScreen(
    onCloseClick: () -> Unit,
    onActivateClick: () -> Unit
) {
    BottomSheetHeader(
        iconPainter = painterResource(R.drawable.ic_pro_user),
        title = stringResource(R.string.ProUsersActivate_Title),
        onCloseClick = onCloseClick
    ) {

        Box(
            modifier = Modifier.fillMaxWidth().padding(start = 32.dp, top = 12.dp, end = 32.dp)
        ) {
            subhead2_grey(
                text = stringResource(R.string.ProUsersActivate_Description),
                overflow = TextOverflow.Ellipsis,
            )
        }

        ButtonPrimaryYellow(
            modifier = Modifier
                .padding(horizontal = 24.dp, vertical = 32.dp)
                .fillMaxWidth(),
            title = stringResource(R.string.Hud_Text_Activate),
            onClick = {
                onActivateClick()
                onCloseClick()
            }
        )
    }
}

@Preview
@Composable
private fun ProUsersActivateScreenPreview() {
    ComposeAppTheme {
        ProUsersActivateScreen(
            {}, {}
        )
    }
}
