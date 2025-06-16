package cash.p.terminal.modules.settings.security.ui

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import cash.p.terminal.R
import cash.p.terminal.modules.settings.security.SecurityCenterCell
import cash.p.terminal.ui.compose.components.HsSwitch
import cash.p.terminal.ui_compose.components.InfoText
import cash.p.terminal.ui_compose.components.CellUniversalLawrenceMutableSection
import cash.p.terminal.ui_compose.components.body_jacob
import cash.p.terminal.ui_compose.components.body_leah
import cash.p.terminal.ui_compose.components.body_lucian
import cash.p.terminal.ui_compose.components.subhead1_grey
import cash.p.terminal.ui_compose.theme.ComposeAppTheme
import cash.p.terminal.wallet.managers.TransactionDisplayLevel

@Composable
internal fun TransactionAutoHideBlock(
    transactionAutoHideEnabled: Boolean,
    displayLevel: TransactionDisplayLevel,
    transactionAutoHideSeparatePinExists: Boolean,
    onTransactionAutoHideEnabledChange: (Boolean) -> Unit,
    onPinClicked: () -> Unit,
    onDisablePinClicked: () -> Unit,
    onChangeDisplayClicked: () -> Unit
) {
    CellUniversalLawrenceMutableSection(
        mutableListOf<@Composable () -> Unit>().apply {
            add {
                SecurityCenterCell(
                    start = {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_off_24),
                            tint = ComposeAppTheme.colors.grey,
                            modifier = Modifier.size(24.dp),
                            contentDescription = null
                        )
                    },
                    center = {
                        body_leah(
                            text = stringResource(id = R.string.Appearance_TransactionAutoHide),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    end = {
                        HsSwitch(
                            checked = transactionAutoHideEnabled,
                            onCheckedChange = onTransactionAutoHideEnabledChange
                        )
                    }
                )
            }
            if (transactionAutoHideEnabled) {
                add {
                    SecurityCenterCell(
                        start = {
                            Icon(
                                painter = painterResource(R.drawable.icon_paper_contract_24),
                                tint = ComposeAppTheme.colors.grey,
                                modifier = Modifier.size(24.dp),
                                contentDescription = null,
                            )
                        },
                        center = {
                            body_leah(
                                text = stringResource(R.string.display),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                        end = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                val displayText = when (displayLevel) {
                                    TransactionDisplayLevel.NOTHING -> R.string.display_nothing
                                    TransactionDisplayLevel.LAST_2_TRANSACTIONS -> R.string.display_last_2_transactions
                                    TransactionDisplayLevel.LAST_4_TRANSACTIONS -> R.string.display_last_4_transactions
                                    TransactionDisplayLevel.LAST_1_TRANSACTION -> R.string.display_last_1_transaction
                                }
                                subhead1_grey(
                                    modifier = Modifier.padding(horizontal = 8.dp),
                                    text = stringResource(displayText)
                                )
                                Icon(
                                    painter = painterResource(R.drawable.ic_big_forward_20),
                                    tint = ComposeAppTheme.colors.grey,
                                    modifier = Modifier.size(24.dp),
                                    contentDescription = null,
                                )
                            }
                        },
                        onClick = onChangeDisplayClicked
                    )
                }
                add {
                    SecurityCenterCell(
                        start = {
                            Icon(
                                painter = painterResource(R.drawable.ic_passcode),
                                tint = ComposeAppTheme.colors.jacob,
                                modifier = Modifier.size(24.dp),
                                contentDescription = null,
                            )
                        },
                        center = {
                            val text = if (transactionAutoHideSeparatePinExists) {
                                R.string.SettingsSecurity_EditPin
                            } else {
                                R.string.set_separate_passcode
                            }
                            body_jacob(
                                text = stringResource(text),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                        onClick = onPinClicked
                    )
                }
                if (transactionAutoHideEnabled && transactionAutoHideSeparatePinExists) {
                    add {
                        SecurityCenterCell(
                            start = {
                                Icon(
                                    painter = painterResource(R.drawable.ic_delete_20),
                                    tint = ComposeAppTheme.colors.lucian,
                                    modifier = Modifier.size(24.dp),
                                    contentDescription = null,
                                )
                            },
                            center = {
                                body_lucian(
                                    text = stringResource(R.string.SettingsSecurity_DeleteSeparatePasscode),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            },
                            onClick = onDisablePinClicked
                        )
                    }
                }
            }
        }
    )
    InfoText(
        text = stringResource(R.string.Appearance_TransactionAutoHide_Description),
        paddingBottom = 32.dp
    )
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun TransactionAutoHideBlockPreview() {
    var transactionAutoHideEnabled by remember { mutableStateOf(true) }
    ComposeAppTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            TransactionAutoHideBlock(
                transactionAutoHideEnabled = transactionAutoHideEnabled,
                displayLevel = TransactionDisplayLevel.LAST_2_TRANSACTIONS,
                transactionAutoHideSeparatePinExists = false,
                onTransactionAutoHideEnabledChange = {
                    transactionAutoHideEnabled = it
                },
                onPinClicked = {},
                onDisablePinClicked = {},
                onChangeDisplayClicked = {},
            )
        }
    }
}
