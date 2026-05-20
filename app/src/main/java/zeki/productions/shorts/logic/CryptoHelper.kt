package zeki.productions.shorts.logic

import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

object CryptoHelper {
    private val keySpec: SecretKeySpec by lazy {
        val sha1 = MessageDigest.getInstance("SHA-1")
        val key = sha1.digest(CryptoConstants.PASSWORD.toByteArray()).copyOfRange(0, 16)
        SecretKeySpec(key, "AES")
    }

    private fun getCipher(mode: Int, iv: ByteArray): Cipher {
        val cipher = Cipher.getInstance(CryptoConstants.ALGORITHM)
        cipher.init(mode, keySpec, IvParameterSpec(iv))
        return cipher
    }

    fun decryptHeader(encryptedData: ByteArray, iv: ByteArray): ByteArray {
        val cipher = getCipher(Cipher.DECRYPT_MODE, iv)
        return cipher.doFinal(encryptedData)
    }

    fun decryptFull(encryptedData: ByteArray): String {
        if (encryptedData.size < CryptoConstants.IV_SIZE) return ""
        val iv = encryptedData.copyOfRange(0, CryptoConstants.IV_SIZE)
        val body = encryptedData.copyOfRange(CryptoConstants.IV_SIZE, encryptedData.size)

        // FIX: Extract multiple of 16 to avert crashes on unaligned chunks
        val validSize = body.size - (body.size % 16)
        if (validSize <= 0) return ""

        return String(decryptHeader(body.copyOf(validSize), iv)).trim()
    }
}