package com.example.aprendemoslavida.activities

import com.example.aprendemoslavida.data.model.SubjectMapper

class AddSubMathGameActivity : RemoteGameActivity() {
    override val backendSubjectCode: String = SubjectMapper.CODE_MATH
    override val backendTopicCode: String = SubjectMapper.TOPIC_ADD_SUB
    override val resultMode: String = SubjectMapper.CODE_MATH
}
