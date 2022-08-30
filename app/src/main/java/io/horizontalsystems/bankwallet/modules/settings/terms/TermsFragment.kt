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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.modules.evmfee.ButtonsGroupWithShade
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.*
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
                    TermsScreen(onCloseClick = { findNavController().popBackStack() })
                }
            }
        }
    }

    @Composable
    private fun TermsScreen(
        onCloseClick: () -> Unit,
        viewModel: TermsViewModel = viewModel(factory = TermsModule.Factory())
    ) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(ComposeAppTheme.colors.tyler)
        ) {
            AppBar(
                title = TranslatableString.ResString(R.string.Settings_Terms),
                menuItems = listOf(
                    MenuItem(
                        title = TranslatableString.ResString(R.string.Button_Close),
                        icon = R.drawable.ic_close,
                        onClick = onCloseClick
                    )
                )
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {

                Spacer(modifier = Modifier.height(12.dp))

                Column(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(12.dp))
                ) {
                    viewModel.termsViewItems.forEach { item ->
                        CellLawrence(
                            borderBottom = true,
                            onClick = { viewModel.onTapTerm(item.termType, !item.checked) }
                        ) {
                            HsCheckbox(
                                checked = item.checked,
                                onCheckedChange = { checked ->
                                    viewModel.onTapTerm(item.termType, checked)
                                },
                            )
                            Spacer(Modifier.width(16.dp))
                            subhead2_leah(text = stringResource(item.termType.description))
                        }
                    }
                }

                Spacer(Modifier.height(60.dp))
            }

            ButtonsGroupWithShade {
                ButtonPrimaryYellow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    title = stringResource(R.string.Button_IAgree),
                    onClick = onCloseClick,
                    enabled = viewModel.buttonEnabled
                )
            }
        }

    }
}
