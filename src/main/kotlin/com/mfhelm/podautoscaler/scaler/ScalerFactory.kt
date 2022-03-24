package com.mfhelm.podautoscaler.scaler

import com.mfhelm.podautoscaler.connection.KubernetesConnection
import com.mfhelm.podautoscaler.connection.MessageQueueConnection
import com.mfhelm.podautoscaler.scaler.config.ScalerConfig
import org.springframework.stereotype.Component

@Component
internal class ScalerFactory(
    private val messageQueueConnection: MessageQueueConnection,
    private val kubernetesConnection: KubernetesConnection
) {

    fun newScaler(config: ScalerConfig): Scaler {
        return Scaler(messageQueueConnection, kubernetesConnection, config)
    }
}
