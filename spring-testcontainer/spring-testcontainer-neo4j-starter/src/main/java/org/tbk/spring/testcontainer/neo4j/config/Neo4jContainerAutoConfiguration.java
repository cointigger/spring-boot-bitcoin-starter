package org.tbk.spring.testcontainer.neo4j.config;

import lombok.extern.slf4j.Slf4j;
import org.neo4j.driver.*;
import org.neo4j.driver.internal.Scheme;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.data.neo4j.Neo4jDataAutoConfiguration;
import org.springframework.boot.autoconfigure.neo4j.ConfigBuilderCustomizer;
import org.springframework.boot.autoconfigure.neo4j.Neo4jAutoConfiguration;
import org.springframework.boot.autoconfigure.neo4j.Neo4jProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.source.InvalidConfigurationPropertyValueException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.tbk.spring.testcontainer.core.MoreTestcontainers;
import org.testcontainers.containers.Neo4jContainer;
import org.testcontainers.utility.DockerImageName;

import java.io.File;
import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

@Slf4j
@Configuration
@EnableConfigurationProperties(Neo4jContainerProperties.class)
@ConditionalOnProperty(value = "org.tbk.spring.testcontainer.neo4j.enabled", havingValue = "true")
@AutoConfigureBefore({
        Neo4jAutoConfiguration.class,
        Neo4jDataAutoConfiguration.class
})
public class Neo4jContainerAutoConfiguration {
    // testcontainers only supports 'neo4j' username atm.
    // throw loud and early if the spring autoconfigure properties mismatch!
    // user must enable testcontainers via `org.tbk.spring.testcontainer.neo4j.enabled: true`
    // so we can allow other usernames in production environments
    private static final String MANDATORY_USERNAME = "neo4j";

    private final Neo4jContainerProperties properties;

    public Neo4jContainerAutoConfiguration(Neo4jContainerProperties properties) {
        this.properties = requireNonNull(properties);
    }

    @Bean(initMethod = "start", destroyMethod = "stop")
    public Neo4jContainer<?> neo4jContainer(Neo4jProperties neo4jProperties) {
        DockerImageName dockerImageName = properties.getDockerImageName();

        String dockerContainerName = String.format("%s-%s", dockerImageName.getUnversionedPart(),
                Integer.toHexString(System.identityHashCode(this)))
                .replace("/", "-");

        return new Neo4jContainer<>(dockerImageName)
                .withCreateContainerCmdModifier(MoreTestcontainers.cmdModifiers().withName(dockerContainerName))
                .withAdminPassword(neo4jProperties.getAuthentication().getPassword());
    }

    @Bean
    public Driver neo4jContainerDriver(Neo4jContainer<?> neo4jContainer,
                                       Neo4jProperties neo4jProperties,
                                       ObjectProvider<ConfigBuilderCustomizer> configBuilderCustomizers) {
        boolean hasCorrectUsername = MANDATORY_USERNAME.equals(neo4jProperties.getAuthentication().getUsername());
        checkArgument(hasCorrectUsername, "Username MUST be 'neo4j' - otherwise testcontainers won't work.");

        AuthToken authToken = AuthTokens.basic(
                neo4jProperties.getAuthentication().getUsername(),
                neo4jProperties.getAuthentication().getPassword(),
                neo4jProperties.getAuthentication().getRealm()
        );

        Config config = mapDriverConfig(neo4jProperties,
                configBuilderCustomizers.orderedStream().collect(Collectors.toList()));
        URI serverUri = URI.create("bolt://localhost:" + neo4jContainer.getMappedPort(7687));

        return GraphDatabase.driver(serverUri, authToken, config);
    }


    Config mapDriverConfig(Neo4jProperties properties, List<ConfigBuilderCustomizer> customizers) {
        Config.ConfigBuilder builder = Config.builder();
        configurePoolSettings(builder, properties.getPool());
        URI uri = properties.getUri();
        String scheme = (uri != null) ? uri.getScheme() : "bolt";
        configureDriverSettings(builder, properties, isSimpleScheme(scheme));
        builder.withLogging(Logging.slf4j());
        customizers.forEach((customizer) -> customizer.customize(builder));
        return builder.build();
    }

