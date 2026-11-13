package bo.edu.uagrm.credenciales.identity;

import java.util.concurrent.ThreadPoolExecutor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableAsync
public class StudentMailConfiguration {
    @Bean("studentMailExecutor")
    ThreadPoolTaskExecutor studentMailExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(2);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("student-mail-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.DiscardPolicy());
        return executor;
    }
}
