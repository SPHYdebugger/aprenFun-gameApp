package com.example.aprendemoslavida.activities

import com.example.aprendemoslavida.data.model.SubjectMapper

class GameActivity : RemoteGameActivity() {
    override val backendSubjectCode: String = SubjectMapper.CODE_NATURAL
    override val resultMode: String = SubjectMapper.CODE_NATURAL
}
