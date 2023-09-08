package io.horizontalsystems.bankwallet.ui.compose.components

import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.shorten
import io.horizontalsystems.bankwallet.core.slideFromBottom
import io.horizontalsystems.bankwallet.core.slideFromRight
import io.horizontalsystems.bankwallet.entities.nft.NftUid
import io.horizontalsystems.bankwallet.modules.coin.CoinFragment
import io.horizontalsystems.bankwallet.modules.contacts.ContactsFragment
import io.horizontalsystems.bankwallet.modules.contacts.ContactsModule
import io.horizontalsystems.bankwallet.modules.contacts.Mode
import io.horizontalsystems.bankwallet.modules.info.TransactionDoubleSpendInfoFragment
import io.horizontalsystems.bankwallet.modules.info.TransactionLockTimeInfoFragment
import io.horizontalsystems.bankwallet.modules.nft.asset.NftAssetModule
import io.horizontalsystems.bankwallet.modules.transactionInfo.ColorName
import io.horizontalsystems.bankwallet.modules.transactionInfo.ColoredValue
import io.horizontalsystems.bankwallet.modules.transactionInfo.TransactionInfoViewItem
import io.horizontalsystems.bankwallet.modules.transactionInfo.options.TransactionInfoOptionsModule
import io.horizontalsystems.bankwallet.modules.transactionInfo.options.TransactionSpeedUpCancelFragment
import io.horizontalsystems.bankwallet.modules.transactions.TransactionStatus
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.helpers.LinkHelper
import io.horizontalsystems.bankwallet.ui.helpers.TextHelper
import io.horizontalsystems.core.helpers.DateHelper
import io.horizontalsystems.core.helpers.HudHelper
import io.horizontalsystems.marketkit.models.BlockchainType

