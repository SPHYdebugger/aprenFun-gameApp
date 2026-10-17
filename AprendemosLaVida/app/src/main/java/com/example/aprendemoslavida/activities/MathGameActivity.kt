package com.example.aprendemoslavida.activities

import com.example.aprendemoslavida.data.model.SubjectMapper

class MathGameActivity : RemoteGameActivity() {
    override val backendSubjectCode: String = SubjectMapper.CODE_MATH
    override val backendTopicCode: String = SubjectMapper.TOPIC_MULTIPLICATION
    override val resultMode: String = SubjectMapper.CODE_MATH
}
