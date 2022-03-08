package com.mfhelm.podautoscaler

import org.slf4j.LoggerFactory

internal fun getLogger(classInstance: Class<out Any>) = LoggerFactory.getLogger(classInstance) ?: error(
    "Could not get logger!"
)
