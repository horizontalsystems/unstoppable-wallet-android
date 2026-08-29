package io.horizontalsystems.walletkit.modules.market.earn

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.horizontalsystems.walletkit.R
import io.horizontalsystems.walletkit.modules.nav3.HSNavigation
import io.horizontalsystems.walletkit.modules.nav3.HSPage
import io.horizontalsystems.walletkit.modules.nav3.LocalResultEventBus
import io.horizontalsystems.walletkit.ui.compose.ComposeAppTheme
import io.horizontalsystems.walletkit.ui.compose.TranslatableString
import io.horizontalsystems.walletkit.ui.compose.components.MenuItem
import io.horizontalsystems.walletkit.ui.compose.components.VSpacer
import io.horizontalsystems.walletkit.ui.compose.components.body_grey
import io.horizontalsystems.walletkit.ui.compose.components.cell.CellBlockchainChecked
import io.horizontalsystems.walletkit.ui.compose.components.cell.CellUniversal
import io.horizontalsystems.walletkit.ui.compose.components.cell.SectionUniversalLawrence
import io.horizontalsystems.walletkit.uiv3.components.HSScaffold
import io.horizontalsystems.marketkit.models.Blockchain
import kotlinx.serialization.Serializable

@Serializable
data class VaultBlockchainsSelectorPage(val input: Input) : HSPage(accessibleWhileLocked = true) {

    @Composable
    override fun GetContent(navigation: HSNavigation) {
        val resultEventBus = LocalResultEventBus.current
        FilterByBlockchainsScreen(
            input.allBlockchains,
            input.selected,
            navigation = navigation,
            onDone = { selected ->
                resultEventBus.sendResult(Result(selected))
                navigation.removeLastOrNull()
            },
        )
    }

    @Serializable
    data class Input(val selected: List<Blockchain>, val allBlockchains: List<Blockchain>)

    data class Result(val selected: List<Blockchain>)
}

@Composable
private fun FilterByBlockchainsScreen(
    blockchains: List<Blockchain>,
    selected: List<Blockchain>,
    navigation: HSNavigation,
    onDone: (List<Blockchain>) -> Unit,
) {
    var selectedBlockchains = remember { mutableStateListOf<Blockchain>() }

    LaunchedEffect(selected) {
        selectedBlockchains.clear()
        selectedBlockchains.addAll(selected)
    }

    BackHandler {
        onDone.invoke(selectedBlockchains)
    }

    HSScaffold(
        title = stringResource(R.string.Market_Filter_Blockchains),
        onBack = { onDone.invoke(selectedBlockchains) },
        menuItems = listOf(
            MenuItem(
                title = TranslatableString.ResString(R.string.Button_Reset),
                enabled = selectedBlockchains.isNotEmpty(),
                onClick = {
                    onDone.invoke(emptyList())
                }
            )
        ),
    ) {
        Column {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                VSpacer(height = 12.dp)

                SectionUniversalLawrence {
                    AnyCell(
                        checked = selectedBlockchains.isEmpty(),
                        onClick = { selectedBlockchains = mutableStateListOf() }
                    )
                    blockchains.forEach { item ->
                        CellBlockchainChecked(
                            blockchain = item,
                            checked = item in selectedBlockchains,
                        ) {
                            if (item in selectedBlockchains) {
                                selectedBlockchains.remove(item)
                            } else {
                                selectedBlockchains.add(item)
                            }
                        }
                    }
                }

                VSpacer(height = 32.dp)
            }
        }
    }
}

@Composable
private fun AnyCell(
    checked: Boolean,
    onClick: () -> Unit
) {
    CellUniversal(
        borderTop = false,
        onClick = onClick
    ) {
        body_grey(
            modifier = Modifier
                .padding(end = 16.dp)
                .weight(1f),
            text = stringResource(R.string.Any),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Icon(
            painter = painterResource(R.drawable.ic_checkmark_20),
            tint = ComposeAppTheme.colors.jacob,
            contentDescription = null,
            modifier = Modifier.alpha(if (checked) 1f else 0f)
        )
    }
}
