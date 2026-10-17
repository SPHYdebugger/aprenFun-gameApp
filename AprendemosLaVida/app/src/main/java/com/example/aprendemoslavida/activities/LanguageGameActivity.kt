package com.example.aprendemoslavida.activities

import com.example.aprendemoslavida.data.model.SubjectMapper

class LanguageGameActivity : RemoteGameActivity() {
    override val backendSubjectCode: String = SubjectMapper.CODE_LANGUAGE
    override val resultMode: String = SubjectMapper.CODE_LANGUAGE
}

