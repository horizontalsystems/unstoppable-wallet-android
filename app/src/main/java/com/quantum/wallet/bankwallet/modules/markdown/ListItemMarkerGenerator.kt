package com.quantum.wallet.bankwallet.modules.markdown

abstract class ListItemMarkerGenerator {
    abstract fun getNext(): String

    object Unordered : ListItemMarkerGenerator() {
        override fun getNext() = "• "
    }

    class Ordered(private var startNumber: Int, private val delimiter: Char) : ListItemMarkerGenerator() {
        override fun getNext(): String {
            return "${startNumber++}$delimiter "
        }
    }
}
