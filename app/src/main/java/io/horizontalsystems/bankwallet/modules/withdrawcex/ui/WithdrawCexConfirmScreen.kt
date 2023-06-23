package io.horizontalsystems.bankwallet.modules.withdrawcex.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.rememberNavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.entities.Currency
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.bankwallet.modules.amount.AmountInputType
import io.horizontalsystems.bankwallet.modules.evmfee.ButtonsGroupWithShade
import io.horizontalsystems.bankwallet.modules.fee.HSFeeInputRaw
import io.horizontalsystems.bankwallet.modules.send.ConfirmAmountCell
import io.horizontalsystems.bankwallet.modules.send.SendButton
import io.horizontalsystems.bankwallet.modules.withdrawcex.WithdrawCexViewModel
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.AppBar
import io.horizontalsystems.bankwallet.ui.compose.components.CellUniversalLawrenceSection
import io.horizontalsystems.bankwallet.ui.compose.components.HsBackButton
import io.horizontalsystems.bankwallet.ui.compose.components.MenuItem
import io.horizontalsystems.bankwallet.ui.compose.components.SectionTitleCell
import io.horizontalsystems.bankwallet.ui.compose.components.TransactionInfoAddressCell
import io.horizontalsystems.bankwallet.ui.compose.components.TransactionInfoCell
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.Coin
import java.math.BigDecimal

@Composable
fun WithdrawCexConfirmScreen(
    mainViewModel: WithdrawCexViewModel,
    openVerification: () -> Unit,
    onNavigateBack: () -> Unit,
    onClose: () -> Unit
) {
    val navController = rememberNavController()

    ComposeAppTheme {
        Scaffold(
            backgroundColor = ComposeAppTheme.colors.tyler,
            topBar = {
                AppBar(
                    title = TranslatableString.ResString(R.string.Send_Confirmation_Title),
                    navigationIcon = {
                        HsBackButton(onClick = onNavigateBack)
                    },
                    menuItems = listOf(
                        MenuItem(
                            title = TranslatableString.ResString(R.string.Button_Close),
                            icon = R.drawable.ic_close,
                            onClick = onClose
                        )
                    )
                )
            }
        ) {
            Column(modifier = Modifier.padding(it)) {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .weight(1f)
                ) {
                    VSpacer(12.dp)
                    val topSectionItems = buildList<@Composable () -> Unit> {
                        add {
                            SectionTitleCell(
                                stringResource(R.string.Send_Confirmation_YouSend),
                                "coin.name",
                                R.drawable.ic_arrow_up_right_12
                            )
                        }
                        add {
                            val coinAmount = "30"

                            val currencyAmount = "$3400"

                            val coin = Coin("BTC", "Bitcoin", "BTC", 8, "wd")

                            ConfirmAmountCell(currencyAmount, coinAmount, coin)
                        }
                        add {
                            TransactionInfoAddressCell(
                                title = stringResource(R.string.Send_Confirmation_To),
                                value = "address.hex",
                                showAdd = false,
                                blockchainType = BlockchainType.Ethereum,
                                navController = navController
                            )
                        }
//                        contact?.let {
//                            add {
//                                TransactionInfoContactCell(name = contact.name)
//                            }
//                        }
                    }

                    CellUniversalLawrenceSection(topSectionItems)

                    VSpacer(16.dp)

                    CellUniversalLawrenceSection(
                        listOf {
                            TransactionInfoCell(stringResource(R.string.CexWithdraw_Network), "Ethereum")
                        }
                    )

                    val bottomSectionItems = buildList<@Composable () -> Unit> {
                        add {
                            HSFeeInputRaw(
                                coinCode = "feeCoin.code",
                                coinDecimal = 18,
                                fee = BigDecimal.TEN,
                                amountInputType = AmountInputType.COIN,
                                rate = CurrencyValue(
                                    Currency(code = "USD", symbol = "$", decimal = 2, R.drawable.icon_32_flag_australia),
                                    3900.toBigDecimal()
                                ),
                                navController = navController
                            )
                        }
                    }

                    VSpacer(16.dp)
                    CellUniversalLawrenceSection(bottomSectionItems)
                    VSpacer(16.dp)
                }

                ButtonsGroupWithShade {
                    SendButton(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, bottom = 32.dp),
                        sendResult = null,
                        onClickSend = {
                            openVerification.invoke()
                        }
                    )
                }
            }
        }
    }
}
