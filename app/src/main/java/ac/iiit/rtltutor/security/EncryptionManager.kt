package ac.iiit.rtltutor.security

import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * EncryptionManager — handles key derivation and AES-GCM encryption/decryption.
 * Uses PBKDF2 for password-based key derivation.
 */
class EncryptionManager {

    companion object {
        private const val KEY_ALGORITHM    = "AES"
        private const val CIPHER_ALGORITHM = "AES/GCM/NoPadding"
        private const val KDF_ALGORITHM    = "PBKDF2WithHmacSHA256"
        private const val KEY_LENGTH_BITS  = 256
        private const val ITERATION_COUNT  = 310_000
        private const val GCM_TAG_BITS     = 128
        private const val GCM_IV_BYTES     = 12
    }

    /**
     * Derive a 256-bit AES key from [password] using [salt] via PBKDF2.
     */
    fun deriveKey(password: String, salt: ByteArray): SecretKey {
        val factory = SecretKeyFactory.getInstance(KDF_ALGORITHM)
        val spec = PBEKeySpec(password.toCharArray(), salt, ITERATION_COUNT, KEY_LENGTH_BITS)
        val tmp = factory.generateSecret(spec)
        return SecretKeySpec(tmp.encoded, KEY_ALGORITHM)
    }

    /**
     * Encrypt [data] bytes with [key] using AES-256-GCM.
     * Returns IV prepended to ciphertext.
     */
    fun encrypt(data: ByteArray, key: SecretKey): ByteArray {
        val iv = ByteArray(GCM_IV_BYTES).also { java.security.SecureRandom().nextBytes(it) }
        val cipher = Cipher.getInstance(CIPHER_ALGORITHM)
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
        val cipherText = cipher.doFinal(data)
        return iv + cipherText
    }

    /**
     * Decrypt [data] bytes (IV + ciphertext) with [key] using AES-256-GCM.
     */
    fun decrypt(data: ByteArray, key: SecretKey): ByteArray {
        val iv = data.copyOfRange(0, GCM_IV_BYTES)
        val cipherText = data.copyOfRange(GCM_IV_BYTES, data.size)
        val cipher = Cipher.getInstance(CIPHER_ALGORITHM)
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
        return cipher.doFinal(cipherText)
    }
}
