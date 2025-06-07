package cash.p.terminal.tangem.domain.address

// cardano:
// byron = legacy
// Shelley = default

// btc:
// segwit = default

// decimal:
// 0x = legacy
// d0 = default
enum class AddressType {
    Default,
    Legacy,
}
