package com.mfhelm.podautoscaler.connection

import org.springframework.amqp.core.AmqpAdmin
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory
import org.springframework.amqp.rabbit.core.RabbitAdmin
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
internal class MessageQueueConnection(
    @Value("\${spring.rabbitmq.host}") private val rabbitHost: String,
    @Value("\${spring.rabbitmq.port}") private val rabbitPort: Int,
    @Value("\${spring.rabbitmq.username}") private val rabbitUser: String,
    @Value("\${spring.rabbitmq.password}") private val rabbitPassword: String
) {

    private val vHostConnections: HashMap<String, AmqpAdmin> = HashMap()

    fun getQueueMessageCount(vHostName: String, queueName: String): Int{
        val conn = getConnection(vHostName)
        return conn.getQueueInfo(queueName)?.messageCount
            ?: throw IllegalArgumentException("queue with name $queueName not found")
    }

    private fun getConnection(vHost: String): AmqpAdmin{
        return vHostConnections.computeIfAbsent(vHost){
            val factory = CachingConnectionFactory().apply {
                host = rabbitHost
                port = rabbitPort
                username = rabbitUser

                setPassword(rabbitPassword)

                virtualHost = vHost
            }
            return@computeIfAbsent RabbitAdmin(factory)
        }
    }
}