package io.horizontalsystems.chartview

//class CurveAnimatorTest {
//
//    @Test
//    fun matchTimestamps3() {
//        val timestamps1 = listOf(785800L, 787600L)
//        val timestamps2 = listOf(784000L)
//
//        val expected = listOf(
//            785800L to 784000L,
//            787600L to 784000L,
//        )
//
//        val actual = CurveAnimator.matchTimestamps(timestamps1, timestamps2)
//
//        assertEquals(expected, actual)
//    }
//    @Test
//    fun matchTimestamps() {
//        val timestamps1 = listOf(4L, 8L)
//        val timestamps2 = listOf(2L, 3L, 4L, 5L, 7L, 9L)
//
//        val expected = listOf(
//            4L to 2L,
//            4L to 3L,
//            4L to 4L,
//            8L to 5L,
//            8L to 7L,
//            8L to 9L,
//        )
//
//        val actual = CurveAnimator.matchTimestamps(timestamps1, timestamps2)
//
//        assertEquals(expected, actual)
//    }
//
//    @Test
//    fun matchTimestamps2() {
//        val timestamps1 = listOf(2L, 3L, 4L, 5L, 7L, 9L)
//        val timestamps2 = listOf(4L, 8L)
//
//        val expected = listOf(
//            2L to 4L,
//            3L to 4L,
//            4L to 4L,
//            5L to 8L,
//            7L to 8L,
//            9L to 8L,
//        )
//
//        val actual = CurveAnimator.matchTimestamps(timestamps1, timestamps2)
//
//        assertEquals(expected, actual)
//    }
//
//
//    @Test
//    fun testXxx1() {
//
//        val from = linkedMapOf(
//            1636329600L to 3.5747635f,
//            1636588800L to 5.186029f,
//            1636848000L to 5.357986f,
//            1637020800L to 5.7387805f,
//            1637107200L to 5.9208145f,
//            1637366400L to 6.466916f,
//            1637625600L to 7.0130177f,
//            1637884800L to 7.5067773f,
//            1638144000L to 8.026708f,
//            1638230400L to 8.200018f,
//            1638403200L to 6.968172f,
//            1638662400L to 5.9096365f,
//            1638835200L to 5.2039466f,
//            1638921600L to 5.5105925f,
//            1639180800L to 5.1115484f,
//            1639440000L to 4.712504f,
//            1639699200L to 4.6086545f,
//            1639958400L to 4.3572073f,
//            1640044800L to 4.2733917f,
//            1640217600L to 4.7893767f,
//            1640476800L to 5.221546f,
//            1640649600L to 5.5096593f,
//            1640736000L to 4.9986005f,
//            1640995200L to 4.775655f,
//            1641254400L to 4.5527086f,
//            1641513600L to 4.184746f,
//            1641772800L to 3.8892913f,
//            1641859200L to 3.7908063f,
//            1642032000L to 3.847497f,
//            1642291200L to 3.805703f,
//            1642464000L to 3.7778401f,
//            1642550400L to 3.3118947f,
//            1642809600L to 2.8180861f,
//            1643068800L to 2.3242779f,
//            1643328000L to 2.739214f,
//            1643587200L to 2.6997778f,
//            1643673600L to 2.6866324f,
//            1643846400L to 2.7638307f,
//            1644105600L to 2.8278835f,
//            1644278400L to 2.8705852f,
//            1644364800L to 2.6516452f,
//            1644624000L to 2.4754071f,
//            1644883200L to 2.2991688f,
//            1645142400L to 1.9287641f,
//            1645401600L to 1.6554426f,
//            1645488000L to 1.5643355f,
//            1645660800L to 1.6195389f,
//            1645920000L to 1.5836352f,
//            1646092800L to 1.5596994f,
//            1646179200L to 1.5826454f,
//            1646438400L to 1.5816555f,
//            1646697600L to 1.5806657f,
//            1646731497L to 1.599997f,
//            1646731545L to 1.6f
//        )
//
//        val to = linkedMapOf(
//            1636329600L to 3.5747635f,
//            1636588800L to 5.186029f,
//            1636848000L to 5.357986f,
//            1637107200L to 5.362092f,
//            1637366400L to 7.672116f,
//            1637625600L to 7.0130177f,
//            1637884800L to 9.312377f,
//            1638144000L to 7.9761953f,
//            1638403200L to 7.405244f,
//            1638662400L to 5.849809f,
//            1638921600L to 5.21653f,
//            1639180800L to 4.7727695f,
//            1639440000L to 4.712504f,
//            1639699200L to 4.936418f,
//            1639958400L to 4.507408f,
//            1640217600L to 4.5131874f,
//            1640476800L to 4.7149305f,
//            1640736000L to 4.859906f,
//            1640995200L to 4.895305f,
//            1641254400L to 4.5527086f,
//            1641513600L to 4.1290045f,
//            1641772800L to 3.7531083f,
//            1642032000L to 3.7015436f,
//            1642291200L to 3.6214633f,
//            1642550400L to 3.7569797f,
//            1642809600L to 2.710161f,
//            1643068800L to 2.3242779f,
//            1643328000L to 2.6639943f,
//            1643587200L to 2.5849485f,
//            1643846400L to 2.9585044f,
//            1644105600L to 2.8767877f,
//            1644364800L to 2.786587f,
//            1644624000L to 2.2984786f,
//            1644883200L to 2.2991688f,
//            1645142400L to 2.1271038f,
//            1645401600L to 1.8300197f,
//            1645660800L to 1.5650332f,
//            1645920000L to 1.4948473f,
//            1646179200L to 1.585158f,
//            1646438400L to 1.3221353f,
//            1646697600L to 1.5806657f,
//            1646731545L to 1.6f
//        )
//
//        val filled = CurveAnimator.fillWith(to, from)
//
//        assert(false) {
//            val x = filled.map { (k, v) ->
//                "$k to $v"
//            }
//                .joinToString(",\n")
//
//            "\nlinkedMapOf(\n$x\n)\n\n"
//
//        }
//
//    }
//
//    @Test
//    fun testXxx() {
//
//        val from = linkedMapOf(
//            1638230400L to 8.200018f,
//        )
//
//        val to = linkedMapOf(
//            1638144000L to 7.9761953f,
//            1638403200L to 7.405244f,
//        )
//
//        val filled = CurveAnimator.fillWith(to, from)
//
//        assert(false) {
//            val x = filled.map { (k, v) ->
//                "$k to $v"
//            }
//                .joinToString(",\n")
//
//            "\nlinkedMapOf(\n$x\n)\n\n"
//
//        }
//
//    }
//
//}