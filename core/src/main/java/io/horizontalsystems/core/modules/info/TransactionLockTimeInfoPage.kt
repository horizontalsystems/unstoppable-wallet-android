package io.horizontalsystems.core.modules.info

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.horizontalsystems.core.R
import io.horizontalsystems.core.modules.info.ui.InfoHeader
import io.horizontalsystems.core.modules.nav3.HSNavigation
import io.horizontalsystems.core.modules.nav3.HSPage
import io.horizontalsystems.core.ui.compose.TranslatableString
import io.horizontalsystems.core.ui.compose.components.InfoTextBody
import io.horizontalsystems.core.ui.compose.components.MenuItem
import io.horizontalsystems.core.ui.compose.components.VSpacer
import io.horizontalsystems.core.uiv3.components.HSScaffold
import kotlinx.serialization.Serializable

@Serializable
data class TransactionLockTimeInfoPage(val input: Input) : HSPage() {

    @Composable
    override fun GetContent(navigation: HSNavigation) {
        InfoScreen(input.lockTime, navigation)
    }

    @Serializable
    data class Input(val lockTime: String)
}

@Composable
private fun InfoScreen(
    lockDate: String,
    navigation: HSNavigation
) {

    val description = stringResource(R.string.Info_LockTime_Description, lockDate)

    HSScaffold(
        title = "",
        menuItems = listOf(
            MenuItem(
                title = TranslatableString.ResString(R.string.Button_Close),
                icon = R.drawable.ic_close,
                onClick = { navigation.removeLastOrNull() }
            )
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            InfoHeader(R.string.Info_LockTime_Title)
            InfoTextBody(description)
            VSpacer(20.dp)
        }
    }
}
