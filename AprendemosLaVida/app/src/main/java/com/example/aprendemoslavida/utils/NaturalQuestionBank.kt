package com.example.aprendemoslavida.utils

import android.content.Context
import com.example.aprendemoslavida.R
import com.example.aprendemoslavida.model.Question
import org.json.JSONArray

object NaturalQuestionBank {
    fun allQuestions(context: Context): List<Question> {
        return try {
            val json = context.resources.openRawResource(R.raw.questions)
                .bufferedReader()
                .use { it.readText() }
            val array = JSONArray(json)
            val list = ArrayList<Question>(array.length())
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val optionsArray = obj.getJSONArray("options")
                val options = ArrayList<String>(optionsArray.length())
                for (j in 0 until optionsArray.length()) {
                    options.add(optionsArray.getString(j))
                }
                list.add(
                    Question(
                        text = obj.getString("text"),
                        options = options,
                        correctIndex = obj.getInt("correctIndex")
                    )
                )
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }
}
