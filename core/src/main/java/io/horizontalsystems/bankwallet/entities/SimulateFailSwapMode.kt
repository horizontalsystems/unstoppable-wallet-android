package io.horizontalsystems.bankwallet.entities

// Debug-only switch for testing failed-swap handling.
// - Server: asks the USwap server to return an action_required swap (testActionRequired=true).
// - Local: overrides the swap status to action_required locally, without any server call.
enum class SimulateFailSwapMode {
    None, Server, Local;

    companion object {
        fun fromString(value: String?) = entries.firstOrNull { it.name == value } ?: None
    }
}
