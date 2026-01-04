package io.github.anirudhk_tech.janus;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
public class JanusApplication {

	public static void main(String[] args) {
		SpringApplication.run(JanusApplication.class, args);
	}

}
