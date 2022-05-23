package io.horizontalsystems.bankwallet.modules.profeatures.yakauthorization

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryDefault
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellow
import io.horizontalsystems.bankwallet.ui.compose.components.HsIconButton
import io.horizontalsystems.bankwallet.ui.compose.components.TextImportantWarning
import io.horizontalsystems.bankwallet.ui.extensions.BaseComposableBottomSheetFragment
import io.horizontalsystems.bankwallet.ui.extensions.BottomSheetHeader
import io.horizontalsystems.core.findNavController

class ProUsersInfoDialog : BaseComposableBottomSheetFragment() {
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
                    ProUsersInfoScreen(
                        findNavController(),
                        listOf(
                            stringResource(R.string.ProUsersInfo_Features_DexVolume),
                            stringResource(R.string.ProUsersInfo_Features_DesLiquidity),
                            stringResource(R.string.ProUsersInfo_Features_ActiveAddresses),
                            stringResource(R.string.ProUsersInfo_Features_TxCount),
                            stringResource(R.string.ProUsersInfo_Features_TxVolume)
                        )
                    )
                }
            }
        }
    }

}

@Composable
private fun ProUsersInfoScreen(navController: NavController, features: List<String>) {
    BottomSheetHeader(
        iconPainter = painterResource(R.drawable.ic_pro_user),
        title = stringResource(R.string.ProUsersInfo_Title),
        subtitle = stringResource(R.string.ProUsersInfo_SubTitle),
        onCloseClick = {
            navController.popBackStack()
        }
    ) {
        Divider(
            modifier = Modifier.fillMaxWidth(),
            thickness = 1.dp,
            color = ComposeAppTheme.colors.steel10
        )

        TextImportantWarning(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            text = stringResource(R.string.ProUsersInfo_Description)
        )
        Divider(
            modifier = Modifier.fillMaxWidth(),
            thickness = 1.dp,
            color = ComposeAppTheme.colors.steel10
        )

        features.forEach { feature ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    modifier = Modifier
                        .padding(start = 16.dp, end = 8.dp)
                        .weight(1f),
                    text = feature,
                    style = ComposeAppTheme.typography.subhead1,
                    color = ComposeAppTheme.colors.grey,
                )
                //IconButton has own padding, that's pushes 16.dp from end
                HsIconButton(
                    onClick = {}
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_checkbox_check),
                        tint = ComposeAppTheme.colors.grey,
                        contentDescription = null,
                    )
                }
            }

            Divider(
                modifier = Modifier.fillMaxWidth(),
                thickness = 1.dp,
                color = ComposeAppTheme.colors.steel10
            )
        }

        ButtonPrimaryYellow(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            title = stringResource(R.string.Hud_Text_LearnMore),
            onClick = {
                navController.popBackStack()
            }
        )

        ButtonPrimaryDefault(
            modifier = Modifier
                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                .fillMaxWidth(),
            title = stringResource(R.string.Button_Cancel),
            onClick = {
                navController.popBackStack()
            }
        )
    }
}
