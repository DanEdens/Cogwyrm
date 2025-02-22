package com.cogwyrm.app.tasker

import android.os.Bundle

interface TaskerPluginInput {
    fun validate(): Boolean
    fun toBundle(): Bundle
}
