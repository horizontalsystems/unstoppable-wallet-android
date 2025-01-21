package cash.p.terminal.modules.multiswap

import java.math.BigDecimal

class SwapRouteNotFound : Throwable()
class NoSupportedSwapProvider : Throwable()
class SwapDepositTooSmall(val minValue: BigDecimal) : Throwable()