@Composable
fun SectionTitleCell(
    title: String,
    value: String,
    iconResId: Int?
) {
    RowUniversal(
        modifier = Modifier.padding(horizontal = 16.dp),
    ) {
        iconResId?.let {
            Icon(
                modifier = Modifier.padding(end = 16.dp),
                painter = painterResource(iconResId),
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
fun TransactionNftAmountCell(
    amount: ColoredValue,
    iconUrl: String?,
    iconPlaceholder: Int?,
    nftUid: NftUid,
    providerCollectionUid: String?,
    navController: NavController
) {
    var modifier = Modifier.padding(horizontal = 16.dp)

    if (nftUid.blockchainType !is BlockchainType.Solana) {
        modifier = modifier.clickable {
            navController.slideFromBottom(
                    R.id.nftAssetFragment,
                    NftAssetModule.prepareParams(
                            providerCollectionUid,
                            nftUid
                    )
            )
        }
    }

    RowUniversal(
        modifier = modifier,
    ) {
        CoinImage(
            iconUrl = iconUrl,
            placeholder = iconPlaceholder,
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(CornerSize(4.dp)))
        )
        Spacer(modifier = Modifier.width(16.dp))
        SubHead1ColoredValue(value = amount)
        Spacer(Modifier.weight(1f))
        Icon(
            modifier = Modifier.size(20.dp),
            painter = painterResource(R.drawable.ic_info_20),
            tint = ComposeAppTheme.colors.grey,
            contentDescription = null
        )
    }
}

@Composable
fun TransactionAmountCell(
    fiatAmount: ColoredValue?,
    coinAmount: ColoredValue,
    coinIconUrl: String?,
    coinIconPlaceholder: Int?,
    coinUid: String?,
    navController: NavController
) {
    val clickable = coinUid?.let {
        Modifier.clickable {
            navController.slideFromRight(R.id.coinFragment, CoinFragment.prepareParams(it))
        }
    } ?: Modifier

    RowUniversal(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .then(clickable)
    ) {
        CoinImage(
            iconUrl = coinIconUrl,
            placeholder = coinIconPlaceholder,
            modifier = Modifier.size(32.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        SubHead1ColoredValue(value = coinAmount)
        Spacer(Modifier.weight(1f))
        fiatAmount?.let { SubHead2ColoredValue(value = it) }
    }
}

@Composable
fun TitleAndValueCell(
    title: String,
    value: String
) {
    RowUniversal(
        modifier = Modifier.padding(horizontal = 16.dp),
    ) {
        subhead2_grey(text = title, modifier = Modifier.padding(end = 16.dp))
        Spacer(Modifier.weight(1f))
        subhead1_leah(text = value, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun TransactionInfoAddressCell(title: String, value: String, showAdd: Boolean, blockchainType: BlockchainType?, navController: NavController? = null) {
    val view = LocalView.current
    var showSaveAddressDialog by remember { mutableStateOf(false) }
    RowUniversal(
        modifier = Modifier.padding(horizontal = 16.dp),
    ) {
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
            }
        )
    }

    if (showSaveAddressDialog) {
        SelectorDialogCompose(
            title = stringResource(R.string.Contacts_AddAddress),
            items = ContactsModule.AddAddressAction.values().map {
                SelectorItem(stringResource(it.title), false, it)
            },
            onDismissRequest = {
                showSaveAddressDialog = false
            },
            onSelectItem = { action ->
                blockchainType?.let {
                    val args = when (action) {
                        ContactsModule.AddAddressAction.AddToNewContact -> {
                            ContactsFragment.prepareParams(mode = Mode.AddAddressToNewContact(blockchainType, value))

                        }
                        ContactsModule.AddAddressAction.AddToExistingContact -> {
                            ContactsFragment.prepareParams(mode = Mode.AddAddressToExistingContact(blockchainType, value))
                        }
                    }
                    navController?.slideFromRight(R.id.contactsFragment, args)
                }
            })
    }
}

@Composable
fun TransactionInfoContactCell(name: String) {
    RowUniversal(
        modifier = Modifier.padding(horizontal = 16.dp),
    ) {
        subhead2_grey(text = stringResource(R.string.TransactionInfo_ContactName))
        HSpacer(16.dp)
        subhead1_leah(
            modifier = Modifier.weight(1f),
            text = name,
            textAlign = TextAlign.Right
        )
    }
}

@Composable
fun TransactionInfoStatusCell(
    status: TransactionStatus,
    navController: NavController
) {
    RowUniversal(
        modifier = Modifier.padding(horizontal = 16.dp),
    ) {
        subhead2_grey(
            text = stringResource(R.string.TransactionInfo_Status),
        )
        Spacer(modifier = Modifier.width(8.dp))
        HsIconButton(
            modifier = Modifier.size(20.dp),
            onClick = { navController.slideFromBottom(R.id.statusInfoDialog) }
        ) {
            Image(
                painter = painterResource(R.drawable.ic_info_20),
                contentDescription = null
            )
        }
        Spacer(Modifier.weight(1f).defaultMinSize(minWidth = 8.dp))
        subhead1_leah(
            text = stringResource(statusTitle(status)),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(end = 8.dp)
        )

        when (status) {
            TransactionStatus.Completed -> {
                Icon(
                    painter = painterResource(id = R.drawable.ic_checkmark_20),
                    contentDescription = null,
                    tint = ComposeAppTheme.colors.remus
                )
            }
            TransactionStatus.Failed -> {
                Icon(
                    painter = painterResource(id = R.drawable.ic_attention_20),
                    contentDescription = null,
                    tint = ComposeAppTheme.colors.lucian
                )
            }
            TransactionStatus.Pending -> {
                HSCircularProgressIndicator(progress = 0.15f, size = 20.dp)
            }
            is TransactionStatus.Processing -> {
                HSCircularProgressIndicator(progress = status.progress, size = 20.dp)
            }
        }
    }
}

@Composable
fun TransactionInfoSpeedUpCell(
    transactionHash: String,
    navController: NavController
) {
    RowUniversal(
        modifier = Modifier.padding(horizontal = 16.dp),
        onClick = {
            openTransactionOptionsModule(
                TransactionInfoOptionsModule.Type.SpeedUp,
                transactionHash,
                navController
            )
        }
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_arrow_medium2_up_24),
            contentDescription = null,
            tint = ComposeAppTheme.colors.jacob
        )
        Spacer(Modifier.width(16.dp))
        body_jacob(text = stringResource(R.string.TransactionInfo_SpeedUp))
    }
}

@Composable
fun TransactionInfoCancelCell(
    transactionHash: String,
    navController: NavController
) {
    RowUniversal(
        modifier = Modifier.padding(horizontal = 16.dp),
        onClick = {
            openTransactionOptionsModule(
                TransactionInfoOptionsModule.Type.Cancel,
                transactionHash,
                navController
            )
        }
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_outgoingraw_24),
            contentDescription = null,
            tint = ComposeAppTheme.colors.redL
        )
        Spacer(Modifier.width(16.dp))
        body_lucian(text = stringResource(R.string.TransactionInfo_Cancel))
    }
}


@Composable
fun TransactionInfoTransactionHashCell(transactionHash: String) {
    val view = LocalView.current
    val context = LocalContext.current

    RowUniversal(
        modifier = Modifier.padding(horizontal = 16.dp),
    ) {
        subhead2_grey(
            text = stringResource(R.string.TransactionInfo_Id),
            modifier = Modifier.padding(end = 16.dp)
        )
        Spacer(Modifier.weight(1f))
        ButtonSecondaryDefault(
            modifier = Modifier.height(28.dp),
            title = transactionHash.shorten(),
            onClick = {
                TextHelper.copyText(transactionHash)
                HudHelper.showSuccessMessage(view, R.string.Hud_Text_Copied)
            }
        )
        Spacer(modifier = Modifier.width(8.dp))
        ButtonSecondaryCircle(
            icon = R.drawable.ic_share_20,
            onClick = {
                context.startActivity(Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, transactionHash)
                    type = "text/plain"
                })
            }
        )
    }
}

