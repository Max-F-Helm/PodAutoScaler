package com.mfhelm.podautoscaler.scaler

import com.mfhelm.podautoscaler.connection.KubernetesConnection
import com.mfhelm.podautoscaler.connection.MessageQueueConnection
import com.mfhelm.podautoscaler.getLogger
import com.mfhelm.podautoscaler.scaler.config.QueueConfig
import com.mfhelm.podautoscaler.scaler.config.ScalerConfig

internal class Scaler(
    private val messageQueueConnection: MessageQueueConnection,
    private val kubernetesConnection: KubernetesConnection,
    internal val config: ScalerConfig // internal for tests
) : Runnable {

    private val logger = getLogger(Scaler::class.java)

    override fun run() {
        try {
            val currentPodCount = kubernetesConnection.getPodCount(config.deploymentNamespace, config.deployment)

            logger.trace("${config.label}: updating podCount (current podCount: $currentPodCount)")

            val newPodCount = config.queues.map(computeMapper(currentPodCount)).maxOf { it }

            if (newPodCount.count != currentPodCount) {
                kubernetesConnection.setPodCount(config.deploymentNamespace, config.deployment, newPodCount.count)
                logger.info(
                    "${config.label} scaled from $currentPodCount to ${newPodCount.count}" +
                        " (by ${newPodCount.byQueueName} with ${newPodCount.byQueueMessages} messages)"
                )
            }
        } catch (e: Exception) {
            logger.error("exception for ${config.label}", e)
        }
    }

    private fun computeMapper(currentPodCount: Int): (QueueConfig) -> ComputedPodCount {
        return { queue ->
            val messageCount = messageQueueConnection.getQueueMessageCount(queue.virtualHost, queue.name)
            val computedCount = queue.ruleset.computePodCount(messageCount, currentPodCount)

            logger.trace(
                "${config.label}: computed podCount: $computedCount" +
                    " for messageCount $messageCount of queue ${queue.virtualHost}/${queue.name}" +
                    " with ruleset ${queue.ruleset}"
            )

            ComputedPodCount(computedCount, "${queue.virtualHost}/${queue.name}", messageCount)
        }
    }
}

private data class ComputedPodCount(
    val count: Int,
    val byQueueName: String,
    val byQueueMessages: Int
) : Comparable<ComputedPodCount> {

    override fun compareTo(other: ComputedPodCount): Int {
        return count.compareTo(other.count)
    }
}
