package io.horizontalsystems.solanakit.transactions

import com.solana.core.PublicKey
import com.solana.core.TransactionInstruction
import java.nio.ByteBuffer
import java.nio.ByteOrder

object ComputeBudgetProgram {

    private val programId = PublicKey("ComputeBudget111111111111111111111111111111")

    fun setComputeUnitLimit(units: Long): TransactionInstruction {
        val byteBuffer = ByteBuffer.allocate(9)
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN)
        byteBuffer.put(2.toByte())
        byteBuffer.putLong(units)
        val data = byteBuffer.array()

        return TransactionInstruction(
            data = data,
            keys = listOf(),
            programId = programId
        )
    }

    fun setComputeUnitPrice(microLamports: Long): TransactionInstruction {
        val byteBuffer = ByteBuffer.allocate(9)
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN)
        byteBuffer.put(3.toByte())
        byteBuffer.putLong(microLamports)
        val data = byteBuffer.array()

        return TransactionInstruction(
            data = data,
            keys = listOf(),
            programId = programId
        )
    }

}
