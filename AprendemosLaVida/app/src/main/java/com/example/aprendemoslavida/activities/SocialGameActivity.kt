package com.example.aprendemoslavida.activities

import com.example.aprendemoslavida.data.model.SubjectMapper

class SocialGameActivity : RemoteGameActivity() {
    override val backendSubjectCode: String = SubjectMapper.CODE_SOCIAL
    override val resultMode: String = SubjectMapper.CODE_SOCIAL

    companion object {
        const val EXTRA_TOPIC = "extra_topic"
    }
}
