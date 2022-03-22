package com.mfhelm.podautoscaler.scaler.config

import com.mfhelm.podautoscaler.scaler.config.ruleset.Ruleset

internal data class QueueConfig(val virtualHost: String, val name: String, val ruleset: Ruleset)
