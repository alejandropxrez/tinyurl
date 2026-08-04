package distributed.tinyurl.urlservice;

import org.springframework.boot.SpringApplication;

public class TestUrlServiceApplication {

	public static void main(String[] args) {
		SpringApplication.from(UrlServiceApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
