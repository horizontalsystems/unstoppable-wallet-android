package io.horizontalsystems.bankwallet.ui.compose.components

import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
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
import io.horizontalsystems.bankwallet.core.stats.StatEntity
import io.horizontalsystems.bankwallet.core.stats.StatEvent
import io.horizontalsystems.bankwallet.core.stats.StatPage
import io.horizontalsystems.bankwallet.core.stats.StatSection
import io.horizontalsystems.bankwallet.core.stats.stat
import io.horizontalsystems.bankwallet.core.stats.statResendType
import io.horizontalsystems.bankwallet.modules.contacts.ContactsFragment
import io.horizontalsystems.bankwallet.modules.contacts.ContactsModule
import io.horizontalsystems.bankwallet.modules.contacts.Mode
import io.horizontalsystems.bankwallet.modules.info.TransactionDoubleSpendInfoFragment
import io.horizontalsystems.bankwallet.modules.info.TransactionLockTimeInfoFragment
import io.horizontalsystems.bankwallet.modules.transactionInfo.AmountType
import io.horizontalsystems.bankwallet.modules.transactionInfo.ColorName
import io.horizontalsystems.bankwallet.modules.transactionInfo.ColoredValue
import io.horizontalsystems.bankwallet.modules.transactionInfo.TransactionInfoViewItem
import io.horizontalsystems.bankwallet.modules.transactionInfo.options.SpeedUpCancelType
import io.horizontalsystems.bankwallet.modules.transactionInfo.options.TransactionSpeedUpCancelFragment
import io.horizontalsystems.bankwallet.modules.transactionInfo.resendbitcoin.ResendBitcoinFragment
import io.horizontalsystems.bankwallet.modules.transactions.TransactionStatus
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.helpers.LinkHelper
import io.horizontalsystems.bankwallet.ui.helpers.TextHelper
import io.horizontalsystems.bankwallet.uiv3.components.menu.MenuGroup
import io.horizontalsystems.bankwallet.uiv3.components.menu.MenuItemX
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
                tint = ComposeAppTheme.colors.leah,
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
fun WarningMessageCell(message: String) {
    TextImportantWarning(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        text = message
    )
}

@Composable
fun DescriptionCell(text: String) {
    subhead2_grey(
        modifier = Modifier.padding(start = 32.dp, end = 32.dp, bottom = 12.dp),
        text = text
    )
}

