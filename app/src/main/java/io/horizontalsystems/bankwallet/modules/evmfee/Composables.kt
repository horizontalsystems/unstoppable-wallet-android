package io.horizontalsystems.bankwallet.modules.evmfee

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Slider
import androidx.compose.material.SliderDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.ethereum.CautionViewItem
import io.horizontalsystems.bankwallet.modules.evmfee.legacy.LegacyFeeSettingsViewModel
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.CellSingleLineLawrenceSection
import io.horizontalsystems.bankwallet.ui.compose.components.TextImportantError
import io.horizontalsystems.bankwallet.ui.compose.components.TextImportantWarning

@Composable
fun LegacyFeeSettings(
    viewModel: LegacyFeeSettingsViewModel,
    onSelectGasPrice: (value: Long) -> Unit
) {
    val feeStatus by viewModel.feeStatusLiveData.observeAsState()
    val sliderViewItem by viewModel.sliderViewItemLiveData.observeAsState()
    val cautions by viewModel.cautionsLiveData.observeAsState()

    val settingsViewItems = mutableListOf<@Composable () -> Unit>()
    var selectedGasPrice by remember { mutableStateOf(1L) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        EvmFeeCell(title = stringResource(R.string.FeeSettings_Fee), feeStatus?.fee)
        Spacer(modifier = Modifier.height(8.dp))

        settingsViewItems.add {
            FeeInfoCell(
                title = stringResource(R.string.FeeSettings_GasLimit),
                value = feeStatus?.gasLimit
            ) {
                //Open Gas Limit info
            }
        }

        sliderViewItem?.let { slider ->
            selectedGasPrice = slider.initialValue

            settingsViewItems.add {
                FeeInfoCell(
                    title = stringResource(R.string.FeeSettings_GasPrice),
                    value = "$selectedGasPrice ${slider.unit}"
                ) {
                    //Open Gas Price info
                }
            }

            settingsViewItems.add {
                HsSlider(
                    value = slider.initialValue,
                    onValueChange = { selectedGasPrice = it },
                    valueRange = slider.range.first..slider.range.last,
                    onValueChangeFinished = { onSelectGasPrice(selectedGasPrice) }
                )
            }
        }

        CellSingleLineLawrenceSection(settingsViewItems)

        cautions?.let {
            Cautions(it)
        }
    }
}

@Composable
fun Cautions(cautions: List<CautionViewItem>) {
    val modifier = Modifier.padding(horizontal = 21.dp, vertical = 12.dp)

    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        cautions.forEach { caution ->

            when (caution.type) {
                CautionViewItem.Type.Error -> {
                    TextImportantError(
                        modifier = modifier,
                        text = caution.text,
                        title = caution.title,
                        icon = R.drawable.ic_attention_20
                    )
                }
                CautionViewItem.Type.Warning -> {
                    TextImportantWarning(
                        modifier = modifier,
                        text = caution.text,
                        title = caution.title,
                        icon = R.drawable.ic_attention_20
                    )
                }
            }
        }
    }
}

@Composable
fun HsSlider(
    value: Long,
    onValueChange: (Long) -> Unit,
    valueRange: ClosedRange<Long>,
    onValueChangeFinished: () -> Unit
) {
    var selectedValue: Float by remember { mutableStateOf(value.toFloat()) }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            modifier = Modifier.clickable {
                if (selectedValue > valueRange.start) {
                    selectedValue--
                    onValueChange(selectedValue.toLong())
                    onValueChangeFinished()
                }
            },
            painter = painterResource(id = R.drawable.ic_minus_20),
            contentDescription = ""
        )
        Slider(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp),
            value = selectedValue,
            onValueChange = {
                selectedValue = it
                onValueChange(selectedValue.toLong())
            },
            valueRange = valueRange.start.toFloat()..valueRange.endInclusive.toFloat(),
            onValueChangeFinished = onValueChangeFinished,
            steps = (valueRange.endInclusive - valueRange.start).toInt(),
            colors = SliderDefaults.colors(
                thumbColor = ComposeAppTheme.colors.grey,
                activeTickColor = ComposeAppTheme.colors.transparent,
                inactiveTickColor = ComposeAppTheme.colors.transparent,
                activeTrackColor = ComposeAppTheme.colors.steel20,
                inactiveTrackColor = ComposeAppTheme.colors.steel20,
                disabledActiveTickColor = ComposeAppTheme.colors.transparent,
                disabledInactiveTrackColor = ComposeAppTheme.colors.steel20
            )
        )
        Image(
            modifier = Modifier.clickable {
                if (selectedValue < valueRange.endInclusive) {
                    selectedValue++
                    onValueChange(selectedValue.toLong())
                    onValueChangeFinished()
                }
            },
            painter = painterResource(id = R.drawable.ic_plus_20),
            contentDescription = ""
        )
    }
}

@Composable
fun FeeInfoCell(title: String, value: String?, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(painter = painterResource(id = R.drawable.ic_info_20), contentDescription = "")
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = title,
            style = ComposeAppTheme.typography.subhead2,
            color = ComposeAppTheme.colors.grey
        )
        Text(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp),
            text = value ?: "",
            style = ComposeAppTheme.typography.subhead1,
            color = ComposeAppTheme.colors.leah,
            textAlign = TextAlign.End,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun EvmFeeCell(title: String, value: String? = null, onClick: (() -> Unit)? = null) {
    CellSingleLineLawrenceSection {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .clickable(enabled = onClick != null, onClick = { onClick?.invoke() })
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = ComposeAppTheme.typography.subhead2,
                color = ComposeAppTheme.colors.grey
            )

            Text(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp),
                text = value ?: "",
                style = ComposeAppTheme.typography.subhead1,
                color = ComposeAppTheme.colors.leah, //leah or oz??
                textAlign = TextAlign.End,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            onClick?.let {
                Image(painter = painterResource(id = R.drawable.ic_edit_20), contentDescription = "")
            }
        }
    }
}
