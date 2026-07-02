package io.horizontalsystems.core.core.managers

import io.horizontalsystems.core.core.IRandomProvider
import java.util.Random

class RandomProvider : IRandomProvider {

    override fun getRandomNumbers(count: Int, maxIndex: Int): List<Int> {
        val numbers = mutableListOf<Int>()

        val random = Random()

        while (numbers.size < count) {
            val number = random.nextInt(maxIndex)

            if (!numbers.contains(number)) {
                numbers.add(number)
            }
        }

        return numbers
    }

}