@Composable
fun TransactionNftAmountCell(
    title: String,
    amount: ColoredValue,
    nftName: String?,
    iconUrl: String?,
    iconPlaceholder: Int?,
    badge: String?,
) {
    RowUniversal(
        modifier = Modifier.padding(horizontal = 16.dp),
    ) {
        HsImage(
            url = iconUrl,
            placeholder = iconPlaceholder,
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(CornerSize(4.dp)))
        )
        HSpacer(16.dp)
        Column {
            subhead2_leah(text = title)
            VSpacer(height = 1.dp)
            caption_grey(text = badge ?: stringResource(id = R.string.CoinPlatforms_Native))
        }
        HSpacer(8.dp)
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.End
        ) {
            SubHead1ColoredValue(value = amount)
            nftName?.let {
                VSpacer(height = 1.dp)
                subhead2_grey(
                    text = it,
                    textAlign = TextAlign.End,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
fun TransactionAmountCell(
    amountType: AmountType,
    fiatAmount: ColoredValue?,
    coinAmount: ColoredValue,
    coinIconUrl: String?,
    alternativeCoinIconUrl: String?,
    badge: String?,
    coinIconPlaceholder: Int?,
    onClick: (() -> Unit)? = null
) {
    val title = when (amountType) {
        AmountType.YouSent -> stringResource(R.string.TransactionInfo_YouSent)
        AmountType.YouGot -> stringResource(R.string.TransactionInfo_YouGot)
        AmountType.Received -> stringResource(R.string.TransactionInfo_Received)
        AmountType.Sent -> stringResource(R.string.TransactionInfo_Sent)
        AmountType.Approved -> stringResource(R.string.TransactionInfo_Approved)
        AmountType.Minted -> stringResource(R.string.TransactionInfo_Minted)
    }
    RowUniversal(
        modifier = Modifier.padding(horizontal = 16.dp),
        onClick = onClick
    ) {
        HsImage(
            url = coinIconUrl,
            alternativeUrl = alternativeCoinIconUrl,
            placeholder = coinIconPlaceholder,
            modifier = Modifier.size(32.dp)
        )
        HSpacer(16.dp)
        Column {
            subhead2_leah(text = title)
            VSpacer(height = 1.dp)
            caption_grey(text = badge ?: stringResource(id = R.string.CoinPlatforms_Native))
        }
        HFillSpacer(minWidth = 8.dp)
        Column(horizontalAlignment = Alignment.End) {
            SubHead1ColoredValue(value = coinAmount)
            fiatAmount?.let {
                VSpacer(height = 1.dp)
                SubHead2ColoredValue(value = it)
            }
        }
    }
}

@Composable
fun TitleAndValueCell(
    title: String,
    value: String,
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
fun PriceWithToggleCell(
    title: String,
    valueOne: String,
    valueTwo: String,
) {
    var showValueOne by remember { mutableStateOf(true) }

    RowUniversal(
        modifier = Modifier.padding(horizontal = 16.dp),
    ) {
        subhead2_grey(text = title, modifier = Modifier.padding(end = 16.dp))
        subhead1_leah(
            text = if (showValueOne) valueOne else valueTwo,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f)
        )
        HSpacer(8.dp)
        HsIconButton(
            onClick = {
                showValueOne = !showValueOne

                stat(page = StatPage.TransactionInfo, event = StatEvent.TogglePrice)
            },
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_arrow_swap3_20),
                contentDescription = null,
                tint = ComposeAppTheme.colors.grey,
            )
        }
    }
}

@Composable
fun TransactionInfoAddressCell(
    title: String,
    value: String,
    showAdd: Boolean,
    blockchainType: BlockchainType?,
    navController: NavController? = null,
    onCopy: (() -> Unit)? = null,
    onAddToExisting: (() -> Unit)? = null,
    onAddToNew: (() -> Unit)? = null,
) {
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

                onCopy?.invoke()
            }
        )
    }

    if (showSaveAddressDialog) {
        MenuGroup(
            title = stringResource(R.string.Contacts_AddAddress),
            items = ContactsModule.AddAddressAction.values().map {
                MenuItemX(stringResource(it.title), false, it)
            },
            onDismissRequest = {
                showSaveAddressDialog = false
            },
            onSelectItem = { action ->
                blockchainType?.let {
                    val args = when (action) {
                        ContactsModule.AddAddressAction.AddToNewContact -> {
                            onAddToNew?.invoke()
                            ContactsFragment.Input(Mode.AddAddressToNewContact(blockchainType, value))
                        }

                        ContactsModule.AddAddressAction.AddToExistingContact -> {
                            onAddToExisting?.invoke()
                            ContactsFragment.Input(Mode.AddAddressToExistingContact(blockchainType, value))
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
            onClick = {
                navController.slideFromBottom(R.id.statusInfoDialog)
                stat(
                    page = StatPage.TransactionInfo,
                    event = StatEvent.Open(StatPage.Info),
                    section = StatSection.Status
                )
            }
        ) {
            Image(
                painter = painterResource(R.drawable.ic_info_20),
                contentDescription = null
            )
        }
        Spacer(
            Modifier
                .weight(1f)
                .defaultMinSize(minWidth = 8.dp)
        )
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
    blockchainType: BlockchainType,
    navController: NavController
) {
    RowUniversal(
        modifier = Modifier.padding(horizontal = 16.dp),
        onClick = {
            openTransactionOptionsModule(
                SpeedUpCancelType.SpeedUp,
                transactionHash,
                blockchainType,
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
        body_leah(text = stringResource(R.string.TransactionInfo_SpeedUp))
    }
}

@Composable
fun TransactionInfoCancelCell(
    transactionHash: String,
    blockchainType: BlockchainType,
    navController: NavController
) {
    RowUniversal(
        modifier = Modifier.padding(horizontal = 16.dp),
        onClick = {
            openTransactionOptionsModule(
                SpeedUpCancelType.Cancel,
                transactionHash,
                blockchainType,
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
        body_leah(text = stringResource(R.string.TransactionInfoOptions_Cancel_Button))
    }
}

@Composable
fun TransactionInfoRbfCell(
    rbfEnabled: Boolean
) {
    RowUniversal {
        subhead2_grey(
            modifier = Modifier.padding(horizontal = 16.dp),
            text = stringResource(R.string.Send_Rbf),
        )
        Spacer(modifier = Modifier.weight(1f))
        val enabledText = if (rbfEnabled) {
            stringResource(R.string.Send_RbfEnabled)
        } else {
            stringResource(R.string.Send_RbfDisabled)
        }
        subhead1_leah(text = enabledText)
        Spacer(modifier = Modifier.width(16.dp))
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

                stat(
                    page = StatPage.TransactionInfo,
                    event = StatEvent.Copy(StatEntity.TransactionId)
                )
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

                stat(
                    page = StatPage.TransactionInfo,
                    event = StatEvent.Share(StatEntity.TransactionId)
                )
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
        onClick = {
            LinkHelper.openLinkInAppBrowser(context, url)

            stat(
                page = StatPage.TransactionInfo,
                event = StatEvent.Open(StatPage.ExternalBlockExplorer)
            )
        }
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

                    stat(
                        page = StatPage.TransactionInfo,
                        event = StatEvent.Copy(StatEntity.RawTransaction)
                    )
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

                    navController.slideFromBottom(
                        R.id.transactionLockTimeInfoFragment,
                        TransactionLockTimeInfoFragment.Input(lockTime)
                    )

                    stat(
                        page = StatPage.TransactionInfo,
                        event = StatEvent.Open(StatPage.Info),
                        section = StatSection.TimeLock
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
                navController.slideFromBottom(
                    R.id.transactionDoubleSpendInfoFragment,
                    TransactionDoubleSpendInfoFragment.Input(
                        transactionHash,
                        conflictingHash
                    )
                )

                stat(page = StatPage.TransactionInfo, event = StatEvent.Open(StatPage.DoubleSpend))
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

private fun openTransactionOptionsModule(
    type: SpeedUpCancelType,
    transactionHash: String,
    blockchainType: BlockchainType,
    navController: NavController
) {
    when (blockchainType) {
        BlockchainType.Bitcoin,
        BlockchainType.BitcoinCash,
        BlockchainType.ECash,
        BlockchainType.Litecoin,
        BlockchainType.Dash -> {
            navController.slideFromRight(
                R.id.resendBitcoinFragment,
                ResendBitcoinFragment.Input(type)
            )
        }

        BlockchainType.Ethereum,
        BlockchainType.BinanceSmartChain,
        BlockchainType.Polygon,
        BlockchainType.Avalanche,
        BlockchainType.Optimism,
        BlockchainType.Base,
        BlockchainType.ZkSync,
        BlockchainType.ArbitrumOne -> {
            navController.slideFromRight(
                R.id.transactionSpeedUpCancelFragment,
                TransactionSpeedUpCancelFragment.Input(blockchainType, type, transactionHash)
            )
        }

        BlockchainType.Zcash,
        BlockchainType.Solana,
        BlockchainType.Gnosis,
        BlockchainType.Fantom,
        BlockchainType.Tron,
        BlockchainType.Ton,
        BlockchainType.Stellar,
        BlockchainType.Monero,
        is BlockchainType.Unsupported -> Unit
    }

    stat(
        page = StatPage.TransactionInfo,
        event = StatEvent.OpenResend(blockchainType.uid, type.statResendType)
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

        ColorName.Grey -> {
            subhead1_grey(text = value.value)
        }

        ColorName.Leah -> {
            subhead2_leah(text = value.value)
        }
    }
}