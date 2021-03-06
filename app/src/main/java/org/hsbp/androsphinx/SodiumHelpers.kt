// SPDX-FileCopyrightText: 2019, Andras Veres-Szentkiaalyi <vsza@vsza.hu>
// SPDX-License-Identifier: MIT

package org.hsbp.androsphinx

import org.libsodium.jni.Sodium
import org.libsodium.jni.SodiumConstants
import java.lang.RuntimeException
import java.nio.ByteBuffer

class SodiumException(message: String) : RuntimeException(message)

const val MASTER_KEY_BYTES = 32

enum class Context(private val value: String) {
    SIGNING("sphinx signing key"),
    ENCRYPTION("sphinx encryption key"),
    SALT("sphinx host salt"),
    PASSWORD("sphinx password context");

    fun foldHash(vararg messages: ByteArray): ByteArray =
        messages.fold(value.toByteArray(), ::genericHash)
}

fun genericHash(message: ByteArray, salt: ByteArray): ByteArray {
    val result = ByteArray(Sodium.crypto_generichash_bytes())
    Sodium.crypto_generichash(result, result.size, message, message.size, salt, salt.size)
    return result
}

inline class MasterKey(private val bytes: ByteArray) {
    companion object {
        fun generate(): MasterKey = MasterKey(randomBytes(MASTER_KEY_BYTES))

        fun fromByteArray(value: ByteArray): MasterKey {
            require(value.size == MASTER_KEY_BYTES) { "Invalid master key size" }
            return MasterKey(value)
        }

        fun fromByteBuffer(buffer: ByteBuffer): MasterKey {
            val s = MasterKey(ByteArray(MASTER_KEY_BYTES))
            buffer.get(s.bytes)
            return s
        }
    }

    fun contentEquals(other: MasterKey) = other.bytes.contentEquals(bytes)

    fun foldHash(context: Context, vararg messages: ByteArray): ByteArray =
        context.foldHash(*(listOf(asBytes) + messages.toList()).toTypedArray())

    val asBytes: ByteArray
        get() = bytes
}

inline class Ed25519PrivateKey(private val key: ByteArray) {
    companion object {
        fun fromSeed(seed: ByteArray): Ed25519PrivateKey {
            require(seed.size == Sodium.crypto_sign_seedbytes()) { "Invalid seed size" }
            val sk = ByteArray(Sodium.crypto_sign_secretkeybytes())
            val pk = ByteArray(Sodium.crypto_sign_publickeybytes())
            Sodium.crypto_sign_seed_keypair(pk, sk, seed)
            return Ed25519PrivateKey(sk)
        }
    }

    fun sign(message: ByteArray): ByteArray {
        require(key.size == Sodium.crypto_sign_secretkeybytes()) { "Invalid secret key size" }
        val signature = ByteArray(SodiumConstants.SIGNATURE_BYTES)
        Sodium.crypto_sign_detached(signature, intArrayOf(signature.size), message, message.size, key)
        return signature
    }

    val publicKey: ByteArray
        get() {
            require(key.size == Sodium.crypto_sign_secretkeybytes()) { "Invalid secret key size" }
            val result = ByteArray(Sodium.crypto_sign_ed25519_publickeybytes())
            Sodium.crypto_sign_ed25519_sk_to_pk(result, key)
            return result
        }
}

inline class SecretBoxKey(private val key: ByteArray) {
    companion object {
        fun fromByteArray(value: ByteArray): SecretBoxKey {
            require(value.size == SodiumConstants.XSALSA20_POLY1305_SECRETBOX_KEYBYTES) { "Invalid key size" }
            return SecretBoxKey(value)
        }
    }

    fun encrypt(plainText: ByteArray): Pair<ByteArray, ByteArray> {
        require(key.size == SodiumConstants.XSALSA20_POLY1305_SECRETBOX_KEYBYTES) { "Invalid key size" }
        val cipherText = ByteArray(plainText.size + SodiumConstants.MAC_BYTES)
        val nonce = randomBytes(SodiumConstants.NONCE_BYTES)
        Sodium.crypto_secretbox_easy(cipherText, plainText, plainText.size, nonce, key)
        return nonce to cipherText
    }

    fun decrypt(input: ByteArray): ByteArray {
        require(key.size == SodiumConstants.XSALSA20_POLY1305_SECRETBOX_KEYBYTES) { "Invalid key size" }
        require(input.size > SodiumConstants.NONCE_BYTES) { "Invalid input size" }
        val cipherText = input.sliceArray(SodiumConstants.NONCE_BYTES until input.size)
        val message = ByteArray(cipherText.size - SodiumConstants.MAC_BYTES)
        if (Sodium.crypto_secretbox_open_easy(message, cipherText, cipherText.size, input, key) != 0) {
            throw SodiumException("Cannot open secretBox")
        }
        return message
    }
}

fun randomBytes(size: Int): ByteArray {
    val result = ByteArray(size)
    Sodium.randombytes(result, result.size)
    return result
}
