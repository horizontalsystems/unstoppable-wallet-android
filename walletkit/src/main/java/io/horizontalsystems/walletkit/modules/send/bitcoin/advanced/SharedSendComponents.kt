package io.horizontalsystems.walletkit.modules.send.bitcoin.advanced

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.horizontalsystems.walletkit.R
import io.horizontalsystems.walletkit.core.HSCaution
import io.horizontalsystems.walletkit.ui.compose.components.HsSwitch
import io.horizontalsystems.walletkit.ui.compose.components.RowUniversal
import io.horizontalsystems.walletkit.ui.compose.components.TextImportantError
import io.horizontalsystems.walletkit.ui.compose.components.TextImportantWarning
import io.horizontalsystems.walletkit.ui.compose.components.body_leah

@Composable
fun UtxoSwitch(enabled: Boolean, onChange: (Boolean) -> Unit) {
    RowUniversal(
        modifier = Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = { onChange.invoke(!enabled) }
        ),
    ) {
        body_leah(
            modifier = Modifier.padding(horizontal = 16.dp),
            text = stringResource(R.string.Send_UtxoExpertMode),
        )
        Spacer(modifier = Modifier.weight(1f))
        HsSwitch(
            modifier = Modifier.padding(end = 16.dp),
            checked = enabled,
            onCheckedChange = { onChange.invoke(it) }
        )
        Spacer(modifier = Modifier.width(16.dp))
    }
}


@Composable
fun FeeRateCaution(modifier: Modifier, feeRateCaution: HSCaution) {
    when (feeRateCaution.type) {
        HSCaution.Type.Error -> {
            TextImportantError(
                modifier = modifier,
                icon = R.drawable.ic_attention_20,
                title = feeRateCaution.getString(),
                text = feeRateCaution.getDescription() ?: ""
            )
        }

        HSCaution.Type.Warning -> {
            TextImportantWarning(
                modifier = modifier,
                icon = R.drawable.ic_attention_20,
                title = feeRateCaution.getString(),
                text = feeRateCaution.getDescription() ?: ""
            )
        }
    }
}
