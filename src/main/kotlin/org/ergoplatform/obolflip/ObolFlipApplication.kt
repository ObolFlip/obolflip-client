package org.ergoplatform.obolflip

import com.fasterxml.jackson.databind.ObjectMapper
import okhttp3.OkHttpClient
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.web.ServerProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableConfigurationProperties(Config::class)
@EnableScheduling
class ObolFlipApplication(
    private val config: Config,
    private val serverProperties: ServerProperties,
) {
    private val logger = LoggerFactory.getLogger(ObolFlipApplication::class.java)

    private val okHttpClient = OkHttpClient()

    init {
        if (config.manageFlip && config.getFeeAddress() != null)
            logger.info("Managing flips, revenue to address ${config.feeAddress}.")
        if (config.feeAddress.isNotBlank() && config.getFeeAddress() == null)
            logger.error("Invalid fee address set in config (${config.feeAddress})")

        if (config.publicurl.isBlank()) {
            val ipAddress = getLocalInetAddress()?.hostAddress
            config.publicurl = "http://$ipAddress:${serverProperties.port}"
            logger.info("No public url set, auto detected address is")
            logger.info("   " + "*".repeat(config.publicurl.length + 4))
            logger.info("   * ${config.publicurl} *")
            logger.info("   " + "*".repeat(config.publicurl.length + 4))
        } else {
            logger.info("public url set to ${config.publicurl}")
        }
    }

    @Bean
    @Primary
    fun objectMapper(): ObjectMapper {
        // enables controller methods annotated with @ResponseBody to directly return
        // Mosaik Actions and elements that will get serialized by Spring automatically
        return org.ergoplatform.mosaik.jackson.MosaikSerializer.getMosaikMapper()
    }

    @Bean
    fun getOkHttpClient(): OkHttpClient {
        return okHttpClient
    }
}

fun main(args: Array<String>) {
    runApplication<ObolFlipApplication>(*args)
}
