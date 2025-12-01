package io.horizontalsystems.bankwallet.uiv3.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.IMenuItem
import io.horizontalsystems.bankwallet.uiv3.components.bars.HSTopAppBar

@Composable
fun HSScaffold(
    title: String,
    menuItems: List<IMenuItem> = listOf(),
    onBack: (() -> Unit)? = null,
    bottomBar: @Composable () -> Unit = {},
    content: @Composable BoxScope.() -> Unit
) {
    Scaffold(
        topBar = {
            HSTopAppBar(title, menuItems, onBack)
        },
        bottomBar = bottomBar,
        backgroundColor = ComposeAppTheme.colors.tyler
    ) {
        Box(
            modifier = Modifier.padding(it).fillMaxSize(),
            content = content
        )
    }
}
