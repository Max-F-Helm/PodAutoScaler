package com.mfhelm.podautoscaler.connection

import org.springframework.amqp.core.AmqpAdmin
import org.springframework.stereotype.Component

@Component
internal class MessageQueueConnection(private val amqpAdmin: AmqpAdmin) {

    fun getQueueMessageCount(queueName: String): Int{
        return amqpAdmin.getQueueInfo(queueName)?.messageCount
            ?: throw IllegalArgumentException("queue with name $queueName not found")
    }
}