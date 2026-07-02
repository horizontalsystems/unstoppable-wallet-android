package io.horizontalsystems.core.modules.info

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.horizontalsystems.core.R
import io.horizontalsystems.core.modules.info.ui.InfoBody
import io.horizontalsystems.core.modules.info.ui.InfoHeader
import io.horizontalsystems.core.modules.info.ui.InfoSubHeader
import io.horizontalsystems.core.modules.nav3.HSNavigation
import io.horizontalsystems.core.modules.nav3.HSPage
import io.horizontalsystems.core.ui.compose.TranslatableString
import io.horizontalsystems.core.ui.compose.components.MenuItem
import io.horizontalsystems.core.ui.compose.components.VSpacer
import io.horizontalsystems.core.uiv3.components.HSScaffold
import kotlinx.serialization.Serializable

@Serializable
data object FeePriorityInfoPage : HSPage() {

    @Composable
    override fun GetContent(navigation: HSNavigation) {
        InfoScreen(
            navigation
        )
    }

}

@Composable
private fun InfoScreen(
    navigation: HSNavigation
) {
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
            InfoHeader(R.string.FeeInfo_Title)
            InfoBody(R.string.FeeInfo_Description)
            InfoSubHeader(R.string.FeeInfo_Slow)
            InfoBody(R.string.FeeInfo_SlowDescription)
            InfoSubHeader(R.string.FeeInfo_Average)
            InfoBody(R.string.FeeInfo_AverageDescription)
            InfoSubHeader(R.string.FeeInfo_Fast)
            InfoBody(R.string.FeeInfo_FastDescription)
            VSpacer(20.dp)
        }
    }
}
