package bo.edu.uagrm.credenciales.security;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import java.util.concurrent.atomic.AtomicBoolean;
class ProductionConfigurationTests {
    MockEnvironment environment(){
        var env=new MockEnvironment();
        for(String name:java.util.List.of("app.security.jwt-secret","app.security.mfa-encryption-key","app.mail-outbox.encryption-key","app.evidence.encryption-key","app.signing.encryption-key"))
            env.setProperty(name,java.util.UUID.randomUUID()+java.util.UUID.randomUUID().toString());
        for(String name:java.util.List.of("spring.datasource.password","app.storage.secret-key","spring.mail.password"))env.setProperty(name,"private-fixture-credential");
        env.setProperty("app.frontend-url","https://credentials.example.test");
        env.setProperty("app.security.allowed-origin","https://credentials.example.test");
        env.setProperty("app.storage.endpoint","https://storage.example.test");
        env.setProperty("spring.datasource.url","jdbc:postgresql://db.example.test/credentials?sslmode=verify-full&sslrootcert=/run/certs/postgres-root.crt");
        env.setProperty("spring.mail.host","smtp.example.test");env.setProperty("spring.mail.username","fixture");env.setProperty("app.mail-from","no-reply@example.test");
        return env;
    }
    @Test void acceptsAnExplicitExternalConfiguration()throws Exception{ProductionConfiguration.validateProductionConfiguration(environment()).postProcessBeanFactory(new org.springframework.beans.factory.support.DefaultListableBeanFactory());}
    @Test void rejectsLocalUrlsDuplicateKeysAndDatabaseWithoutCertificateVerification(){
        var env=environment();env.setProperty("app.frontend-url","http://localhost:4300");
        assertThrows(IllegalStateException.class,()->ProductionConfiguration.validateProductionConfiguration(env).postProcessBeanFactory(new org.springframework.beans.factory.support.DefaultListableBeanFactory()));
        var duplicate=environment();duplicate.setProperty("app.signing.encryption-key",duplicate.getProperty("app.evidence.encryption-key"));
        assertThrows(IllegalStateException.class,()->ProductionConfiguration.validateProductionConfiguration(duplicate).postProcessBeanFactory(new org.springframework.beans.factory.support.DefaultListableBeanFactory()));
        var db=environment();db.setProperty("spring.datasource.url","jdbc:postgresql://db.example.test/credentials");
        assertThrows(IllegalStateException.class,()->ProductionConfiguration.validateProductionConfiguration(db).postProcessBeanFactory(new org.springframework.beans.factory.support.DefaultListableBeanFactory()));
    }
    @Test void rejectsDemoSigningAndDefaultPasswords(){
        var env=environment();env.setProperty("app.signing.allow-demo","true");
        assertThrows(IllegalStateException.class,()->ProductionConfiguration.validateProductionConfiguration(env).postProcessBeanFactory(new org.springframework.beans.factory.support.DefaultListableBeanFactory()));
        var local=environment();local.setProperty("app.storage.secret-key","minioadmin123");
        assertThrows(IllegalStateException.class,()->ProductionConfiguration.validateProductionConfiguration(local).postProcessBeanFactory(new org.springframework.beans.factory.support.DefaultListableBeanFactory()));
    }
    @ParameterizedTest
    @ValueSource(strings = {
        "jdbc:postgresql://db.example.test/credentials?sslmode=verify-full&sslrootcert=/run/certs/postgres-root.crt&sslmode=disable",
        "jdbc:postgresql://db.example.test/credentials?note=sslmode=verify-full",
        "jdbc:postgresql://db.example.test/credentials?sslmode=verify-full&sslrootcert=/run/certs/postgres-root.crt&sslfactory=org.postgresql.ssl.NonValidatingFactory",
        "jdbc:postgresql://db.example.test/credentials?sslmode=verify-full&sslrootcert=/run/certs/postgres-root.crt&sslhostnameverifier=custom.UnsafeVerifier",
        "jdbc:postgresql://db.example.test/credentials?sslmode=verify-full&sslrootcert=/run/certs/postgres-root.crt&ssl=false",
        "jdbc:postgresql://db.example.test/credentials?sslmode=verify-full&sslrootcert=/another/certificate",
        "jdbc:postgresql://private-user:private-password@db.example.test/credentials?sslmode=verify-full&sslrootcert=/run/certs/postgres-root.crt",
        "jdbc:postgresql://db.example.test/credentials?sslmode=verify-full&sslrootcert=/run/certs/postgres-root.crt#private-fragment",
        "jdbc:h2:mem:test?sslmode=verify-full",
        "jdbc:postgresql://db.example.test/credentials?sslmode=verify-full&sslrootcert=%XX"
    })
    void rejectsAmbiguousOrNonValidatingTlsWithoutDisclosingTheUrl(String url) {
        var env = environment();
        env.setProperty("spring.datasource.url", url);
        var error = assertThrows(IllegalStateException.class,
                () -> ProductionConfiguration.validateProductionConfiguration(env)
                    .postProcessBeanFactory(new org.springframework.beans.factory.support.DefaultListableBeanFactory()));
        assertFalse(error.getMessage().contains("private-password"));
        assertFalse(error.getMessage().contains("db.example.test"));
    }
    @ParameterizedTest
    @ValueSource(strings = {"sslmode", "sslfactory", "sslhostnameverifier"})
    void rejectsTlsOverridesOutsideTheReviewedUrl(String name) {
        var env = environment();
        env.setProperty("spring.datasource.hikari.data-source-properties." + name, "override");
        assertThrows(IllegalStateException.class,
                () -> ProductionConfiguration.validateProductionConfiguration(env)
                    .postProcessBeanFactory(new org.springframework.beans.factory.support.DefaultListableBeanFactory()));
    }
    @Test void validatesBeforeSingletonsCanOpenConnections() {
        var attempted = new AtomicBoolean();
        try (var context = new AnnotationConfigApplicationContext()) {
            var env = environment();
            env.setActiveProfiles("production");
            env.setProperty("spring.datasource.url", "jdbc:postgresql://db.example.test/credentials?sslmode=disable");
            context.setEnvironment(env);
            context.register(ProductionConfiguration.class);
            context.registerBean("connectionProbe", Object.class, () -> {
                attempted.set(true);
                return new Object();
            });
            assertThrows(IllegalStateException.class, context::refresh);
            assertFalse(attempted.get(), "Unsafe configuration must fail before a connection is attempted");
        }
    }
    @Test void acceptsEncodedCertificatePathAndTheValidatingDefaultFactory() throws Exception {
        var env = environment();
        env.setProperty("spring.datasource.url", "jdbc:postgresql://db.example.test/credentials?sslmode=verify-full&sslrootcert=%2Frun%2Fcerts%2Fpostgres-root.crt&sslfactory=org.postgresql.ssl.LibPQFactory");
        ProductionConfiguration.validateProductionConfiguration(env)
            .postProcessBeanFactory(new org.springframework.beans.factory.support.DefaultListableBeanFactory());
    }
}
