package com.example.aprendemoslavida.data.storage

import android.content.Context
import android.content.SharedPreferences
import java.util.UUID

class IdempotencyStorage(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getOrCreateKey(actionKey: String): String {
        var key = prefs.getString(actionKey, null)
        if (key.isNullOrBlank()) {
            key = UUID.randomUUID().toString()
            // The request must never leave the process before the key is durable.
            prefs.edit().putString(actionKey, key).commit()
        }
        return checkNotNull(key)
    }

    fun clearKey(actionKey: String) {
        prefs.edit().remove(actionKey).commit()
    }

    companion object {
        private const val PREFS_NAME = "idempotency_keys_prefs"
    }
}
