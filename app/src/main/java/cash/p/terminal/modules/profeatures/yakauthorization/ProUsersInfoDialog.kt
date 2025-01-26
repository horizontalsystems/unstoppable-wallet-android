package cash.p.terminal.modules.profeatures.yakauthorization

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import cash.p.terminal.R
import cash.p.terminal.ui_compose.theme.ComposeAppTheme
import cash.p.terminal.ui_compose.components.ButtonPrimaryYellow
import cash.p.terminal.ui_compose.components.CellUniversalLawrenceSection
import cash.p.terminal.ui_compose.components.HsIconButton
import cash.p.terminal.ui_compose.components.RowUniversal
import cash.p.terminal.ui_compose.components.TextImportantWarning
import cash.p.terminal.ui_compose.components.subhead2_leah
import cash.p.terminal.ui.extensions.BaseComposableBottomSheetFragment
import cash.p.terminal.ui.extensions.BottomSheetHeader
import cash.p.terminal.ui_compose.findNavController

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
                cash.p.terminal.ui_compose.theme.ComposeAppTheme {
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
        iconPainter = painterResource(R.drawable.icon_24_lock),
        iconTint = ColorFilter.tint(cash.p.terminal.ui_compose.theme.ComposeAppTheme.colors.jacob),
        title = stringResource(R.string.ProUsersInfo_UnstoppablePass),
        onCloseClick = {
            navController.popBackStack()
        }
    ) {

        TextImportantWarning(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            text = stringResource(R.string.ProUsersInfo_Description)
        )

        CellUniversalLawrenceSection(features, showFrame = true) { feature ->
            RowUniversal {
                subhead2_leah(
                    modifier = Modifier
                        .padding(start = 16.dp, end = 8.dp)
                        .weight(1f),
                    text = feature,
                )
                //IconButton has own padding, that's pushes 16.dp from end
                HsIconButton(
                    onClick = {}
                ) {
                    Icon(
                        painter = painterResource(R.drawable.checkbox_active_24),
                        tint = ComposeAppTheme.colors.grey,
                        contentDescription = null,
                    )
                }
            }
        }

        Spacer(Modifier.height(44.dp))
        ButtonPrimaryYellow(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .fillMaxWidth(),
            title = stringResource(R.string.Hud_Text_LearnMore),
            onClick = {
                navController.popBackStack()
            },
            enabled = false
        )

        Spacer(Modifier.height(32.dp))
    }
}
