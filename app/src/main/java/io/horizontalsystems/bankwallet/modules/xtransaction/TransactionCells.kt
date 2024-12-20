package io.horizontalsystems.bankwallet.modules.xtransaction

import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.slideFromRight
import io.horizontalsystems.bankwallet.modules.contacts.ContactsFragment
import io.horizontalsystems.bankwallet.modules.contacts.ContactsModule
import io.horizontalsystems.bankwallet.modules.contacts.Mode
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonSecondaryCircle
import io.horizontalsystems.bankwallet.ui.compose.components.HSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.SelectorDialogCompose
import io.horizontalsystems.bankwallet.ui.compose.components.SelectorItem
import io.horizontalsystems.bankwallet.ui.compose.components.body_leah
import io.horizontalsystems.bankwallet.ui.compose.components.cell.CellUniversal
import io.horizontalsystems.bankwallet.ui.compose.components.subhead1_grey
import io.horizontalsystems.bankwallet.ui.compose.components.subhead1_leah
import io.horizontalsystems.bankwallet.ui.compose.components.subhead2_grey
import io.horizontalsystems.bankwallet.ui.helpers.TextHelper
import io.horizontalsystems.core.helpers.HudHelper
import io.horizontalsystems.marketkit.models.BlockchainType
import java.math.BigDecimal

@Composable
fun XxxSectionHeaderCell(
    title: String,
    value: String,
    borderTop: Boolean = true,
    painter: Painter?
) {
    CellUniversal(borderTop = borderTop) {
        painter?.let {
            Icon(
                modifier = Modifier.padding(end = 16.dp),
                painter = painter,
                tint = ComposeAppTheme.colors.grey,
                contentDescription = null,
            )
        }

        body_leah(text = title)

        subhead1_grey(
            modifier = Modifier
                .weight(1f)
                .padding(start = 8.dp),
            text = value,
            textAlign = TextAlign.End,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun XxxTitleAndValueCell(
    title: String,
    value: String,
    borderTop: Boolean = true
) {
    CellUniversal(borderTop = borderTop) {
        subhead2_grey(text = title, modifier = Modifier.padding(end = 16.dp))
        HSpacer(16.dp)
        subhead1_leah(
            modifier = Modifier.weight(1f),
            text = value,
            textAlign = TextAlign.Right
        )
    }
}

@Composable
fun XxxAddress(
    title: String,
    value: String,
    showAdd: Boolean,
    blockchainType: BlockchainType?,
    navController: NavController? = null,
    onCopy: (() -> Unit)? = null,
    onAddToExisting: (() -> Unit)? = null,
    onAddToNew: (() -> Unit)? = null,
    borderTop: Boolean = true
) {
    val view = LocalView.current
    var showSaveAddressDialog by remember { mutableStateOf(false) }
    CellUniversal(borderTop = borderTop) {
        subhead2_grey(text = title)

        HSpacer(16.dp)
        subhead1_leah(
            modifier = Modifier.weight(1f),
            text = value,
            textAlign = TextAlign.Right
        )

        if (showAdd) {
            HSpacer(16.dp)
            ButtonSecondaryCircle(
                icon = R.drawable.icon_20_user_plus,
                onClick = { showSaveAddressDialog = true }
            )
        }

        HSpacer(16.dp)
        ButtonSecondaryCircle(
            icon = R.drawable.ic_copy_20,
            onClick = {
                TextHelper.copyText(value)
                HudHelper.showSuccessMessage(view, R.string.Hud_Text_Copied)

                onCopy?.invoke()
            }
        )
    }

    if (showSaveAddressDialog) {
        SelectorDialogCompose(
            title = stringResource(R.string.Contacts_AddAddress),
            items = ContactsModule.AddAddressAction.entries.map {
                SelectorItem(stringResource(it.title), false, it)
            },
            onDismissRequest = {
                showSaveAddressDialog = false
            },
            onSelectItem = { action ->
                blockchainType?.let {
                    val args = when (action) {
                        ContactsModule.AddAddressAction.AddToNewContact -> {
                            onAddToNew?.invoke()
                            ContactsFragment.Input(
                                Mode.AddAddressToNewContact(
                                    blockchainType,
                                    value
                                )
                            )
                        }

                        ContactsModule.AddAddressAction.AddToExistingContact -> {
                            onAddToExisting?.invoke()
                            ContactsFragment.Input(
                                Mode.AddAddressToExistingContact(
                                    blockchainType,
                                    value
                                )
                            )
                        }
                    }
                    navController?.slideFromRight(R.id.contactsFragment, args)
                }
            })
    }
}

@Composable
fun xxxCoinAmount(value: BigDecimal?, coinCode: String, sign: String): String {
//    if (hideAmount) return "*****"
    if (value == null) return "---"

    return sign + App.numberFormatter.formatCoinFull(value, coinCode, 8)
}

@Composable
fun xxxFiatAmount(value: BigDecimal?, fiatSymbol: String): String {
//    if (hideAmount) return "*****"
    if (value == null) return "---"

    return App.numberFormatter.formatFiatFull(value, fiatSymbol)
}