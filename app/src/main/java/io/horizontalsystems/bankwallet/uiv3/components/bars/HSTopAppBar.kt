package io.horizontalsystems.bankwallet.uiv3.components.bars

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HSTopAppBar(title: String) {
    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = ComposeAppTheme.colors.tyler,
            titleContentColor = ComposeAppTheme.colors.leah,
        ),
        title = {
            Text(
                text = title,
                style = ComposeAppTheme.typography.headline1
            )
        },
        navigationIcon = {
        }
    )
}
