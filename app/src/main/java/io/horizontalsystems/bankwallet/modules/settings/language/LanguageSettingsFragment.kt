package io.horizontalsystems.bankwallet.modules.settings.language

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.modules.main.MainModule
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.CellUniversalLawrenceSection
import io.horizontalsystems.bankwallet.ui.compose.components.RowUniversal
import io.horizontalsystems.bankwallet.ui.compose.components.headline2_leah
import io.horizontalsystems.bankwallet.ui.compose.components.subhead2_grey
import io.horizontalsystems.bankwallet.uiv3.components.HSScaffold

class LanguageSettingsFragment : BaseComposeFragment() {

    @Composable
    override fun GetContent(navController: NavController) {
        LanguageScreen(
            navController,
            { activity?.let { MainModule.startAsNewTask(it) } }
        )
    }

}

@Composable
private fun LanguageScreen(
    navController: NavController,
    reloadApp: () -> Unit,
    viewModel: LanguageSettingsViewModel = viewModel(
        factory = LanguageSettingsModule.Factory()
    )
) {
    if (viewModel.closeScreen) {
        navController.popBackStack()
    }

    if (viewModel.reloadApp) {
        reloadApp()
    }

    HSScaffold(
        title = stringResource(R.string.Settings_Language),
        onBack = navController::popBackStack,
    ) {
        Column(
            modifier = Modifier
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(Modifier.height(12.dp))
            CellUniversalLawrenceSection(viewModel.languageItems) { item ->
                LanguageCell(
                    title = item.name,
                    subtitle = item.nativeName,
                    icon = item.icon,
                    checked = item.current,
                    onClick = { viewModel.onSelectLocale(item.localeType) }
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun LanguageCell(
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
            headline2_leah(
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
