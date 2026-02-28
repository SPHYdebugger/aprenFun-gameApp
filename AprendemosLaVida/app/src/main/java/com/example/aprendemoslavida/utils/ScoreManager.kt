package com.example.aprendemoslavida.utils

import android.content.Context
import com.example.aprendemoslavida.R
import com.example.aprendemoslavida.model.ScoreEntry
import org.json.JSONArray
import org.json.JSONObject

class ScoreManager(private val context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun saveScore(entry: ScoreEntry) {
        val all = loadScores().toMutableList()
        all.add(entry)
        val array = JSONArray()
        all.forEach {
            val obj = JSONObject()
            obj.put("name", it.name)
            obj.put("date", it.date)
            obj.put("score", it.score)
            obj.put("mode", it.mode)
            array.put(obj)
        }
        prefs.edit().putString(KEY_ENTRIES, array.toString()).apply()
    }

    fun getTopScores(limit: Int = 10, mode: String = MODE_NATURAL): List<ScoreEntry> {
        return loadScores()
            .filter { it.mode == mode }
            .sortedWith(compareByDescending<ScoreEntry> { it.score }.thenByDescending { it.date })
            .take(limit)
    }

    fun clearScores(mode: String) {
        val remaining = loadScores().filter { it.mode != mode }
        val array = JSONArray()
        remaining.forEach {
            val obj = JSONObject()
            obj.put("name", it.name)
            obj.put("date", it.date)
            obj.put("score", it.score)
            obj.put("mode", it.mode)
            array.put(obj)
        }
        prefs.edit().putString(KEY_ENTRIES, array.toString()).apply()
    }

    private fun loadScores(): List<ScoreEntry> {
        val raw = prefs.getString(KEY_ENTRIES, null) ?: return emptyList()
        val defaultName = context.getString(R.string.score_default_name)
        val array = JSONArray(raw)
        val list = ArrayList<ScoreEntry>(array.length())
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            list.add(
                ScoreEntry(
                    name = obj.optString("name", defaultName),
                    date = obj.optString("date", ""),
                    score = obj.optInt("score", 0),
                    mode = obj.optString("mode", MODE_NATURAL)
                )
            )
        }
        return list
    }

    companion object {
        private const val PREFS_NAME = "aprendemos_scores"
        private const val KEY_ENTRIES = "entries"
        const val MODE_NATURAL = "natural"
        const val MODE_MATH = "math"
        const val MODE_ENGLISH = "english"
        const val MODE_SOCIAL = "social"
    }
}
