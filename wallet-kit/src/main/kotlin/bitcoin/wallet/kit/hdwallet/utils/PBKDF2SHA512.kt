/*
 * Copyright (c) 2012 Cole Barnes [cryptofreek{at}gmail{dot}com]
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 * 
 */

package bitcoin.wallet.kit.hdwallet.utils

import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.experimental.xor

/**
 *
 * This is a clean-room implementation of PBKDF2 using RFC 2898 as a reference.
 *
 *
 * RFC 2898: http://tools.ietf.org/html/rfc2898#section-5.2
 *
 *
 * This code passes all RFC 6070 test vectors: http://tools.ietf.org/html/rfc6070
 *
 *
 * http://cryptofreek.org/2012/11/29/pbkdf2-pure-java-implementation/<br></br>
 * Modified to use SHA-512 - Ken Sedgwick ken@bonsai.com
 */
object PBKDF2SHA512 {
    fun derive(P: String, S: String, c: Int, dkLen: Int): ByteArray {
        val baos = ByteArrayOutputStream()

        try {
            val hLen = 20

            if (dkLen > (Math.pow(2.0, 32.0) - 1) * hLen) {
                throw IllegalArgumentException("derived key too long")
            } else {
                val l = Math.ceil(dkLen.toDouble() / hLen.toDouble()).toInt()
                // int r = dkLen - (l-1)*hLen;

                for (i in 1..l) {
                    val T = F(P, S, c, i)
                    baos.write(T!!)
                }
            }
        } catch (e: Exception) {
            throw RuntimeException(e)
        }

        val baDerived = ByteArray(dkLen)
        System.arraycopy(baos.toByteArray(), 0, baDerived, 0, baDerived.size)

        return baDerived
    }

    @Throws(Exception::class)
    private fun F(P: String, S: String, c: Int, i: Int): ByteArray? {
        var U_LAST: ByteArray? = null
        var U_XOR: ByteArray? = null

        val key = SecretKeySpec(P.toByteArray(charset("UTF-8")), "HmacSHA512")
        val mac = Mac.getInstance(key.algorithm)
        mac.init(key)

        for (j in 0 until c) {
            if (j == 0) {
                val baS = S.toByteArray(charset("UTF-8"))
                val baI = INT(i)
                val baU = ByteArray(baS.size + baI.size)

                System.arraycopy(baS, 0, baU, 0, baS.size)
                System.arraycopy(baI, 0, baU, baS.size, baI.size)

                U_XOR = mac.doFinal(baU)
                U_LAST = U_XOR
                mac.reset()
            } else {
                val baU = mac.doFinal(U_LAST)
                mac.reset()

                for (k in U_XOR!!.indices) {
                    U_XOR[k] = (U_XOR[k].xor(baU[k]))
                }

                U_LAST = baU
            }
        }

        return U_XOR
    }

    private fun INT(i: Int): ByteArray {
        val bb = ByteBuffer.allocate(4)
        bb.order(ByteOrder.BIG_ENDIAN)
        bb.putInt(i)

        return bb.array()
    }
}
