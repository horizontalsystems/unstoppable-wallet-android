package io.horizontalsystems.bankwallet.modules.basecurrency

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.*
import io.horizontalsystems.bankwallet.ui.extensions.BottomSheetHeader
import io.horizontalsystems.core.findNavController
import kotlinx.coroutines.launch

class BaseCurrencySettingsFragment : BaseFragment() {

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
                BaseCurrencyScreen(findNavController())
            }
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun BaseCurrencyScreen(
    navController: NavController,
    viewModel: BaseCurrencySettingsViewModel = viewModel(
        factory = BaseCurrencySettingsModule.Factory()
    )
) {
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(
        ModalBottomSheetValue.Hidden,
        confirmStateChange = {
            if (it == ModalBottomSheetValue.Hidden) {
                viewModel.closeDisclaimer()
            }
            true
        }
    )

    if (viewModel.closeScreen) {
        navController.popBackStack()
    }

    if (viewModel.showDisclaimer) {
        LaunchedEffect(Unit) {
            sheetState.show()
        }
    }

    ComposeAppTheme {
        ModalBottomSheetLayout(
            sheetState = sheetState,
            sheetBackgroundColor = ComposeAppTheme.colors.transparent,
            sheetContent = {
                WarningBottomSheet(
                    text = stringResource(
                        R.string.SettingsCurrency_DisclaimerText,
                        viewModel.disclaimerCurrencies
                    ),
                    onCloseClick = {
                        viewModel.closeDisclaimer()
                        scope.launch { sheetState.hide() }
                    },
                    onOkClick = {
                        viewModel.onAcceptDisclaimer()
                        scope.launch { sheetState.hide() }
                    }
                )
            }
        ) {
            Column(
                modifier = Modifier.background(color = ComposeAppTheme.colors.tyler)
            ) {
                AppBar(
                    title = TranslatableString.ResString(R.string.SettingsCurrency_Title),
                    navigationIcon = {
                        HsBackButton(onClick = { navController.popBackStack() })
                    }
                )
                Column(
                    Modifier.verticalScroll(rememberScrollState())
                ) {
                    Spacer(Modifier.height(12.dp))
                    CellUniversalLawrenceSection(viewModel.popularItems) { item ->
                        CurrencyCell(
                            item.currency.code,
                            item.currency.symbol,
                            item.currency.flag,
                            item.selected,
                            { viewModel.onSelectBaseCurrency(item.currency) }
                        )
                    }
                    Spacer(Modifier.height(24.dp))
                    HeaderText(
                        stringResource(R.string.SettingsCurrency_Other)
                    )
                    CellUniversalLawrenceSection(viewModel.otherItems) { item ->
                        CurrencyCell(
                            item.currency.code,
                            item.currency.symbol,
                            item.currency.flag,
                            item.selected,
                            { viewModel.onSelectBaseCurrency(item.currency) }
                        )
                    }
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
private fun WarningBottomSheet(
    text: String,
    onCloseClick: () -> Unit,
    onOkClick: () -> Unit
) {
    BottomSheetHeader(
        iconPainter = painterResource(R.drawable.ic_attention_24),
        title = stringResource(R.string.SettingsCurrency_DisclaimerTitle),
        iconTint = ColorFilter.tint(ComposeAppTheme.colors.jacob),
        onCloseClick = onCloseClick
    ) {
        TextImportantWarning(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            text = text
        )

        ButtonPrimaryYellow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 24.dp, top = 20.dp),
            title = stringResource(id = R.string.Button_Change),
            onClick = onOkClick
        )

        ButtonPrimaryTransparent(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            title = stringResource(id = R.string.Button_Cancel),
            onClick = onCloseClick
        )
        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
private fun CurrencyCell(
    title: String,
    subtitle: String,
    icon: Int,
    checked: Boolean,
    onClick: () -> Unit
) {
    RowUniversal(
        onClick = onClick
    ) {
        Image(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .size(32.dp),
            painter = painterResource(icon),
            contentDescription = null
        )
        Column(modifier = Modifier.weight(1f)) {
            body_leah(
                text = title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(1.dp))
            subhead2_grey(
                text = subtitle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Box(
            modifier = Modifier
                .width(52.dp)
                .fillMaxHeight(),
            contentAlignment = Alignment.Center
        ) {
            if (checked) {
                Icon(
                    painter = painterResource(R.drawable.ic_checkmark_20),
                    tint = ComposeAppTheme.colors.jacob,
                    contentDescription = null,
                )
            }
        }
    }
}
