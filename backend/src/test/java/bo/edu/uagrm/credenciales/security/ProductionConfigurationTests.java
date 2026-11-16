package bo.edu.uagrm.credenciales.security;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
class ProductionConfigurationTests {
    MockEnvironment environment(){
        var env=new MockEnvironment();
        for(String name:java.util.List.of("app.security.jwt-secret","app.security.mfa-encryption-key","app.mail-outbox.encryption-key","app.evidence.encryption-key","app.signing.encryption-key"))
            env.setProperty(name,java.util.UUID.randomUUID()+java.util.UUID.randomUUID().toString());
        for(String name:java.util.List.of("spring.datasource.password","app.storage.secret-key","spring.mail.password"))env.setProperty(name,"private-fixture-credential");
        env.setProperty("app.frontend-url","https://credentials.example.test");
        env.setProperty("app.security.allowed-origin","https://credentials.example.test");
        env.setProperty("app.storage.endpoint","https://storage.example.test");
        env.setProperty("spring.datasource.url","jdbc:postgresql://db.example.test/credentials?sslmode=verify-full");
        env.setProperty("spring.mail.host","smtp.example.test");env.setProperty("spring.mail.username","fixture");env.setProperty("app.mail-from","no-reply@example.test");
        return env;
    }
    @Test void acceptsAnExplicitExternalConfiguration()throws Exception{new ProductionConfiguration().validateProductionConfiguration(environment()).afterPropertiesSet();}
    @Test void rejectsLocalUrlsDuplicateKeysAndDatabaseWithoutCertificateVerification(){
        var env=environment();env.setProperty("app.frontend-url","http://localhost:4300");
        assertThrows(IllegalStateException.class,()->new ProductionConfiguration().validateProductionConfiguration(env).afterPropertiesSet());
        var duplicate=environment();duplicate.setProperty("app.signing.encryption-key",duplicate.getProperty("app.evidence.encryption-key"));
        assertThrows(IllegalStateException.class,()->new ProductionConfiguration().validateProductionConfiguration(duplicate).afterPropertiesSet());
        var db=environment();db.setProperty("spring.datasource.url","jdbc:postgresql://db.example.test/credentials");
        assertThrows(IllegalStateException.class,()->new ProductionConfiguration().validateProductionConfiguration(db).afterPropertiesSet());
    }
    @Test void rejectsDemoSigningAndDefaultPasswords(){
        var env=environment();env.setProperty("app.signing.allow-demo","true");
        assertThrows(IllegalStateException.class,()->new ProductionConfiguration().validateProductionConfiguration(env).afterPropertiesSet());
        var local=environment();local.setProperty("app.storage.secret-key","minioadmin123");
        assertThrows(IllegalStateException.class,()->new ProductionConfiguration().validateProductionConfiguration(local).afterPropertiesSet());
    }
}
