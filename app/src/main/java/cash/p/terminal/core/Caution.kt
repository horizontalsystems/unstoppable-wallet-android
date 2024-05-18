package cash.p.terminal.core

data class Caution(val text: String, val type: Type) {
    enum class Type {
        Error, Warning
    }
}
