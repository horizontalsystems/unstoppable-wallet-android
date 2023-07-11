package cash.p.terminal.modules.coin.indicators

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import androidx.navigation.fragment.findNavController
import cash.p.terminal.R
import cash.p.terminal.core.BaseFragment
import cash.p.terminal.core.slideFromRight
import cash.p.terminal.ui.compose.ComposeAppTheme
import cash.p.terminal.ui.compose.TranslatableString
import cash.p.terminal.ui.compose.components.AppBar
import cash.p.terminal.ui.compose.components.CellUniversalLawrenceSection
import cash.p.terminal.ui.compose.components.HSpacer
import cash.p.terminal.ui.compose.components.HeaderText
import cash.p.terminal.ui.compose.components.HsBackButton
import cash.p.terminal.ui.compose.components.HsIconButton
import cash.p.terminal.ui.compose.components.HsSwitch
import cash.p.terminal.ui.compose.components.RowUniversal
import cash.p.terminal.ui.compose.components.TextImportantError
import cash.p.terminal.ui.compose.components.VSpacer
import cash.p.terminal.ui.compose.components.body_leah

class IndicatorsFragment : BaseFragment() {

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
                    Indicators(
                        navController = findNavController(),
                    )
                }
            }
        }
    }
}

@Composable
fun Indicators(navController: NavController) {
    var showDataError by remember { mutableStateOf(true) }

    Scaffold(
        backgroundColor = ComposeAppTheme.colors.tyler,
        topBar = {
            AppBar(
                title = TranslatableString.ResString(R.string.CoinPage_Indicators),
                navigationIcon = {
                    HsBackButton(onClick = { navController.popBackStack() })
                }
            )
        }
    ) {
        Column(Modifier.padding(it)) {
            HeaderText(
                stringResource(R.string.CoinPage_MovingAverages).uppercase()
            )
            CellUniversalLawrenceSection(
                listOf(
                    {
                        IndicatorCell(
                            title = "EMA 1",
                            checked = true,
                            leftIcon = {
                                Icon(
                                    painter = painterResource(R.drawable.ic_chart_type_2_24),
                                    tint = ComposeAppTheme.colors.jacob,
                                    contentDescription = null,
                                )
                            },
                            onEditClick = {
                                navController.slideFromRight(R.id.emaSettingsFragment)
                            }
                        )
                    },
                    {
                        IndicatorCell(
                            title = "EMA 2",
                            checked = true,
                            leftIcon = {
                                Icon(
                                    painter = painterResource(R.drawable.ic_chart_type_2_24),
                                    tint = ComposeAppTheme.colors.laguna,
                                    contentDescription = null,
                                )
                            },
                            onEditClick = {
                                navController.slideFromRight(R.id.emaSettingsFragment)
                            }
                        )
                    },
                    {
                        IndicatorCell(
                            title = "EMA 3",
                            checked = true,
                            leftIcon = {
                                Icon(
                                    painter = painterResource(R.drawable.ic_chart_type_2_24),
                                    tint = ComposeAppTheme.colors.purple,
                                    contentDescription = null,
                                )
                            },
                            onEditClick = {
                                navController.slideFromRight(R.id.emaSettingsFragment)
                            }
                        )
                    }
                )
            )
            if (showDataError) {
                VSpacer(12.dp)
                TextImportantError(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    title = stringResource(R.string.CoinPage_InsufficientData),
                    text = stringResource(R.string.CoinPage_InsufficientDataError),
                    icon = R.drawable.ic_attention_20
                )
            }
            VSpacer(24.dp)
            HeaderText(
                stringResource(R.string.CoinPage_OscillatorsSettings).uppercase()
            )
            CellUniversalLawrenceSection(
                listOf(
                    {
                        IndicatorCell(
                            title = "RSI",
                            checked = true,
                            onEditClick = {
                                navController.slideFromRight(R.id.rsiSettingsFragment)
                            }
                        )
                    },
                    {
                        IndicatorCell(
                            title = "MACD",
                            checked = true,
                            onEditClick = {
                                navController.slideFromRight(R.id.macdSettingsFragment)
                            }
                        )
                    }
                )
            )
        }
    }
}

@Composable
private fun IndicatorCell(
    title: String,
    checked: Boolean,
    leftIcon: (@Composable () -> Unit)? = null,
    onEditClick: () -> Unit
) {
    RowUniversal(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        leftIcon?.invoke()
        HSpacer(16.dp)
        body_leah(
            text = title,
            modifier = Modifier.weight(1f)
        )
        HsIconButton(
            modifier = Modifier.size(20.dp),
            onClick = onEditClick
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_edit_20),
                contentDescription = null,
                tint = ComposeAppTheme.colors.grey
            )
        }
        HSpacer(16.dp)
        HsSwitch(
            modifier = Modifier.padding(0.dp),
            checked = checked,
            onCheckedChange = {}
        )
    }
}

@Preview
@Composable
private fun Preview_Indicators() {
    val navController = rememberNavController()
    ComposeAppTheme {
        Indicators(navController)
    }
}
