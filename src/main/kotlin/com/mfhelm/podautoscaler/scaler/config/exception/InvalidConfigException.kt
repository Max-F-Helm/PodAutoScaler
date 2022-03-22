package com.mfhelm.podautoscaler.scaler.config.exception

class InvalidConfigException(message: String, cause: Throwable?) : RuntimeException(message, cause) {
    constructor(message: String) : this(message, null)
}
