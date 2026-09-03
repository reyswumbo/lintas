package com.lintas.app.data

import android.content.Context
import com.lintas.app.util.Constants

object ServerConfig {
    private const val PREFS = "lintas_server"
    private const val KEY_BASE_URL = "base_url"

    fun getBaseUrl(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return prefs.getString(KEY_BASE_URL, null)
            ?: Constants.DEFAULT_BASE_URL
    }

    fun setBaseUrl(context: Context, url: String) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_BASE_URL, url.trimEnd('/')).apply()
    }

    fun resetBaseUrl(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_BASE_URL).apply()
    }
}
