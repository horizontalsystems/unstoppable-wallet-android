package io.horizontalsystems.chartview

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class XxxTest {

    private lateinit var xxx: Xxx

    @Before
    fun setup() {
        xxx = Xxx(animator)
    }

    @Test
    fun testGetPointsForFrame1() {
        val prevStartTimestamp = 0L
        val prevPointsMap = linkedMapOf(
            0L to Xxx.Point(0f, 0f),
            10L to Xxx.Point(10f, 10f),
        )
        val nextPointsMap = linkedMapOf(
            -1L to Xxx.Point(100f, 100f),
            5L to Xxx.Point(100f, 100f),
        )

        val expected = linkedMapOf(
            -1L to Xxx.Point(-1f, 5f),
            0L to Xxx.Point(0f, 0f),
            5L to Xxx.Point(5f, 5f),
            10L to Xxx.Point(10f, 10f)
        )

        val actual = xxx.fillWith(prevPointsMap, nextPointsMap, prevStartTimestamp)

        assertEquals(expected, actual)
    }
}