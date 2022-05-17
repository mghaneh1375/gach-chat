package com.example.websocketdemo.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry
                .addEndpoint("/ws")
//                .setAllowedOrigins("https://okft.org")
//                .setAllowedOrigins("http://185.239.106.26")
//                .setAllowedOrigins("http://localhost:3000")
//                .setAllowedOrigins("http://localhost:8088")
//                .setAllowedOrigins("http://127.0.0.1:5500")
		.setAllowedOriginPatterns("*")
                .withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/chat");
        config.setApplicationDestinationPrefixes("/app");
        config.setUserDestinationPrefix("/user");
    }

//	@Override
//	public boolean configureMessageConverters(List<MessageConverter> messageConverters) {
//		DefaultContentTypeResolver resolver = new DefaultContentTypeResolver();
//		resolver.setDefaultMimeType(MimeTypeUtils.APPLICATION_JSON);
//		MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
//		converter.setObjectMapper(new ObjectMapper());
//		converter.setContentTypeResolver(resolver);
//		messageConverters.add(converter);
//		return false;
//	}

}
