package com.mfhelm.podautoscaler.connection

import org.springframework.amqp.core.AmqpAdmin
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory
import org.springframework.amqp.rabbit.core.RabbitAdmin
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
internal class MessageQueueConnection {

    @Value("\${spring.rabbitmq.host}")
    private lateinit var rabbitHost: String
    @Value("\${spring.rabbitmq.port}")
    private var rabbitPort: Int = 0
    @Value("\${spring.rabbitmq.username}")
    private lateinit var rabbitUser: String
    @Value("\${spring.rabbitmq.password}")
    private lateinit var rabbitPassword: String

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