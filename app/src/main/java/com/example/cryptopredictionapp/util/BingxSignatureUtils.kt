package com.example.cryptopredictionapp.util

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import java.util.TreeMap

object BingxSignatureUtils {

    fun generateSignature(originString: String, secretKey: String): String {
        val signingKey = SecretKeySpec(secretKey.toByteArray(), "HmacSHA256")
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(signingKey)
        return bytesToHex(mac.doFinal(originString.toByteArray()))
    }

    private fun bytesToHex(bytes: ByteArray): String {
        val sb = StringBuilder()
        for (b in bytes) {
            val hex = Integer.toHexString(0xFF and b.toInt())
            if (hex.length == 1) sb.append('0')
            sb.append(hex)
        }
        return sb.toString()
    }

    // Parametreleri A'dan Z'ye sıralayıp string yapar (API'nin kabul etmesi için ŞART)
    fun createQueryString(params: TreeMap<String, String>): String {
        return params.map { "${it.key}=${it.value}" }.joinToString("&")
    }
}