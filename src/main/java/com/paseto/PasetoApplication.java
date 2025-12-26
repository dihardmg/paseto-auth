package com.paseto;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@SpringBootApplication
public class PasetoApplication extends SpringBootServletInitializer {

	@Override
	protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
		return application.sources(PasetoApplication.class);
	}

	public static void main(String[] args) {
		SpringApplication application = new SpringApplication(PasetoApplication.class);

		// Enable Virtual Threads (Java 25)
		System.setProperty("jdk.virtualThreadParallelism", "true");

		application.run(args);
	}

}
