package cash.p.terminal.wallet.balance

import cash.p.terminal.wallet.Warning

sealed class BalanceWarning : Warning() {
    data object TronInactiveAccountWarning : BalanceWarning()
}