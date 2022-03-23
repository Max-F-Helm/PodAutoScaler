package com.mfhelm.podautoscaler.scaler.config

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import java.util.Objects

@JsonDeserialize
internal class ScalerConfig(
    label: String?,
    deploymentNamespace: String?,
    internal val deployment: String,
    @JsonDeserialize(using = IntervalValueDeserializer::class) internal val interval: Long,
    internal val queues: Set<QueueConfig>
) {

    internal val label: String
    internal val deploymentNamespace: String

    init {
        this.label = label ?: DEFAULT_LABEL

        this.deploymentNamespace = deploymentNamespace
            ?: System.getProperty("NAMESPACE")
                    ?: throw NoSuchElementException("missing parameter 'deploymentNamespace' and no default is present")
    }

    override fun equals(other: Any?): Boolean {
        if(other !is ScalerConfig){
            return false
        }

        return label == other.label
                && deploymentNamespace == other.deploymentNamespace
                && deployment == other.deployment
                && interval == other.interval
                && queues == other.queues
    }

    override fun hashCode(): Int {
        return Objects.hash(ScalerConfig::class.qualifiedName, label, deploymentNamespace, deployment, interval, queues)
    }

    override fun toString(): String {
        return "ScalerConfig:{" +
                "label: $label, " +
                "deploymentNamespace: $deploymentNamespace, " +
                "deployment: $deployment, " +
                "interval: $interval, " +
                "queues: $queues}"
    }
}
