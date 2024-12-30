package cash.p.terminal.wallet

interface IAdapter {
    fun start()
    fun stop()
    fun refresh()

    val debugInfo: String
}