package com.example.aprendemoslavida.activities

import com.example.aprendemoslavida.data.model.SubjectMapper

class EnglishGameActivity : RemoteGameActivity() {
    override val backendSubjectCode: String = SubjectMapper.CODE_ENGLISH
    override val resultMode: String = SubjectMapper.CODE_ENGLISH
}
