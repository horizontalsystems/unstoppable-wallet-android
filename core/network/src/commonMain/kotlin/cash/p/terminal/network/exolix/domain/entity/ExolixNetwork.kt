package cash.p.terminal.network.exolix.domain.entity

data class ExolixNetwork(
    val network: String,
    val name: String,
    val shortName: String?,
    val memoNeeded: Boolean,
    val memoName: String?,
    val contract: String?,
)
