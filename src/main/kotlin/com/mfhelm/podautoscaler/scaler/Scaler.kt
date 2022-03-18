package com.mfhelm.podautoscaler.scaler

import com.mfhelm.podautoscaler.connection.KubernetesConnection
import com.mfhelm.podautoscaler.connection.MessageQueueConnection
import com.mfhelm.podautoscaler.getLogger
import com.mfhelm.podautoscaler.scaler.config.ScalerConfig
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

@Component(value = "Scaler")
@Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
internal class Scaler(private val messageQueueConnection: MessageQueueConnection, private val kubernetesConnection: KubernetesConnection) : Runnable {

    internal lateinit var config: ScalerConfig

    private val logger = getLogger(Scaler::class.java)

    override fun run(){
        try {
            val currentPodCount = kubernetesConnection.getPodCount(config.deploymentNamespace, config.deployment)

            logger.trace("${config.label}: updating podCount (current podCount: $currentPodCount)")

            var newPodCount = -1
            var triggeringQueue = ""
            var triggeringMessageCount = 0
            for(queue in config.queueSet) {
                val messageCount = messageQueueConnection.getQueueMessageCount(queue.virtualHost, queue.name)
                val computedCount = queue.ruleset.computePodCount(messageCount, currentPodCount)

                logger.trace("${config.label}: computed podCount: $computedCount for messageCount $messageCount of queue ${queue.virtualHost}/${queue.name} with ruleset ${queue.ruleset}")

                // for comparison -1 must be translated
                val computedCountTranslated = if(computedCount == -1) currentPodCount else computedCount
                if(computedCountTranslated > newPodCount){
                    newPodCount = computedCountTranslated

                    triggeringQueue = "${queue.virtualHost}/${queue.name}"
                    triggeringMessageCount = messageCount
                }
            }

            // back-translate -1
            if(newPodCount == currentPodCount)
                newPodCount = -1

            if (newPodCount != -1) {
                kubernetesConnection.setPodCount(config.deploymentNamespace, config.deployment, newPodCount)
                logger.info("${config.label} scaled from $currentPodCount to $newPodCount (by $triggeringQueue with $triggeringMessageCount messages)")
            }
        }catch (e: Exception){
            logger.error("exception for ${config.label}", e)
        }
    }
}