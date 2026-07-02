package io.horizontalsystems.walletkit.modules.walletconnect.session.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.horizontalsystems.walletkit.R
import io.horizontalsystems.walletkit.modules.nav3.HSNavigation
import io.horizontalsystems.walletkit.ui.compose.components.ButtonPrimaryDefault
import io.horizontalsystems.walletkit.ui.compose.components.ListEmptyView

@Composable
fun WCSessionError(
    error: String,
    navigation: HSNavigation
) {
    Box(Modifier.fillMaxSize()) {
        ListEmptyView(text = error, icon = R.drawable.ic_stop)
        ButtonPrimaryDefault(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 32.dp)
                .align(Alignment.BottomCenter),
            title = stringResource(R.string.Button_Close),
            onClick = { navigation.removeLastOrNull() }
        )
    }
}
