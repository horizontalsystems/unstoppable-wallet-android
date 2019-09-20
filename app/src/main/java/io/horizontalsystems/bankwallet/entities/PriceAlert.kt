package io.horizontalsystems.bankwallet.entities

data class PriceAlert(val coin: Coin, var state: State) {
    enum class State(val value: Int?) {
        OFF(null),
        PERCENT_2(2),
        PERCENT_3(3),
        PERCENT_5(5);

        companion object {
            fun valueOf(value: Int?): State {
                return values().find { it.value == value } ?: OFF
            }
        }
    }
}
