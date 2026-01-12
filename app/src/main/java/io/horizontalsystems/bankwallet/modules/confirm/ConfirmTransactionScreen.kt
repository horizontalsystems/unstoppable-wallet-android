package io.horizontalsystems.bankwallet.modules.confirm

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.modules.coin.overview.ui.Loading
import io.horizontalsystems.bankwallet.modules.evmfee.ButtonsGroupWithShade
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.MenuItem
import io.horizontalsystems.bankwallet.ui.compose.components.MenuItemDropdown
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.uiv3.components.HSScaffold

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfirmTransactionScreen(
    title: String = stringResource(R.string.Swap_Confirm_Title),
    initialLoading: Boolean = false,
    onClickBack: (() -> Unit)?,
    onClickFeeSettings: (() -> Unit)?,
    onClickNonceSettings: (() -> Unit)? = null,
    onClickSlippageSettings: (() -> Unit)? = null,
    onClickRecipientSettings: (() -> Unit)? = null,
    defenseSlot: (@Composable ColumnScope.() -> Unit)? = null,
    buttonsSlot: @Composable() (ColumnScope.() -> Unit),
    content: @Composable() (ColumnScope.() -> Unit)
) {
    HSScaffold(
        title = title,
        onBack = onClickBack,
        menuItems = buildList {
            val dropdownMenuItems = buildList {
                onClickFeeSettings?.let {
                    add(
                        MenuItem(
                            title = TranslatableString.ResString(R.string.SendEvmSettings_EditFee),
                            onClick = it
                        )
                    )
                }

                onClickSlippageSettings?.let {
                    add(
                        MenuItem(
                            title = TranslatableString.ResString(R.string.SendEvmSettings_SlippageTolerance),
                            onClick = it
                        )
                    )
                }

                onClickNonceSettings?.let {
                    add(
                        MenuItem(
                            title = TranslatableString.ResString(R.string.SendEvmSettings_Nonce),
                            onClick = it
                        )
                    )
                }

                onClickRecipientSettings?.let {
                    add(
                        MenuItem(
                            title = TranslatableString.ResString(R.string.SendEvmSettings_SetRecipient),
                            onClick = it
                        )
                    )
                }
            }

            if (dropdownMenuItems.isNotEmpty()) {
                add(
                    MenuItemDropdown(
                        title = TranslatableString.ResString(R.string.Settings_Title),
                        icon = R.drawable.manage_24,
                        enabled = !initialLoading,
                        items = dropdownMenuItems
                    )
                )
            }
        },
    ) {
        Crossfade(initialLoading) {
            if (it) {
                Loading()
            } else {
                Box {
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
    }
}