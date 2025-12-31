package io.horizontalsystems.bankwallet.modules.confirm

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.Scaffold
import androidx.compose.material.ripple
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.modules.evmfee.ButtonsGroupWithShade
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.MenuItem
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.uiv3.components.bars.HSTopAppBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfirmTransactionScreen(
    title: String = stringResource(R.string.Swap_Confirm_Title),
    onClickBack: (() -> Unit)?,
    onClickSettings: (() -> Unit)?,
    onClickClose: (() -> Unit)?,
    onClickNonceSettings: (() -> Unit)? = null,
    defenseSlot: (@Composable ColumnScope.() -> Unit)? = null,
    buttonsSlot: @Composable() (ColumnScope.() -> Unit),
    content: @Composable() (ColumnScope.() -> Unit)
) {
    Scaffold(
        topBar = {
            if (onClickNonceSettings != null && onClickSettings != null) {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = ComposeAppTheme.colors.tyler,
                        titleContentColor = ComposeAppTheme.colors.leah,
                    ),
                    title = {
                        Text(
                            modifier = Modifier.padding(start = 4.dp),
                            text = title,
                            style = ComposeAppTheme.typography.headline1
                        )
                    },
                    actions = {
                        var expanded by remember { mutableStateOf(false) }
                        Box(
                            modifier = Modifier.size(40.dp),
                        ) {
                            Icon(
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .size(24.dp)
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = ripple(
                                            bounded = false,
                                            color = ComposeAppTheme.colors.leah
                                        ),
                                        onClick = {
                                            expanded = true
                                        }
                                    ),
                                painter = painterResource(R.drawable.manage_24),
                                contentDescription = null,
                                tint = Color.Unspecified
                            )
                        }

                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                            shape = RoundedCornerShape(16.dp),
                            containerColor = ComposeAppTheme.colors.lawrence,
                            modifier = Modifier.defaultMinSize(minWidth = 200.dp)
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Text(stringResource(R.string.Settings_Title))
                                       },
                                onClick = {
                                    expanded = false
                                    onClickSettings?.invoke()
                                }
                            )
                            Divider()
                            DropdownMenuItem(
                                text = {
                                    Text(stringResource(R.string.SendEvmSettings_Nonce))
                                },
                                onClick = {
                                    expanded = false
                                    onClickNonceSettings.invoke()
                                }
                            )
                        }
                    }
                )
            } else {
                HSTopAppBar(
                    title = title,
                    menuItems = buildList {
                        onClickSettings?.let {
                            add(
                                MenuItem(
                                    title = TranslatableString.ResString(R.string.Settings_Title),
                                    icon = R.drawable.manage_24,
                                    onClick = onClickSettings
                                )
                            )
                        }
                        onClickClose?.let {
                            add(
                                MenuItem(
                                    title = TranslatableString.ResString(R.string.Button_Close),
                                    icon = R.drawable.ic_close,
                                    onClick = onClickClose
                                )
                            )
                        }
                    },
                    onBack = onClickBack
                )
            }
        },
        backgroundColor = ComposeAppTheme.colors.tyler,
    ) {
        Box() {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                VSpacer(height = 12.dp)

                content.invoke(this)

                VSpacer(height = 362.dp)
            }
            Column(
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {

                defenseSlot?.let {
                    it.invoke(this)
                    VSpacer(height = 16.dp)
                }

                ButtonsGroupWithShade {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        content = buttonsSlot
                    )
                }
            }
        }
    }
}