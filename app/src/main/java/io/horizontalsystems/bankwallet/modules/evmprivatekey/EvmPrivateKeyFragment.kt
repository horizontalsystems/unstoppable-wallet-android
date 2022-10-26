package io.horizontalsystems.bankwallet.modules.evmprivatekey

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.modules.evmfee.ButtonsGroupWithShade
import io.horizontalsystems.bankwallet.modules.recoveryphrase.ConfirmCopyBottomSheet
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.*
import io.horizontalsystems.bankwallet.ui.helpers.TextHelper
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.helpers.HudHelper
import kotlinx.coroutines.launch

class EvmPrivateKeyFragment : BaseFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        disallowScreenshot()
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnLifecycleDestroyed(viewLifecycleOwner)
            )
            setContent {
                EvmPrivateKeyScreen(
                    navController = findNavController(),
                    account = arguments?.getParcelable(EvmPrivateKeyModule.ACCOUNT)!!
                )
            }
        }
    }

    override fun onDestroyView() {
        allowScreenshot()
        super.onDestroyView()
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun EvmPrivateKeyScreen(
    navController: NavController,
    account: Account,
) {
    val viewModel = viewModel<EvmPrivateKeyViewModel>(factory = EvmPrivateKeyModule.Factory(account))
    val view = LocalView.current
    val coroutineScope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(
        initialValue = ModalBottomSheetValue.Hidden,
    )

    ComposeAppTheme {
        ModalBottomSheetLayout(
            sheetState = sheetState,
            sheetBackgroundColor = ComposeAppTheme.colors.transparent,
            sheetContent = {
                ConfirmCopyBottomSheet(
                    onConfirm = {
                        coroutineScope.launch {
                            TextHelper.copyText(viewModel.ethereumPrivateKey)
                            HudHelper.showSuccessMessage(view, R.string.Hud_Text_Copied)
                            sheetState.hide()
                        }
                    },
                    onCancel = {
                        coroutineScope.launch {
                            sheetState.hide()
                        }
                    }
                )
            }
        ) {
            Column(modifier = Modifier.background(color = ComposeAppTheme.colors.tyler)) {
                AppBar(
                    title = TranslatableString.ResString(R.string.EvmPrivateKey_Title),
                    navigationIcon = {
                        HsIconButton(onClick = navController::popBackStack) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_back),
                                contentDescription = "back button",
                                tint = ComposeAppTheme.colors.jacob
                            )
                        }
                    },
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.Top
                ) {
                    Spacer(Modifier.height(12.dp))
                    TextImportantWarning(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        text = stringResource(R.string.EvmPrivateKey_Warning)
                    )
                    Spacer(Modifier.height(24.dp))
                    var hidden by remember { mutableStateOf(true) }
                    HidableContent(viewModel.ethereumPrivateKey, hidden, stringResource(R.string.EvmPrivateKey_ShowPrivateKey)) {
                        hidden = it
                    }
                }
                ActionButton(R.string.Alert_Copy) {
                    coroutineScope.launch {
                        sheetState.show()
                    }
                }
            }
        }
    }
}

@Composable
fun ActionButton(title: Int, onClick: () -> Unit) {
    ButtonsGroupWithShade {
        ButtonPrimaryYellow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 32.dp),
            title = stringResource(title),
            onClick = onClick,
        )
    }
}

@Composable
fun HidableContent(
    content: String,
    hidden: Boolean,
    title: String,
    onClickToggle: ((Boolean) -> Unit)?
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(24.dp))
            .border(1.dp, ComposeAppTheme.colors.steel20, RoundedCornerShape(24.dp))
            .clickable(enabled = onClickToggle != null, onClick = { onClickToggle?.invoke(!hidden) })
    ) {

        D2(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 24.dp),
            text = content
        )

        if (hidden) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(ComposeAppTheme.colors.tyler),
                contentAlignment = Alignment.Center
            ) {
                subhead2_grey(title)
            }
        }
    }
}
