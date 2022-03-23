package com.mfhelm.podautoscaler.scaler.config.ruleset

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.mfhelm.podautoscaler.scaler.config.RulesetDeserializer

@JsonDeserialize(using = RulesetDeserializer::class)
internal interface Ruleset {

    /**
     * computes the new pod-count using the rules in this ruleset
     * @param messageCount count of messages in the queue
     * @param currentPodCount current count of pods
     * @return the new pod-count
     */
    fun computePodCount(messageCount: Int, currentPodCount: Int): Int
}
