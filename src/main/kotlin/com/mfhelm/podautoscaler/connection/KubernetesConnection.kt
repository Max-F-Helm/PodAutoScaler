package com.mfhelm.podautoscaler.connection

import io.fabric8.kubernetes.api.model.apps.Deployment
import io.fabric8.kubernetes.client.KubernetesClient
import io.fabric8.kubernetes.client.dsl.RollableScalableResource
import org.springframework.stereotype.Component

@Component
internal class KubernetesConnection(private val clientConfig: KubernetesClient) {

    internal fun getPodCount(namespace: String, deploymentName: String): Int{
        return getDeployment(namespace, deploymentName).scale().status.replicas
    }

    internal fun setPodCount(namespace: String, deploymentName: String, count: Int){
        getDeployment(namespace, deploymentName).scale(count)
    }

    private fun getDeployment(namespace: String, deploymentName: String): RollableScalableResource<Deployment> {
        return clientConfig.apps().deployments()
            .inNamespace(namespace)
            .withName(deploymentName)
    }
}