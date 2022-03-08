package com.mfhelm.podautoscaler.connection

import io.fabric8.kubernetes.client.DefaultKubernetesClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
internal open class KubernetesClientConfig {

    @Bean
    open fun kubernetesClient() = DefaultKubernetesClient()
}