package bo.edu.uagrm.credenciales;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(exclude = org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration.class)
public class CredentialApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(CredentialApiApplication.class, args);
	}

}
