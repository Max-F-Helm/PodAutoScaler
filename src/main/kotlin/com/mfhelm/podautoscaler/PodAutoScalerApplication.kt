package com.mfhelm.podautoscaler

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
internal class PodAutoScalerApplication

@Suppress("SpreadOperator")
fun main(args: Array<String>) {
    runApplication<PodAutoScalerApplication>(*args)
}
