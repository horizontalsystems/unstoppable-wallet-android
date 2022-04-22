package io.horizontalsystems.bankwallet.core.adapters

import io.horizontalsystems.core.toHexString
import io.horizontalsystems.ethereumkit.contracts.ContractMethodHelper

class EvmContractMethodParser {

    private var methods = mutableMapOf<String, String>()

    init {
        addMethodBySignature("Deposit", "deposit(uint256)")
        addMethodBySignature("TradeWithHintAndFee", "tradeWithHintAndFee(address,uint256,address,address,uint256,uint256,address,uint256,bytes)")

        addMethodById("Farm Deposit", "e2bbb158")
        addMethodById("Farm Withdrawal", "441a3e70")
        addMethodById("Pool Deposit", "f305d719")
        addMethodById("Pool Withdrawal", "ded9382a")
        addMethodById("Stake", "a59f3e0c")
        addMethodById("Unstake", "67dfd4c9")
        addMethodById("Swap", "5ae401dc") // Uniswap v3
        addMethodById("Register Domain Name", "f14fcbc8") // ENS: Request to Register domain Name
    }

    fun parse(input: ByteArray): String? {
        val methodId = input.take(4).toByteArray()
        return methods[methodId.toHexString()]
    }

    private fun addMethodById(name: String, methodId: String) {
        methods[methodId] = name
    }

    private fun addMethodBySignature(name: String, signature: String) {
        val methodId = ContractMethodHelper.getMethodId(signature)

        methods[methodId.toHexString()] = name
    }

}
