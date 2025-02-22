package com.cogwyrm.app.tasker

import android.os.Bundle

interface TaskerPluginOutput {
    fun toBundle(): Bundle
}
