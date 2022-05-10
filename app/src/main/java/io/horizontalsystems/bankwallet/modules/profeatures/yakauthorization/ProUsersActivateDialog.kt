package io.horizontalsystems.bankwallet.modules.profeatures.yakauthorization

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.navGraphViewModels
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellow
import io.horizontalsystems.bankwallet.ui.extensions.BaseComposableBottomSheetFragment
import io.horizontalsystems.bankwallet.ui.extensions.BottomSheetHeader
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
                    ProUsersActivateScreen(findNavController(), authorizationViewModel)
                }
            }
        }
    }

}

@Composable
private fun ProUsersActivateScreen(navController: NavController, viewModel: YakAuthorizationViewModel) {
    BottomSheetHeader(
        iconPainter = painterResource(R.drawable.ic_pro_user),
        title = stringResource(R.string.ProUsersActivate_Title),
        subtitle = "",
        onCloseClick = {
            navController.popBackStack()
        }
    ) {
        Divider(
            modifier = Modifier.fillMaxWidth(),
            thickness = 1.dp,
            color = ComposeAppTheme.colors.steel10
        )

        Box(
            modifier = Modifier.fillMaxWidth().padding(all = 16.dp)
        ) {
            Text(
                text = stringResource(R.string.ProUsersActivate_Description),
                color = ComposeAppTheme.colors.grey,
                style = ComposeAppTheme.typography.subhead2,
                overflow = TextOverflow.Ellipsis,
            )
        }

        ButtonPrimaryYellow(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            title = stringResource(R.string.Hud_Text_Activate),
            onClick = {
                viewModel.onActivateClick()
                navController.popBackStack()
            }
        )
    }
}
