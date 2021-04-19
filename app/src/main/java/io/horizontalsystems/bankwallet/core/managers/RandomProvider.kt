package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.IRandomProvider
import java.util.*

class RandomProvider : IRandomProvider {

    override fun getRandomIndexes(count: Int, maxIndex: Int): List<Int> {
        val indexes = mutableListOf<Int>()

        val random = Random()

        while (indexes.size < count) {
            val index = random.nextInt(maxIndex)

            if (!indexes.contains(index)) {
                indexes.add(index)
            }
        }

        return indexes
    }

}
