package com.example.websocketdemo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.handler.invocation.HandlerMethodArgumentResolver;
import org.springframework.messaging.handler.invocation.HandlerMethodReturnValueHandler;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.support.AbstractSubscribableChannel;

import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.*;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurationSupport;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.util.List;

import static com.example.websocketdemo.utility.Statics.DEV_MODE;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig extends WebSocketMessageBrokerConfigurationSupport implements WebSocketMessageBrokerConfigurer {


    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {

        if(DEV_MODE)
            registry
                    .addEndpoint("/ws")
                    .setAllowedOriginPatterns("*")
                    .withSockJS();
        else
            registry
                    .addEndpoint("/ws")
                    .setAllowedOrigins("https://okft.org")
                    .withSockJS();

    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {

        ThreadPoolTaskScheduler te = new ThreadPoolTaskScheduler();
        te.setPoolSize(1);
        te.setThreadNamePrefix("wss-heartbeat-thread-");
        te.initialize();

        config.enableSimpleBroker("/chat")
                .setHeartbeatValue(new long[]{30000, 30000})
                .setTaskScheduler(te);;
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

    @Override
    public void configureWebSocketTransport(WebSocketTransportRegistration registry) {
        super.configureWebSocketTransport(registry);
    }

    @Override
    public boolean configureMessageConverters(List<MessageConverter> messageConverters) {
        return super.configureMessageConverters(messageConverters);
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        super.configureClientInboundChannel(registration);
    }

    @Override
    public void configureClientOutboundChannel(ChannelRegistration registration) {
        super.configureClientOutboundChannel(registration);
    }


    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> argumentResolvers) {
        super.addArgumentResolvers(argumentResolvers);
    }

    @Override
    public void addReturnValueHandlers(List<HandlerMethodReturnValueHandler> returnValueHandlers) {
        super.addReturnValueHandlers(returnValueHandlers);
    }

    @Primary
    @Order(Ordered.HIGHEST_PRECEDENCE)
    @Bean
    public WebSocketHandler subProtocolWebSocketHandler(AbstractSubscribableChannel clientInboundChannel, AbstractSubscribableChannel clientOutboundChannel) {
        return new CustomSubProtocolWebSocketHandler(clientInboundChannel, clientOutboundChannel);
    }
}
