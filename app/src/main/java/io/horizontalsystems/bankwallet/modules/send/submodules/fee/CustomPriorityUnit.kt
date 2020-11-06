package io.horizontalsystems.bankwallet.modules.send.submodules.fee

sealed class CustomPriorityUnit {
    object Satoshi: CustomPriorityUnit()
    object Gwei: CustomPriorityUnit()

    fun getLabel(): String{
        return when(this){
            Satoshi -> "sat/byte"
            Gwei -> "gwei"
        }
    }

    fun getConvertedValue(value: Long): Long {
        return when(this){
            Satoshi -> value
            Gwei -> value * 1_000_000_000
        }
    }

    fun getUnits(value: Long): Long {
        return when(this){
            Satoshi -> value
            Gwei -> value / 1_000_000_000
        }
    }
}
