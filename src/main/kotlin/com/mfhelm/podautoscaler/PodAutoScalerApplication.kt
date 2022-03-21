package com.mfhelm.podautoscaler

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
internal class PodAutoScalerApplication

fun main(args: Array<String>){
    runApplication<PodAutoScalerApplication>(*args)
}
