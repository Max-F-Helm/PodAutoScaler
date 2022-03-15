package com.mfhelm.podautoscaler.test

import com.mfhelm.podautoscaler.connection.KubernetesConnection
import com.mfhelm.podautoscaler.connection.MessageQueueConnection
import com.mfhelm.podautoscaler.scaler.Scaler
import com.mfhelm.podautoscaler.scaler.config.*
import io.mockk.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ScalerTest {

    private val queueConnection = mockk<MessageQueueConnection>(relaxed = true)
    private val kubernetesConnection = mockk<KubernetesConnection>(relaxed = true)

    private val limitConfig = ScalerConfig("A", "VH-A", "Q-A", "NS-A", "P-A", 10,
        LimitRuleset("limit", ArrayList<LimitRule>().apply{
            add(LimitRule(0, 1))
            add(LimitRule(100, 2))
            add(LimitRule(200, 3))
        }))
    private val linearScaleConfig = ScalerConfig("B", "VH-B", "Q-B", "NS-B", "P-B", 10,
        LinearScaleRuleset("linearScale",
            LinearScaleRule(0.1, 2, 2, 20)
        )
    )
    private val logarithmicScaleConfig = ScalerConfig("C", "VH-C", "Q-C", "NS-C", "P-C", 10,
        LogarithmicScaleRuleset("logScale",
            LogarithmicScaleRule(10.0, 2, 2, 20)
        )
    )

    @BeforeEach
    fun resetMocks(){
        clearAllMocks()
    }

    @Test
    fun scaleLimit(){
        val podCount = slot<Int>()
        every { kubernetesConnection.setPodCount(limitConfig.podNamespace, limitConfig.pod, capture(podCount)) }.answers {}
        every { queueConnection.getQueueMessageCount(limitConfig.queueVirtualHost, limitConfig.queueName) } returns 105
        every { kubernetesConnection.getPodCount(limitConfig.podNamespace, limitConfig.pod) } returns 1

        val scaler = Scaler(queueConnection, kubernetesConnection)
        scaler.config = limitConfig

        scaler.run()

        assertTrue(podCount.isCaptured)
        assertEquals(2, podCount.captured)
    }

    @Test
    fun scaleLimit_noScaling(){
        val podCount = slot<Int>()
        every { kubernetesConnection.setPodCount(limitConfig.podNamespace, limitConfig.pod, capture(podCount)) }.answers {}
        every { queueConnection.getQueueMessageCount(limitConfig.queueVirtualHost, limitConfig.queueName) } returns 105
        every { kubernetesConnection.getPodCount(limitConfig.podNamespace, limitConfig.pod) } returns 2

        val scaler = Scaler(queueConnection, kubernetesConnection)
        scaler.config = limitConfig

        scaler.run()

        assertFalse(podCount.isCaptured)
    }

    @Test
    fun scaleLinearScale(){
        val podCount = slot<Int>()
        every { kubernetesConnection.setPodCount(linearScaleConfig.podNamespace, linearScaleConfig.pod, capture(podCount)) }.answers {}
        every { queueConnection.getQueueMessageCount(linearScaleConfig.queueVirtualHost, linearScaleConfig.queueName) } returns 105
        every { kubernetesConnection.getPodCount(linearScaleConfig.podNamespace, linearScaleConfig.pod) } returns 2

        val scaler = Scaler(queueConnection, kubernetesConnection)
        scaler.config = linearScaleConfig

        scaler.run()

        assertTrue(podCount.isCaptured)
        assertEquals(11, podCount.captured)
    }

    @Test
    fun scaleLinearScale_noScaling(){
        val podCount = slot<Int>()
        every { kubernetesConnection.setPodCount(linearScaleConfig.podNamespace, linearScaleConfig.pod, capture(podCount)) }.answers {}
        every { queueConnection.getQueueMessageCount(linearScaleConfig.queueVirtualHost, linearScaleConfig.queueName) } returns 32
        every { kubernetesConnection.getPodCount(linearScaleConfig.podNamespace, linearScaleConfig.pod) } returns 2

        val scaler = Scaler(queueConnection, kubernetesConnection)
        scaler.config = linearScaleConfig

        scaler.run()

        assertFalse(podCount.isCaptured)
    }

    @Test
    fun scaleLogarithmicScale(){
        val podCount = slot<Int>()
        every { kubernetesConnection.setPodCount(logarithmicScaleConfig.podNamespace, logarithmicScaleConfig.pod, capture(podCount)) }.answers {}
        every { queueConnection.getQueueMessageCount(logarithmicScaleConfig.queueVirtualHost, logarithmicScaleConfig.queueName) } returns 105
        every { kubernetesConnection.getPodCount(logarithmicScaleConfig.podNamespace, logarithmicScaleConfig.pod) } returns 5

        val scaler = Scaler(queueConnection, kubernetesConnection)
        scaler.config = logarithmicScaleConfig

        scaler.run()

        assertTrue(podCount.isCaptured)
        assertEquals(2, podCount.captured)
    }

    @Test
    fun scaleLogarithmicScale_noScaling(){
        val podCount = slot<Int>()
        every { kubernetesConnection.setPodCount(logarithmicScaleConfig.podNamespace, logarithmicScaleConfig.pod, capture(podCount)) }.answers {}
        every { queueConnection.getQueueMessageCount(logarithmicScaleConfig.queueVirtualHost, logarithmicScaleConfig.queueName) } returns 32
        every { kubernetesConnection.getPodCount(logarithmicScaleConfig.podNamespace, logarithmicScaleConfig.pod) } returns 2

        val scaler = Scaler(queueConnection, kubernetesConnection)
        scaler.config = logarithmicScaleConfig

        scaler.run()

        assertFalse(podCount.isCaptured)
    }
}