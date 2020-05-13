package com.eric.rockpaperscissor.Common

import android.text.TextUtils
import android.util.Base64
import android.util.Log
import java.io.UnsupportedEncodingException
import java.security.*
import java.security.spec.InvalidKeySpecException
import java.security.spec.X509EncodedKeySpec

class CipherUtil {
    /**
     * the method to check the signature for the data returned from the interface
     * @param content Unsigned data
     * @param sign the signature for content
     * @param publicKey the public of the application
     * @return boolean
     */

    companion object {
        const val TAG: String = "HMS_LOG_CipherUtil"
        const val SIGN_ALGORITHMS: String = "SHA256WithRSA"

        fun doCheck(content: String, sign: String, publicKey: String): Boolean {
            if (TextUtils.isEmpty(publicKey)) {
                Log.e(TAG, "publicKey is null");
                return false
            }
            try {
                val keyFactory = KeyFactory.getInstance("RSA")
                val encodedKey = Base64.decode(publicKey, Base64.DEFAULT)
                val pubKey = keyFactory.generatePublic(X509EncodedKeySpec(encodedKey))

                val signature = Signature.getInstance(SIGN_ALGORITHMS)
                signature.initVerify(pubKey)
                signature.update(content.toByteArray())

                val bverify = signature.verify(Base64.decode(sign, Base64.DEFAULT))
                return bverify
            } catch (e: NoSuchAlgorithmException) {
                Log.e(TAG, "doCheck NoSuchAlgorithmException" + e);
            } catch (e: InvalidKeySpecException) {
                Log.e(TAG, "doCheck InvalidKeySpecException" + e);
            } catch (e: InvalidKeyException) {
                Log.e(TAG, "doCheck InvalidKeyException" + e);
            } catch (e: SignatureException) {
                Log.e(TAG, "doCheck SignatureException" + e);
            } catch (e: UnsupportedEncodingException) {
                Log.e(TAG, "doCheck UnsupportedEncodingException" + e);
            }
            return false
        }
    }
}