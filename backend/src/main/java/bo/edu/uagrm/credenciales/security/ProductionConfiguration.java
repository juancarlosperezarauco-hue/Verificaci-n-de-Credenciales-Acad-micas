package bo.edu.uagrm.credenciales.security;

import java.net.URI;
import java.util.*;
import org.springframework.context.annotation.*;
import org.springframework.core.env.Environment;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;

/** Stop before accepting traffic if production still uses local or missing secrets. */
@Configuration @Profile("production")
public class ProductionConfiguration {
    @Bean static BeanFactoryPostProcessor validateProductionConfiguration(Environment env){
        return beanFactory->{
            Set<String> secrets=new HashSet<>();
            for(String name:List.of("app.security.jwt-secret","app.security.mfa-encryption-key",
                    "app.mail-outbox.encryption-key","app.evidence.encryption-key","app.signing.encryption-key")){
                String value=required(env,name);
                if(value.length()<32||!secrets.add(value)||value.toLowerCase(Locale.ROOT).contains("test-secret"))
                    throw new IllegalStateException("Configura claves privadas independientes: "+name);
            }
            for(String name:List.of("spring.datasource.password","app.storage.secret-key","spring.mail.password")){
                String value=required(env,name);
                if(Set.of("credentials","credentials_local","minioadmin","minioadmin123","test","test-secret").contains(value)||value.length()<12)
                    throw new IllegalStateException("Reemplaza la credencial de desarrollo: "+name);
            }
            for(String name:List.of("app.frontend-url","app.security.allowed-origin","app.storage.endpoint")){
                URI uri=URI.create(required(env,name));
                if(!"https".equals(uri.getScheme())||uri.getHost()==null||Set.of("localhost","127.0.0.1","minio").contains(uri.getHost())||uri.getUserInfo()!=null||uri.getRawQuery()!=null||uri.getRawFragment()!=null
                        || !(uri.getPath().isEmpty() || uri.getPath().equals("/")))
                    throw new IllegalStateException("Configura una URL HTTPS externa: "+name);
            }
            if(!required(env,"app.frontend-url").replaceAll("/+$","").equals(required(env,"app.security.allowed-origin").replaceAll("/+$","")))
                throw new IllegalStateException("El origen permitido debe coincidir con el frontend público.");
            String db=required(env,"spring.datasource.url");
            PostgresTlsPolicy.validate(db);
            for (String key : List.of("sslfactory", "sslhostnameverifier", "sslmode")) {
                if (env.containsProperty("spring.datasource.hikari.data-source-properties." + key))
                    throw new IllegalStateException("Configura TLS de PostgreSQL únicamente en DB_URL.");
            }
            required(env,"spring.mail.host");required(env,"spring.mail.username");required(env,"app.mail-from");
            if(env.getProperty("app.signing.allow-demo",Boolean.class,false))throw new IllegalStateException("Firma de demostración deshabilitada en producción.");
        };
    }
    private static String required(Environment env,String name){
        String value=env.getProperty(name);if(value==null||value.isBlank())throw new IllegalStateException("Falta configuración de producción: "+name);return value;
    }
}
