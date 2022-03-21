package com.mfhelm.podautoscaler.scaler.config

class InvalidConfigException(message: String, cause: Throwable?) : RuntimeException(message, cause) {
    constructor(message: String) : this(message, null)
}
