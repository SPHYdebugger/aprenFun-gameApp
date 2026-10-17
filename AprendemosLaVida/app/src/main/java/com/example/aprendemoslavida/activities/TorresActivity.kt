package com.example.aprendemoslavida.activities

import com.example.aprendemoslavida.data.model.SubjectMapper

/** Castle mode is a remote CASTLE game session. */
class TorresActivity : RemoteGameActivity() {
    override val backendSubjectCode: String = SubjectMapper.CODE_MATH
    override val backendTopicCode: String = SubjectMapper.TOPIC_CASTLES
    override val resultMode: String = SubjectMapper.CODE_MATH
}
