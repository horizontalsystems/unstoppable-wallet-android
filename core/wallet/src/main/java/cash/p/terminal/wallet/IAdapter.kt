package cash.p.terminal.wallet

interface IAdapter {
    fun start()
    fun stop()
    suspend fun refresh()

    val debugInfo: String
    val statusInfo: Map<String, Any>
}