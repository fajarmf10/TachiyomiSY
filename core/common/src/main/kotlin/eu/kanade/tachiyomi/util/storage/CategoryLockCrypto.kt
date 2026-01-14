package eu.kanade.tachiyomi.util.storage

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import eu.kanade.tachiyomi.core.security.SecurityPreferences
import uy.kohesive.injekt.injectLazy
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec

// SY -->
/**
 * Encryption utilities for category PIN storage.
 * PINs are encrypted using Android KeyStore and stored in SharedPreferences
 * as a StringSet in format "categoryId:encryptedPin"
 */
object CategoryLockCrypto {
    private const val KEYSTORE = "AndroidKeyStore"
    private const val ALIAS_CATEGORY_PIN = "categoryPin"
    private const val ALGORITHM = KeyProperties.KEY_ALGORITHM_AES
    private const val BLOCK_MODE = KeyProperties.BLOCK_MODE_CBC
    private const val PADDING = KeyProperties.ENCRYPTION_PADDING_PKCS7
    private const val CRYPTO_SETTINGS = "$ALGORITHM/$BLOCK_MODE/$PADDING"
    private const val KEY_SIZE = 256
    private const val IV_SIZE = 16
    private const val BUFFER_SIZE = 2048

    private val securityPreferences: SecurityPreferences by injectLazy()

    private val keyStore = KeyStore.getInstance(KEYSTORE).apply {
        load(null)
    }

    private val encryptionCipher: Cipher
        get() = Cipher.getInstance(CRYPTO_SETTINGS).apply {
            init(Cipher.ENCRYPT_MODE, getKey())
        }

    private fun getDecryptCipher(iv: ByteArray): Cipher {
        return Cipher.getInstance(CRYPTO_SETTINGS).apply {
            init(Cipher.DECRYPT_MODE, getKey(), IvParameterSpec(iv))
        }
    }

    private fun getKey(): SecretKey {
        val loadedKey = keyStore.getEntry(ALIAS_CATEGORY_PIN, null) as? KeyStore.SecretKeyEntry
        return loadedKey?.secretKey ?: generateKey()
    }

    private fun generateKey(): SecretKey {
        return KeyGenerator.getInstance(ALGORITHM).apply {
            init(
                KeyGenParameterSpec.Builder(
                    ALIAS_CATEGORY_PIN,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
                )
                    .setKeySize(KEY_SIZE)
                    .setBlockModes(BLOCK_MODE)
                    .setEncryptionPaddings(PADDING)
                    .setRandomizedEncryptionRequired(true)
                    .setUserAuthenticationRequired(false)
                    .build(),
            )
        }.generateKey()
    }

    /**
     * Encrypts a PIN string using AES encryption
     */
    private fun encryptPin(pin: String): String {
        val cipher = encryptionCipher
        val outputStream = ByteArrayOutputStream()
        outputStream.use { output ->
            // Write IV first
            output.write(cipher.iv)
            ByteArrayInputStream(pin.toByteArray()).use { input ->
                val buffer = ByteArray(BUFFER_SIZE)
                while (input.available() > BUFFER_SIZE) {
                    input.read(buffer)
                    output.write(cipher.update(buffer))
                }
                output.write(cipher.doFinal(input.readBytes()))
            }
        }
        return Base64.encodeToString(outputStream.toByteArray(), Base64.DEFAULT)
    }

    /**
     * Decrypts an encrypted PIN string
     */
    private fun decryptPin(encryptedPin: String): String {
        val inputStream = Base64.decode(encryptedPin, Base64.DEFAULT).inputStream()
        return inputStream.use { input ->
            val iv = ByteArray(IV_SIZE)
            input.read(iv)
            val cipher = getDecryptCipher(iv)
            val outputStream = ByteArrayOutputStream()
            outputStream.use { output ->
                val buffer = ByteArray(BUFFER_SIZE)
                while (input.available() > BUFFER_SIZE) {
                    input.read(buffer)
                    output.write(cipher.update(buffer))
                }
                output.write(cipher.doFinal(input.readBytes()))
                String(output.toByteArray())
            }
        }
    }

    /**
     * Get all stored category lock data as a map
     */
    private fun getCategoryLockMap(): MutableMap<Long, String> {
        val lockPins = securityPreferences.categoryLockPins().get()
        val map = mutableMapOf<Long, String>()
        lockPins.forEach { entry ->
            val parts = entry.split(":", limit = 2)
            if (parts.size == 2) {
                val categoryId = parts[0].toLongOrNull()
                val encryptedPin = parts[1]
                if (categoryId != null) {
                    map[categoryId] = encryptedPin
                }
            }
        }
        return map
    }

    /**
     * Save the category lock map back to preferences
     */
    private fun saveCategoryLockMap(map: Map<Long, String>) {
        val stringSet = map.map { (categoryId, encryptedPin) ->
            "$categoryId:$encryptedPin"
        }.toSet()
        securityPreferences.categoryLockPins().set(stringSet)
    }

    /**
     * Set a PIN for a category
     */
    fun setPinForCategory(categoryId: Long, pin: String) {
        require(pin.length in 4..10) { "PIN must be 4-10 digits" }
        require(pin.all { it.isDigit() }) { "PIN must contain only digits" }

        val map = getCategoryLockMap()
        map[categoryId] = encryptPin(pin)
        saveCategoryLockMap(map)
    }

    /**
     * Remove PIN for a category
     */
    fun removePinForCategory(categoryId: Long) {
        val map = getCategoryLockMap()
        map.remove(categoryId)
        saveCategoryLockMap(map)
    }

    /**
     * Check if a category has a lock
     */
    fun hasLock(categoryId: Long): Boolean {
        return getCategoryLockMap().containsKey(categoryId)
    }

    /**
     * Verify if the input PIN matches the stored PIN for a category
     */
    fun verifyPin(categoryId: Long, inputPin: String): Boolean {
        val map = getCategoryLockMap()
        val encryptedPin = map[categoryId] ?: return false

        return try {
            val decryptedPin = decryptPin(encryptedPin)
            decryptedPin == inputPin
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Get all category IDs that have locks
     */
    fun getLockedCategoryIds(): Set<Long> {
        return getCategoryLockMap().keys
    }

    /**
     * Delete the encryption key (will invalidate all stored PINs)
     */
    fun deleteKey() {
        keyStore.deleteEntry(ALIAS_CATEGORY_PIN)
        generateKey()
        // Clear all stored PINs since they can't be decrypted anymore
        securityPreferences.categoryLockPins().set(emptySet())
    }
}
// SY <--
