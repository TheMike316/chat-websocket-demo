package com.example.chatwebsocketdemo.controller

import com.example.chatwebsocketdemo.model.Chat
import com.example.chatwebsocketdemo.model.ChatMessage
import com.example.chatwebsocketdemo.model.MessageType
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.messaging.handler.annotation.DestinationVariable
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.messaging.simp.SimpMessageHeaderAccessor
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.messaging.simp.stomp.StompHeaderAccessor
import org.springframework.stereotype.Component
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.socket.messaging.SessionConnectedEvent
import org.springframework.web.socket.messaging.SessionDisconnectEvent
import java.util.*

private const val MESSAGE_BROKER_TOPIC = "/topic/public"

// to simulate an environment with multiple user chats
private val CHAT_ID = UUID.randomUUID()

@Controller
class ChatController(private val websocketTemplate: SimpMessagingTemplate) {

    @GetMapping("/chat")
    @ResponseBody
    fun getChat() = Chat(CHAT_ID)

    // we directly send the message to the broker to be published to all subscribers
    @MessageMapping("/chat/{chatId}.sendMessage")
//    @SendTo(MESSAGE_BROKER_TOPIC)
    fun sendMessage(@DestinationVariable chatId: UUID, @Payload chatMessage: ChatMessage) {
        websocketTemplate.convertAndSend("$MESSAGE_BROKER_TOPIC/$chatId", chatMessage)
    }

    @MessageMapping("/chat/{chatId}.addUser")
//    @SendTo(MESSAGE_BROKER_TOPIC)
    fun addUser(@DestinationVariable chatId: UUID, @Payload chatMessage: ChatMessage,
                headerAccessor: SimpMessageHeaderAccessor) {
        // add sender name to session and send the message to the broker
        headerAccessor.sessionAttributes!!["username"] = chatMessage.sender
        headerAccessor.sessionAttributes!!["chatId"] = chatId
        websocketTemplate.convertAndSend("$MESSAGE_BROKER_TOPIC/$chatId", chatMessage)
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
        val sessionAttributes = StompHeaderAccessor.wrap(event.message).sessionAttributes

        (sessionAttributes?.get("username") as? String)
                ?.let { username ->
                    LOGGER.info("User $username disconnected")

                    (sessionAttributes["chatId"] as? UUID)
                            ?.let { chatId ->
                                messagingTemplate.convertAndSend("$MESSAGE_BROKER_TOPIC/$chatId",
                                        ChatMessage(MessageType.LEAVE, username))
                            }
                } ?: LOGGER.error("Disconnect Event without username or chatId!")
    }
}