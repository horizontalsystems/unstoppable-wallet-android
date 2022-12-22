package io.horizontalsystems.bankwallet.modules.addtoken

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.entities.DataState
import io.horizontalsystems.bankwallet.modules.evmfee.ButtonsGroupWithShade
import io.horizontalsystems.bankwallet.modules.market.ImageSource
import io.horizontalsystems.bankwallet.modules.swap.settings.Caution
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.*
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.helpers.HudHelper
import io.horizontalsystems.snackbar.SnackbarDuration

class AddTokenFragment : BaseFragment() {

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
                AddTokenScreen(findNavController())
            }
        }
    }
}

@Composable
private fun AddTokenScreen(
    navController: NavController,
    viewModel: AddTokenViewModel = viewModel(factory = AddTokenModule.Factory())
) {
    val uiState = viewModel.uiState

    if (uiState.finished) {
        HudHelper.showSuccessMessage(
            LocalView.current, R.string.Hud_Text_Done, SnackbarDuration.LONG
        )
        navController.popBackStack()
    }

    ComposeAppTheme {
        Column(modifier = Modifier.background(color = ComposeAppTheme.colors.tyler)) {
            AppBar(
                title = TranslatableString.ResString(R.string.AddToken_Title),
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
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                Spacer(modifier = Modifier.height(12.dp))

                FormsInput(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    enabled = false,
                    hint = stringResource(R.string.AddToken_AddressOrSymbol),
                    state = getState(uiState.caution, uiState.loading),
                    qrScannerEnabled = true,
                ) {
                    viewModel.onEnterText(it)
                }

                val tokens = uiState.tokens
                val alreadyAddedTokens = uiState.alreadyAddedTokens

                AnimatedVisibility(tokens.isNotEmpty()) {
                    Column {
                        Spacer(modifier = Modifier.height(24.dp))
                        tokens.forEachIndexed { index, token ->
                            if (index != 0) {
                                Spacer(modifier = Modifier.height(12.dp))
                            }
                            TokenCell(
                                title = token.title,
                                subtitle = token.subtitle,
                                badge = token.badge,
                                image = token.image,
                                checked = token.checked,
                                onCheckedChange = { viewModel.onToggleToken(token) }
                            )
                        }
                    }
                }

                AnimatedVisibility(alreadyAddedTokens.isNotEmpty()) {
                    Column {
                        Spacer(modifier = Modifier.height(24.dp))
                        HeaderText(text = stringResource(id = R.string.AddToken_AlreadyAdded))
                        alreadyAddedTokens.forEachIndexed { index, token ->
                            if (index != 0) {
                                Spacer(modifier = Modifier.height(12.dp))
                            }
                            TokenCell(
                                title = token.title,
                                subtitle = token.subtitle,
                                badge = token.badge,
                                image = token.image,
                                alreadyAdded = true,
                            )
                        }
                    }
                }
            }

            ButtonsGroupWithShade {
                ButtonPrimaryYellow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 32.dp),
                    title = uiState.actionButton.title,
                    onClick = { viewModel.onAddClick() },
                    enabled = uiState.actionButton.enabled
                )
            }
        }
    }
}

@Composable
private fun TokenCell(
    title: String,
    subtitle: String,
    badge: String?,
    image: ImageSource,
    checked: Boolean = true,
    alreadyAdded: Boolean = false,
    onCheckedChange: (() -> Unit)? = null,
) {
    CellUniversalLawrenceSection(
        listOf {
            RowUniversal(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalPadding = 0.dp,
                onClick = onCheckedChange?.let {
                    { it.invoke() }
                }
            ) {
                Image(
                    painter = image.painter(),
                    modifier = Modifier
                        .padding(vertical = 12.dp)
                        .size(32.dp),
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(
                    modifier = Modifier.weight(weight = 1f),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        body_leah(
                            text = title,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        badge?.let { badgeText ->
                            Box(
                                modifier = Modifier
                                    .padding(start = 8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(ComposeAppTheme.colors.jeremy)
                            ) {
                                Text(
                                    modifier = Modifier.padding(
                                        start = 4.dp,
                                        end = 4.dp,
                                        bottom = 1.dp
                                    ),
                                    text = badgeText.uppercase(),
                                    color = ComposeAppTheme.colors.bran,
                                    style = ComposeAppTheme.typography.microSB,
                                    maxLines = 1,
                                )
                            }
                        }
                    }
                    subhead2_grey(text = subtitle)
                }
                if (alreadyAdded) {
                    Icon(
                        painter = painterResource(R.drawable.ic_checkmark_20),
                        tint = ComposeAppTheme.colors.grey,
                        contentDescription = null,
                    )
                } else {
                    HsCheckbox(
                        checked = checked,
                        onCheckedChange = { onCheckedChange?.invoke() }
                    )
                }
            }
        }
    )
}

private fun getState(caution: Caution?, loading: Boolean) = when (caution?.type) {
    Caution.Type.Error -> DataState.Error(Exception(caution.text))
    Caution.Type.Warning -> DataState.Error(FormsInputStateWarning(caution.text))
    null -> if (loading) DataState.Loading else null
}

@Preview
@Composable
private fun Preview_TokenCell() {
    ComposeAppTheme {
        Column {
            TokenCell(
                "ACD",
                "Token Name",
                "Bep20",
                ImageSource.Local(R.drawable.bep20),
                true,
                false,
                {})
            Spacer(Modifier.height(12.dp))
            TokenCell(
                "TCD",
                "Token Name",
                "Polygon",
                ImageSource.Local(R.drawable.polygon_erc20),
                false,
                false,
                {})
            Spacer(Modifier.height(12.dp))
            TokenCell(
                "BTCD",
                "Token Name",
                "Erc20",
                ImageSource.Local(R.drawable.erc20),
                alreadyAdded = true
            )
        }
    }
}