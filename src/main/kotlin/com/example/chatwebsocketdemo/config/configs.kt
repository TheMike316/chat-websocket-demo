package com.example.chatwebsocketdemo.config

import org.springframework.context.annotation.Configuration
import org.springframework.messaging.simp.config.MessageBrokerRegistry
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker
import org.springframework.web.socket.config.annotation.StompEndpointRegistry
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer

@Configuration
@EnableWebSocketMessageBroker
class WebSocketConfig : WebSocketMessageBrokerConfigurer {

    override fun registerStompEndpoints(registry: StompEndpointRegistry) {
        registry.addEndpoint("/ws").setAllowedOrigins("*").withSockJS()
    }

    override fun configureMessageBroker(registry: MessageBrokerRegistry) {
        registry.apply {
            // /app seems to be industry standard
            setApplicationDestinationPrefixes("/app")

            // again, /topic seems to be industry standard
            // enableSimpleBroker("/topic") ...for in-memory broker

            // to use rabbit
            enableStompBrokerRelay("/topic")
                    .setRelayHost("localhost")
                    .setRelayPort(61613)
                    .setClientLogin("guest")
                    .setClientPasscode("guest")
        }
    }
}