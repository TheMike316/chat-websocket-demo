package com.example.chatwebsocketdemo.controller

import com.example.chatwebsocketdemo.model.ChatMessage
import com.example.chatwebsocketdemo.model.MessageType
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.messaging.handler.annotation.SendTo
import org.springframework.messaging.simp.SimpMessageHeaderAccessor
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.messaging.simp.stomp.StompHeaderAccessor
import org.springframework.stereotype.Component
import org.springframework.stereotype.Controller
import org.springframework.web.socket.messaging.SessionConnectedEvent
import org.springframework.web.socket.messaging.SessionDisconnectEvent

private const val MESSAGE_BROKER_TOPIC = "/topic/public"

@Controller
class ChatController {

    // we directly send the message to the broker to be published to all subscribers
    @MessageMapping("/chat.sendMessage")
    @SendTo(MESSAGE_BROKER_TOPIC)
    fun sendMessage(@Payload chatMessage: ChatMessage) = chatMessage

    @MessageMapping("/chat.addUser")
    @SendTo(MESSAGE_BROKER_TOPIC)
    fun addUser(@Payload chatMessage: ChatMessage, headerAccessor: SimpMessageHeaderAccessor): ChatMessage {
        // add sender name to session and send the message to the broker
        return chatMessage.also {
            headerAccessor.sessionAttributes!!["username"] = chatMessage.sender
        }
    }
}

@Component
class WebSocketEventListener(private val messagingTemplate: SimpMessagingTemplate) {
    private companion object {
        val LOGGER: Logger = LoggerFactory.getLogger(WebSocketEventListener::class.java)
    }

    @EventListener
    // we already broadcast a join message in addUser()
    fun handleConnect(event: SessionConnectedEvent) = LOGGER.info("Established new web socket connection")

    @EventListener
    fun handleDisconnect(event: SessionDisconnectEvent) {
        (StompHeaderAccessor.wrap(event.message).sessionAttributes
                ?.get("username") as? String)
                ?.let { username ->
                    LOGGER.info("User $username disconnected")
                    messagingTemplate.convertAndSend(MESSAGE_BROKER_TOPIC, ChatMessage(MessageType.LEAVE, username))
                } ?: LOGGER.error("Disconnect Event without username!")
    }
}