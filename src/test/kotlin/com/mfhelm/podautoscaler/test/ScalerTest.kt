package com.mfhelm.podautoscaler.test

import com.mfhelm.podautoscaler.connection.KubernetesConnection
import com.mfhelm.podautoscaler.connection.MessageQueueConnection
import com.mfhelm.podautoscaler.scaler.Scaler
import com.mfhelm.podautoscaler.scaler.config.QueueConfig
import com.mfhelm.podautoscaler.scaler.config.ScalerConfig
import com.mfhelm.podautoscaler.scaler.config.ruleset.LimitRule
import com.mfhelm.podautoscaler.scaler.config.ruleset.LimitRuleset
import com.mfhelm.podautoscaler.scaler.config.ruleset.LinearScaleRule
import com.mfhelm.podautoscaler.scaler.config.ruleset.LinearScaleRuleset
import com.mfhelm.podautoscaler.scaler.config.ruleset.LogarithmicScaleRule
import com.mfhelm.podautoscaler.scaler.config.ruleset.LogarithmicScaleRuleset
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ScalerTest {

    private val queueConnection = mockk<MessageQueueConnection>(relaxed = true)
    private val kubernetesConnection = mockk<KubernetesConnection>(relaxed = true)

    private val limitConfig = ScalerConfig(
        "A", "NS-A", "P-A", 10,
        setOf(
            QueueConfig(
                "VH-A", "Q-A",
                LimitRuleset(
                    ArrayList<LimitRule>().apply {
                        add(LimitRule(0, 1))
                        add(LimitRule(100, 2))
                        add(LimitRule(200, 3))
                    }
                )
            )
        )
    )
    private val linearScaleConfig = ScalerConfig(
        "B", "NS-B", "P-B", 10,
        setOf(
            QueueConfig(
                "VH-B", "Q-B",
                LinearScaleRuleset(
                    LinearScaleRule(0.1, 2, 2, 20)
                )
            )
        )
    )
    private val logarithmicScaleConfig = ScalerConfig(
        "C", "NS-C", "P-C", 10,
        setOf(
            QueueConfig(
                "VH-C", "Q-C",
                LogarithmicScaleRuleset(
                    LogarithmicScaleRule(10.0, 2, 2, 20)
                )
            )
        )
    )

    private val multiQueueQueues = listOf(
        QueueConfig(
            "VH-D1", "Q-D1",
            LimitRuleset(
                ArrayList<LimitRule>().apply {
                    add(LimitRule(0, 1))
                    add(LimitRule(100, 2))
                    add(LimitRule(200, 3))
                }
            )
        ),
        QueueConfig(
            "VH-D2", "Q-D2",
            LinearScaleRuleset(LinearScaleRule(0.005, 1, 1, 20))
        )
    )
    private val multiQueueConfig = ScalerConfig(
        "D", "NS-D", "P-D", 10,
        multiQueueQueues.toSet()
    )

    @BeforeEach
    fun resetMocks() {
        clearAllMocks()
    }

    @Test
    fun scaleLimit() {
        val queue = limitConfig.queues.first()

        val podCount = slot<Int>()
        every {
            kubernetesConnection.setPodCount(
                limitConfig.deploymentNamespace,
                limitConfig.deployment,
                capture(podCount)
            )
        }.answers {}
        every { queueConnection.getQueueMessageCount(queue.virtualHost, queue.name) } returns 105
        every { kubernetesConnection.getPodCount(limitConfig.deploymentNamespace, limitConfig.deployment) } returns 1

        val scaler = Scaler(queueConnection, kubernetesConnection, limitConfig)

        scaler.run()

        assertTrue(podCount.isCaptured)
        assertEquals(2, podCount.captured)
    }

    @Test
    fun scaleLimit_noScaling() {
        val queue = limitConfig.queues.first()

        val podCount = slot<Int>()
        every {
            kubernetesConnection.setPodCount(
                limitConfig.deploymentNamespace,
                limitConfig.deployment,
                capture(podCount)
            )
        }.answers {}
        every { queueConnection.getQueueMessageCount(queue.virtualHost, queue.name) } returns 105
        every { kubernetesConnection.getPodCount(limitConfig.deploymentNamespace, limitConfig.deployment) } returns 2

        val scaler = Scaler(queueConnection, kubernetesConnection, limitConfig)

        scaler.run()

        assertFalse(podCount.isCaptured)
    }

    @Test
    fun scaleLinearScale() {
        val queue = linearScaleConfig.queues.first()

        val podCount = slot<Int>()
        every {
            kubernetesConnection.setPodCount(
                linearScaleConfig.deploymentNamespace,
                linearScaleConfig.deployment,
                capture(podCount)
            )
        }.answers {}
        every { queueConnection.getQueueMessageCount(queue.virtualHost, queue.name) } returns 105
        every {
            kubernetesConnection.getPodCount(
                linearScaleConfig.deploymentNamespace,
                linearScaleConfig.deployment
            )
        } returns 2

        val scaler = Scaler(queueConnection, kubernetesConnection, linearScaleConfig)

        scaler.run()

        assertTrue(podCount.isCaptured)
        assertEquals(11, podCount.captured)
    }

    @Test
    fun scaleLinearScale_noScaling() {
        val queue = linearScaleConfig.queues.first()

        val podCount = slot<Int>()
        every {
            kubernetesConnection.setPodCount(
                linearScaleConfig.deploymentNamespace,
                linearScaleConfig.deployment,
                capture(podCount)
            )
        }.answers {}
        every { queueConnection.getQueueMessageCount(queue.virtualHost, queue.name) } returns 32
        every {
            kubernetesConnection.getPodCount(
                linearScaleConfig.deploymentNamespace,
                linearScaleConfig.deployment
            )
        } returns 2

        val scaler = Scaler(queueConnection, kubernetesConnection, linearScaleConfig)

        scaler.run()

        assertFalse(podCount.isCaptured)
    }

    @Test
    fun scaleLogarithmicScale() {
        val queue = logarithmicScaleConfig.queues.first()

        val podCount = slot<Int>()
        every {
            kubernetesConnection.setPodCount(
                logarithmicScaleConfig.deploymentNamespace,
                logarithmicScaleConfig.deployment,
                capture(podCount)
            )
        }.answers {}
        every { queueConnection.getQueueMessageCount(queue.virtualHost, queue.name) } returns 105
        every {
            kubernetesConnection.getPodCount(
                logarithmicScaleConfig.deploymentNamespace,
                logarithmicScaleConfig.deployment
            )
        } returns 5

        val scaler = Scaler(queueConnection, kubernetesConnection, logarithmicScaleConfig)

        scaler.run()

        assertTrue(podCount.isCaptured)
        assertEquals(2, podCount.captured)
    }

    @Test
    fun scaleLogarithmicScale_noScaling() {
        val queue = logarithmicScaleConfig.queues.first()

        val podCount = slot<Int>()
        every {
            kubernetesConnection.setPodCount(
                logarithmicScaleConfig.deploymentNamespace,
                logarithmicScaleConfig.deployment,
                capture(podCount)
            )
        }.answers {}
        every { queueConnection.getQueueMessageCount(queue.virtualHost, queue.name) } returns 32
        every {
            kubernetesConnection.getPodCount(
                logarithmicScaleConfig.deploymentNamespace,
                logarithmicScaleConfig.deployment
            )
        } returns 2

        val scaler = Scaler(queueConnection, kubernetesConnection, logarithmicScaleConfig)

        scaler.run()

        assertFalse(podCount.isCaptured)
    }

    @Test
    fun scaleMultiQueueA() {
        val podCount = slot<Int>()

        every {
            kubernetesConnection.getPodCount(
                multiQueueConfig.deploymentNamespace,
                multiQueueConfig.deployment
            )
        } returns 0
        every {
            queueConnection.getQueueMessageCount(
                multiQueueQueues[0].virtualHost,
                multiQueueQueues[0].name
            )
        } returns 190
        every {
            queueConnection.getQueueMessageCount(
                multiQueueQueues[1].virtualHost,
                multiQueueQueues[1].name
            )
        } returns 200

        val scaler = Scaler(queueConnection, kubernetesConnection, multiQueueConfig)

        every {
            kubernetesConnection.setPodCount(
                multiQueueConfig.deploymentNamespace,
                multiQueueConfig.deployment,
                capture(podCount)
            )
        }.answers {}

        scaler.run()

        assertTrue(podCount.isCaptured)
        assertEquals(2, podCount.captured)
    }

    @Test
    fun scaleMultiQueueB() {
        val podCount = slot<Int>()

        val scaler = Scaler(queueConnection, kubernetesConnection, multiQueueConfig)

        every {
            kubernetesConnection.getPodCount(
                multiQueueConfig.deploymentNamespace,
                multiQueueConfig.deployment
            )
        } returns 0
        every {
            queueConnection.getQueueMessageCount(
                multiQueueQueues[0].virtualHost,
                multiQueueQueues[0].name
            )
        } returns 190
        every {
            queueConnection.getQueueMessageCount(
                multiQueueQueues[1].virtualHost,
                multiQueueQueues[1].name
            )
        } returns 5000

        every {
            kubernetesConnection.setPodCount(
                multiQueueConfig.deploymentNamespace,
                multiQueueConfig.deployment,
                capture(podCount)
            )
        }.answers {}

        scaler.run()

        assertTrue(podCount.isCaptured)
        assertEquals(20, podCount.captured)
    }

    @Test
    fun scaleMultiQueueC() {
        val podCount = slot<Int>()

        val scaler = Scaler(queueConnection, kubernetesConnection, multiQueueConfig)

        every {
            kubernetesConnection.getPodCount(
                multiQueueConfig.deploymentNamespace,
                multiQueueConfig.deployment
            )
        } returns 6
        every {
            queueConnection.getQueueMessageCount(
                multiQueueQueues[0].virtualHost,
                multiQueueQueues[0].name
            )
        } returns 190
        every {
            queueConnection.getQueueMessageCount(
                multiQueueQueues[1].virtualHost,
                multiQueueQueues[1].name
            )
        } returns 500

        every {
            kubernetesConnection.setPodCount(
                multiQueueConfig.deploymentNamespace,
                multiQueueConfig.deployment,
                capture(podCount)
            )
        }.answers {}

        scaler.run()

        assertTrue(podCount.isCaptured)
        assertEquals(3, podCount.captured)
    }
}
