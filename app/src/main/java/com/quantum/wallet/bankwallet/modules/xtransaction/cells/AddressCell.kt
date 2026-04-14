package com.quantum.wallet.bankwallet.modules.xtransaction.cells

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.quantum.wallet.bankwallet.R
import com.quantum.wallet.bankwallet.core.slideFromRight
import com.quantum.wallet.bankwallet.core.stats.StatEntity
import com.quantum.wallet.bankwallet.core.stats.StatEvent
import com.quantum.wallet.bankwallet.core.stats.StatPage
import com.quantum.wallet.bankwallet.core.stats.StatSection
import com.quantum.wallet.bankwallet.core.stats.stat
import com.quantum.wallet.bankwallet.modules.contacts.ContactsFragment
import com.quantum.wallet.bankwallet.modules.contacts.ContactsModule
import com.quantum.wallet.bankwallet.modules.contacts.Mode
import com.quantum.wallet.bankwallet.ui.compose.components.ButtonSecondaryCircle
import com.quantum.wallet.bankwallet.ui.compose.components.HSpacer
import com.quantum.wallet.bankwallet.ui.compose.components.cell.CellUniversal
import com.quantum.wallet.bankwallet.ui.compose.components.subhead1_leah
import com.quantum.wallet.bankwallet.ui.compose.components.subhead2_grey
import com.quantum.wallet.bankwallet.ui.helpers.TextHelper
import com.quantum.wallet.bankwallet.uiv3.components.menu.MenuGroup
import com.quantum.wallet.bankwallet.uiv3.components.menu.MenuItemX
import com.quantum.wallet.core.helpers.HudHelper
import io.horizontalsystems.marketkit.models.BlockchainType

@Composable
fun AddressCell(
    title: String,
    value: String,
    showAddContactButton: Boolean,
    blockchainType: BlockchainType?,
    statPage: StatPage,
    statSection: StatSection,
    navController: NavController? = null,
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

        if (showAddContactButton) {
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

                stat(
                    page = statPage,
                    event = StatEvent.Copy(StatEntity.Address),
                    section = statSection
                )
            }
        )
    }

    if (showSaveAddressDialog) {
        MenuGroup(
            title = stringResource(R.string.Contacts_AddAddress),
            items = ContactsModule.AddAddressAction.entries.map {
                MenuItemX(stringResource(it.title), false, it)
            },
            onDismissRequest = {
                showSaveAddressDialog = false
            },
            onSelectItem = { action ->
                blockchainType?.let {
                    val args = when (action) {
                        ContactsModule.AddAddressAction.AddToNewContact -> {
                            stat(
                                page = statPage,
                                event = StatEvent.Open(StatPage.ContactNew),
                                section = statSection
                            )
                            ContactsFragment.Input(
                                Mode.AddAddressToNewContact(
                                    blockchainType,
                                    value
                                )
                            )
                        }

                        ContactsModule.AddAddressAction.AddToExistingContact -> {
                            stat(
                                page = statPage,
                                event = StatEvent.Open(StatPage.ContactAddToExisting),
                                section = statSection
                            )
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