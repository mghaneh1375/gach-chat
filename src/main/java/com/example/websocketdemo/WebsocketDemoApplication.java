package com.example.websocketdemo;

import com.example.websocketdemo.db.ChatPresenceRepository;
import com.example.websocketdemo.db.ChatRoomRepository;
import com.example.websocketdemo.db.ClassRepository;
import com.example.websocketdemo.db.UserRepository;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.web.socket.config.annotation.DelegatingWebSocketMessageBrokerConfiguration;
import org.springframework.web.socket.messaging.SubProtocolWebSocketHandler;

import java.util.concurrent.TimeUnit;

@SpringBootApplication
public class WebsocketDemoApplication {

	final static private ConnectionString connString = new ConnectionString(
			"mongodb://localhost:27017/austria"
	);
	public static MongoDatabase mongoDatabase;
	public static UserRepository userRepository;
	public static ClassRepository classRepository;
	public static ChatRoomRepository chatRoomRepository;
	public static ChatPresenceRepository chatPresenceRepository;

	public static final int SOCKET_MAX_REQUESTS_PER_MIN = 50; //or whatever you want it to be
	public static LoadingCache<String, Integer> socketRequestCountsPerIpAddress;

	private static void setupDB() {

		try {
			MongoClientSettings settings = MongoClientSettings.builder()
					.applyConnectionString(connString)
					.retryWrites(true)
					.build();

			MongoClient mongoClient = MongoClients.create(settings);
			mongoDatabase = mongoClient.getDatabase("austria");

			userRepository = new UserRepository();
			classRepository = new ClassRepository();
			chatRoomRepository = new ChatRoomRepository();
			chatPresenceRepository = new ChatPresenceRepository();

			socketRequestCountsPerIpAddress = CacheBuilder.newBuilder().
					expireAfterWrite(1, TimeUnit.MINUTES).build(new CacheLoader<>() {
						public Integer load(String key) {
							return 0;
						}
					});


		} catch (Exception x) {
			x.printStackTrace();
		}
	}

	public static void main(String[] args) {
		setupDB();
		SpringApplication.run(WebsocketDemoApplication.class, args);
	}

}
