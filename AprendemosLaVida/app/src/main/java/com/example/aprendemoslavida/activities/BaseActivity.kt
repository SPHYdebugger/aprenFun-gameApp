package com.example.aprendemoslavida.activities

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import com.example.aprendemoslavida.utils.LocaleManager

open class BaseActivity : AppCompatActivity() {
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleManager.applyLocale(newBase))
    }
}