    private boolean isSimpleScheme(String scheme) {
        String lowerCaseScheme = scheme.toLowerCase(Locale.ENGLISH);
        try {
            Scheme.validateScheme(lowerCaseScheme);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException(String.format("'%s' is not a supported scheme.", scheme));
        }
        return lowerCaseScheme.equals("bolt") || lowerCaseScheme.equals("neo4j");
    }

    private void configurePoolSettings(Config.ConfigBuilder builder, Neo4jProperties.Pool pool) {
        if (pool.isLogLeakedSessions()) {
            builder.withLeakedSessionsLogging();
        }
        builder.withMaxConnectionPoolSize(pool.getMaxConnectionPoolSize());
        Duration idleTimeBeforeConnectionTest = pool.getIdleTimeBeforeConnectionTest();
        if (idleTimeBeforeConnectionTest != null) {
            builder.withConnectionLivenessCheckTimeout(idleTimeBeforeConnectionTest.toMillis(), TimeUnit.MILLISECONDS);
        }
        builder.withMaxConnectionLifetime(pool.getMaxConnectionLifetime().toMillis(), TimeUnit.MILLISECONDS);
        builder.withConnectionAcquisitionTimeout(pool.getConnectionAcquisitionTimeout().toMillis(),
                TimeUnit.MILLISECONDS);
        if (pool.isMetricsEnabled()) {
            builder.withDriverMetrics();
        } else {
            builder.withoutDriverMetrics();
        }
    }

    private void configureDriverSettings(Config.ConfigBuilder builder, Neo4jProperties properties,
                                         boolean withEncryptionAndTrustSettings) {
        if (withEncryptionAndTrustSettings) {
            applyEncryptionAndTrustSettings(builder, properties.getSecurity());
        }
        builder.withConnectionTimeout(properties.getConnectionTimeout().toMillis(), TimeUnit.MILLISECONDS);
        builder.withMaxTransactionRetryTime(properties.getMaxTransactionRetryTime().toMillis(), TimeUnit.MILLISECONDS);
    }

    private void applyEncryptionAndTrustSettings(Config.ConfigBuilder builder,
                                                 Neo4jProperties.Security securityProperties) {
        if (securityProperties.isEncrypted()) {
            builder.withEncryption();
        } else {
            builder.withoutEncryption();
        }
        builder.withTrustStrategy(mapTrustStrategy(securityProperties));
    }

    private Config.TrustStrategy mapTrustStrategy(Neo4jProperties.Security securityProperties) {
        String propertyName = "spring.neo4j.security.trust-strategy";
        Neo4jProperties.Security.TrustStrategy strategy = securityProperties.getTrustStrategy();
        Config.TrustStrategy trustStrategy = createTrustStrategy(securityProperties, propertyName, strategy);
        if (securityProperties.isHostnameVerificationEnabled()) {
            trustStrategy.withHostnameVerification();
        } else {
            trustStrategy.withoutHostnameVerification();
        }
        return trustStrategy;
    }

    private Config.TrustStrategy createTrustStrategy(Neo4jProperties.Security securityProperties, String propertyName,
                                                     Neo4jProperties.Security.TrustStrategy strategy) {
        switch (strategy) {
            case TRUST_ALL_CERTIFICATES:
                return Config.TrustStrategy.trustAllCertificates();
            case TRUST_SYSTEM_CA_SIGNED_CERTIFICATES:
                return Config.TrustStrategy.trustSystemCertificates();
            case TRUST_CUSTOM_CA_SIGNED_CERTIFICATES:
                File certFile = securityProperties.getCertFile();
                if (certFile == null || !certFile.isFile()) {
                    throw new InvalidConfigurationPropertyValueException(propertyName, strategy.name(),
                            "Configured trust strategy requires a certificate file.");
                }
                return Config.TrustStrategy.trustCustomCertificateSignedBy(certFile);
            default:
                throw new InvalidConfigurationPropertyValueException(propertyName, strategy.name(), "Unknown strategy.");
        }
    }
}
