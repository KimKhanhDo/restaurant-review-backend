package com.dev.restaurant_backend;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Starts a throwaway Elasticsearch container for tests.
 * The version matches docker-compose.yaml so tests run against the same engine as local development.
 * Security is turned off, like in docker-compose.yaml, so the app can connect over plain HTTP.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    private static final DockerImageName ELASTICSEARCH_IMAGE =
            DockerImageName.parse("docker.elastic.co/elasticsearch/elasticsearch:9.4.8");

    @Bean
    @ServiceConnection
    ElasticsearchContainer elasticsearchContainer() {
        return new ElasticsearchContainer(ELASTICSEARCH_IMAGE)
                .withEnv("xpack.security.enabled", "false")
                .withEnv("ES_JAVA_OPTS", "-Xms512m -Xmx512m");
    }
}
