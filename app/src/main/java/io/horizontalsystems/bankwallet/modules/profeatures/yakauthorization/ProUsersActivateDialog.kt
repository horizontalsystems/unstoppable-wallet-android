package io.horizontalsystems.bankwallet.modules.profeatures.yakauthorization

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavBackStack
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.modules.coin.CoinFragment
import io.horizontalsystems.bankwallet.modules.nav3.HSScreen
import io.horizontalsystems.bankwallet.modules.nav3.viewModelForScreen
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellow
import io.horizontalsystems.bankwallet.ui.compose.components.subhead2_grey
import io.horizontalsystems.bankwallet.ui.extensions.BaseComposableBottomSheetFragment
import io.horizontalsystems.bankwallet.ui.extensions.BottomSheetHeader

class ProUsersActivateDialog : BaseComposableBottomSheetFragment() {
    @Composable
    override fun GetContent(navController: NavBackStack<HSScreen>) {
        val authorizationViewModel  = navController.viewModelForScreen<YakAuthorizationViewModel>(CoinFragment::class)
        ProUsersActivateScreen(
            { navController.removeLastOrNull() },
            { authorizationViewModel.onActivateClick() }
        )
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