@Composable
fun TransactionInfoExplorerCell(
    title: String,
    url: String
) {
    val context = LocalContext.current
    RowUniversal(
        modifier = Modifier.padding(horizontal = 16.dp),
        onClick = { LinkHelper.openLinkInAppBrowser(context, url) }
    ) {
        Image(
            modifier = Modifier.size(20.dp),
            painter = painterResource(id = R.drawable.ic_language),
            contentDescription = null,
        )
        body_leah(
            text = title,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(Modifier.weight(1f))
        Image(
            modifier = Modifier.size(20.dp),
            painter = painterResource(id = R.drawable.ic_arrow_right),
            contentDescription = null,
        )
    }
}

@Composable
fun TransactionInfoRawTransaction(rawTransaction: () -> String?) {
    val view = LocalView.current
    RowUniversal(
        modifier = Modifier.padding(horizontal = 16.dp),
    ) {
        subhead2_grey(
            text = stringResource(R.string.TransactionInfo_RawTransaction),
            modifier = Modifier.padding(end = 16.dp)
        )
        Spacer(Modifier.weight(1f))
        ButtonSecondaryCircle(
            icon = R.drawable.ic_copy_20,
            onClick = {
                rawTransaction()?.let {
                    TextHelper.copyText(it)
                    HudHelper.showSuccessMessage(view, R.string.Hud_Text_Copied)
                }
            }
        )
    }
}

@Composable
fun TransactionInfoBtcLockCell(
    lockState: TransactionInfoViewItem.LockState,
    navController: NavController
) {
    RowUniversal(
        modifier = Modifier.padding(horizontal = 16.dp),
    ) {
        Icon(
            modifier = Modifier.padding(end = 16.dp),
            painter = painterResource(lockState.leftIcon),
            tint = ComposeAppTheme.colors.grey,
            contentDescription = null,
        )
        subhead2_grey(text = lockState.title, modifier = Modifier.padding(end = 16.dp))
        Spacer(modifier = Modifier.weight(1f))
        if (lockState.showLockInfo) {
            HsIconButton(
                modifier = Modifier.size(20.dp),
                onClick = {
                    val lockTime = DateHelper.getFullDate(lockState.date)
                    val params = TransactionLockTimeInfoFragment.prepareParams(lockTime)

                    navController.slideFromBottom(
                        R.id.transactionLockTimeInfoFragment,
                        params
                    )
                }
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_info_20),
                    tint = ComposeAppTheme.colors.grey,
                    contentDescription = null,
                )
            }
        }
    }
}

