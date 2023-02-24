package io.horizontalsystems.bankwallet.modules.contacts.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.modules.contacts.Route
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.AppBar
import io.horizontalsystems.bankwallet.ui.compose.components.HsBackButton
import io.horizontalsystems.bankwallet.ui.compose.components.MenuItem

@Composable
fun ContactsScreen(
    navController: NavController,
    navHostController: NavHostController
) {
    ComposeAppTheme {
        Column(modifier = Modifier.background(color = ComposeAppTheme.colors.tyler)) {
            AppBar(
                title = TranslatableString.ResString(R.string.Contacts),
                navigationIcon = {
                    HsBackButton { navController.popBackStack() }
                },
                menuItems = listOf(
                    MenuItem(
                        title = TranslatableString.ResString(R.string.Contacts_Export),
                        icon = R.drawable.icon_search,
                        enabled = false,
                        onClick = { }
                    ),
                    MenuItem(
                        title = TranslatableString.ResString(R.string.Contacts_Export),
                        icon = R.drawable.icon_export,
                        enabled = false,
                        onClick = { }
                    ),
                    MenuItem(
                        title = TranslatableString.ResString(R.string.NewContact),
                        icon = R.drawable.icon_user_plus,
                        tint = ComposeAppTheme.colors.jacob,
                        onClick = { navHostController.navigate(Route.NewContact) }
                    )
                )
            )
        }
    }
}
