package io.horizontalsystems.bankwallet.modules.settings.terms

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.AppBar
import io.horizontalsystems.bankwallet.ui.compose.components.CellCheckboxLawrence
import io.horizontalsystems.bankwallet.ui.compose.components.HsCheckbox
import io.horizontalsystems.bankwallet.ui.compose.components.HsIconButton
import io.horizontalsystems.core.findNavController

class TermsFragment : BaseFragment() {

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
                    TermsScreen(
                        findNavController(),
                    )
                }
            }
        }
    }

    @Composable
    private fun TermsScreen(navController: NavController) {
        val viewModel = viewModel<TermsViewModel>(factory = TermsModule.Factory())
        val terms by viewModel.termsLiveData.observeAsState()

        Column(modifier = Modifier.fillMaxSize().background(ComposeAppTheme.colors.tyler)) {
            AppBar(
                title = TranslatableString.ResString(R.string.Settings_Terms),
                navigationIcon = {
                    HsIconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_back),
                            contentDescription = "back",
                            tint = ComposeAppTheme.colors.jacob
                        )
                    }
                }
            )
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = stringResource(R.string.SettingsTerms_Text),
                    style = ComposeAppTheme.typography.body,
                    color = ComposeAppTheme.colors.bran,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )

                Spacer(modifier = Modifier.height(36.dp))

                Column(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(12.dp))
                ) {
                    terms?.forEachIndexed { index, item ->
                        CellCheckboxLawrence(
                            borderBottom = true,
                            onClick = { viewModel.onTapTerm(index, !item.checked) }
                        ) {
                            HsCheckbox(
                                checked = item.checked,
                                onCheckedChange = { checked ->
                                    viewModel.onTapTerm(index, checked)
                                },
                            )
                            Spacer(Modifier.width(16.dp))
                            Text(
                                text = stringResource(item.termType.description),
                                style = ComposeAppTheme.typography.subhead2,
                                color = ComposeAppTheme.colors.leah
                            )
                        }
                    }
                }

                Spacer(Modifier.height(46.dp))

                Text(
                    text = stringResource(R.string.SettingsTerms_BottomThankYou),
                    style = ComposeAppTheme.typography.title3,
                    color = ComposeAppTheme.colors.jacob,
                    modifier = Modifier
                        .padding(horizontal = 24.dp)
                        .align(Alignment.CenterHorizontally)
                )

                Spacer(Modifier.height(32.dp))

                Divider(
                    modifier = Modifier.fillMaxWidth(),
                    thickness = 1.dp,
                    color = ComposeAppTheme.colors.steel10
                )

                Spacer(Modifier.height(12.dp))

                Text(
                    text = stringResource(R.string.FooterText),
                    style = ComposeAppTheme.typography.caption,
                    color = ComposeAppTheme.colors.grey,
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .align(Alignment.CenterHorizontally)
                )

                Spacer(Modifier.height(28.dp))
            }
        }

    }
}
