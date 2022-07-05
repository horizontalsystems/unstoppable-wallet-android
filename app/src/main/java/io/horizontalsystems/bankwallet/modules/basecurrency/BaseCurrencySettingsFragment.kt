package io.horizontalsystems.bankwallet.modules.basecurrency

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.*
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.views.helpers.LayoutHelper

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

@Composable
private fun BaseCurrencyScreen(
    navController: NavController,
    viewModel: BaseCurrencySettingsViewModel = viewModel(
        factory = BaseCurrencySettingsModule.Factory()
    )
) {
    val context = LocalContext.current

    if (viewModel.closeScreen) {
        navController.popBackStack()
    }

    ComposeAppTheme {
        if (viewModel.showDisclaimer) {
            DisclaimerDialog(
                viewModel.disclaimerCurrencies,
                { viewModel.onAcceptDisclaimer() },
                { viewModel.closeDisclaimer() }
            )
        }
        Column(
            modifier = Modifier.background(color = ComposeAppTheme.colors.tyler)
        ) {
            AppBar(
                title = TranslatableString.ResString(R.string.SettingsCurrency_Title),
                navigationIcon = {
                    HsIconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_back),
                            contentDescription = "back button",
                            tint = ComposeAppTheme.colors.jacob
                        )
                    }
                }
            )
            Column(
                Modifier.verticalScroll(rememberScrollState())
            ) {
                Spacer(Modifier.height(12.dp))
                CellMultilineLawrenceSection(viewModel.popularItems) { item ->
                    CurrencyCell(
                        item.currency.code,
                        item.currency.symbol,
                        LayoutHelper.getCurrencyDrawableResource(
                            context, item.currency.code.lowercase()
                        ),
                        item.selected,
                        { viewModel.onSelectBaseCurrency(item.currency) }
                    )
                }
                Spacer(Modifier.height(32.dp))
                HeaderText(
                    stringResource(R.string.SettingsCurrency_Other)
                )
                CellMultilineLawrenceSection(viewModel.otherItems) { item ->
                    CurrencyCell(
                        item.currency.code,
                        item.currency.symbol,
                        LayoutHelper.getCurrencyDrawableResource(
                            context, item.currency.code.lowercase()
                        ),
                        item.selected,
                        { viewModel.onSelectBaseCurrency(item.currency) }
                    )
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun DisclaimerDialog(
    currencyCodes: String,
    onAccept: () -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(ComposeAppTheme.colors.lawrence)
        ) {
            Row(
                modifier = Modifier.height(64.dp).fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    painter = painterResource(R.drawable.ic_attention_24),
                    contentDescription = null,
                    tint = ComposeAppTheme.colors.jacob
                )
                headline2_leah(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 48.dp),
                    text = stringResource(R.string.SettingsCurrency_DisclaimerTitle),
                    textAlign = TextAlign.Center
                )
            }
            Divider(
                thickness = 1.dp,
                color = ComposeAppTheme.colors.steel10,
            )
            Spacer(Modifier.height(11.dp))
            TextImportantWarning(
                modifier = Modifier.padding(horizontal = 16.dp),
                text = stringResource(R.string.SettingsCurrency_DisclaimerText, currencyCodes)
            )
            ButtonPrimaryYellow(
                modifier = Modifier
                    .padding(
                        start = 16.dp,
                        top = 28.dp,
                        end = 16.dp,
                        bottom = 16.dp
                    )
                    .fillMaxWidth(),
                title = stringResource(R.string.SettingsCurrency_Understand),
                onClick = onAccept
            )
        }
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
    Row(
        modifier = Modifier
            .fillMaxSize()
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            modifier = Modifier.padding(horizontal = 16.dp),
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