@Composable
fun TransactionInfoDoubleSpendCell(
    transactionHash: String,
    conflictingHash: String,
    navController: NavController
) {
    RowUniversal(
        modifier = Modifier.padding(horizontal = 16.dp),
    ) {
        Icon(
            modifier = Modifier.padding(end = 16.dp),
            painter = painterResource(R.drawable.ic_double_spend_20),
            tint = ComposeAppTheme.colors.grey,
            contentDescription = null,
        )
        subhead2_grey(
            text = stringResource(R.string.TransactionInfo_DoubleSpendNote),
            modifier = Modifier.padding(end = 16.dp)
        )
        Spacer(modifier = Modifier.weight(1f))
        HsIconButton(
            modifier = Modifier.size(20.dp),
            onClick = {
                val params = TransactionDoubleSpendInfoFragment.prepareParams(
                    transactionHash,
                    conflictingHash
                )
                navController.slideFromBottom(
                    R.id.transactionDoubleSpendInfoFragment,
                    params
                )
            }
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_info_20),
                tint = ComposeAppTheme.colors.grey,
                contentDescription = null,
            )
        }

    }
}

@Composable
fun TransactionInfoSentToSelfCell() {
    RowUniversal(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        Icon(
            modifier = Modifier.padding(end = 16.dp),
            painter = painterResource(R.drawable.ic_arrow_return_20),
            tint = ComposeAppTheme.colors.grey,
            contentDescription = null,
        )
        subhead2_grey(text = stringResource(R.string.TransactionInfo_SentToSelfNote))
    }
}

@Composable
fun TransactionInfoCell(title: String, value: String) {
    RowUniversal(
        modifier = Modifier.padding(horizontal = 16.dp),
    ) {
        subhead2_grey(text = title)
        HSpacer(16.dp)
        subhead1_leah(
            modifier = Modifier.weight(1f),
            text = value,
            textAlign = TextAlign.Right
        )
    }
}

private fun openTransactionOptionsModule(type: TransactionInfoOptionsModule.Type, transactionHash: String, navController: NavController) {
    val params = TransactionSpeedUpCancelFragment.prepareParams(type, transactionHash)
    navController.slideFromRight(
        R.id.transactionSpeedUpCancelFragment,
        params
    )
}

private fun statusTitle(status: TransactionStatus) = when (status) {
    TransactionStatus.Completed -> R.string.Transactions_Completed
    TransactionStatus.Failed -> R.string.Transactions_Failed
    TransactionStatus.Pending -> R.string.Transactions_Pending
    is TransactionStatus.Processing -> R.string.Transactions_Processing
}

@Composable
private fun SubHead2ColoredValue(value: ColoredValue) {
    when (value.color) {
        ColorName.Remus -> {
            subhead2_remus(text = value.value)
        }
        ColorName.Lucian -> {
            subhead2_lucian(text = value.value)
        }
        ColorName.Grey -> {
            subhead2_grey(text = value.value)
        }
        ColorName.Leah -> {
            subhead2_leah(text = value.value)
        }
    }
}


@Composable
private fun SubHead1ColoredValue(value: ColoredValue) {
    when (value.color) {
        ColorName.Remus -> {
            subhead1_remus(text = value.value)
        }
        ColorName.Lucian -> {
            subhead1_lucian(text = value.value)
        }
        ColorName.Grey -> {
            subhead1_grey(text = value.value)
        }
        ColorName.Leah -> {
            subhead2_leah(text = value.value)
        }
    }
}