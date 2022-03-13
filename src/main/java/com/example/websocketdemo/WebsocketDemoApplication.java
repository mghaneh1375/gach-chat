package com.example.websocketdemo;

import com.example.websocketdemo.db.ChatPresenceRepository;
import com.example.websocketdemo.db.ChatRoomRepository;
import com.example.websocketdemo.db.ClassRepository;
import com.example.websocketdemo.db.UserRepository;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

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

		} catch (Exception x) {
			x.printStackTrace();
		}
	}

	public static void main(String[] args) {
		setupDB();
		SpringApplication.run(WebsocketDemoApplication.class, args);
	}

}
