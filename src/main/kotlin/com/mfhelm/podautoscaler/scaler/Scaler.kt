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
            val messageCount = messageQueueConnection.getQueueMessageCount(config.queueVirtualHost, config.queueName)
            val currentPodCount = kubernetesConnection.getPodCount(config.podNamespace, config.pod)
            val newPodCount = config.ruleset.computePodCount(messageCount, currentPodCount)
            if (newPodCount != -1) {
                kubernetesConnection.setPodCount(config.podNamespace, config.pod, newPodCount)
                logger.info("${config.label} scaled from $currentPodCount to $newPodCount")
            }
        }catch (e: Exception){
            logger.error("exception for ${config.label}", e)
        }
    }
}