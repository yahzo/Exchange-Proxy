package com.yahzo.exchangeproxy.producer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class ExchangeRateProducerApp {
    private static final String DEFAULT_TOPIC_NAME = "exchange-rates";
    private static final String DEFAULT_KAFKA_SERVER = "localhost:9092";
    private static final String DEFAULT_API_URL = "https://api.exchangerate-api.com/v4/latest/USD";
    private static final int DEFAULT_INTERVAL_SECONDS = 60;

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    public static void main(String[] args) {
        String topicName = env("TOPIC_NAME", DEFAULT_TOPIC_NAME);
        String kafkaServer = env("KAFKA_SERVER", DEFAULT_KAFKA_SERVER);
        String apiUrl = env("API_URL", DEFAULT_API_URL);
        int intervalSeconds = envInt("FETCH_INTERVAL_SECONDS", DEFAULT_INTERVAL_SECONDS);

        createTopicIfNeeded(kafkaServer, topicName);

        Properties properties = new Properties();
        properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaServer);
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        properties.put(ProducerConfig.ACKS_CONFIG, "all");

        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build();

        System.out.printf("Producer lance. Envoi des taux toutes les %ds vers le topic '%s'.%n",
                intervalSeconds,
                topicName);

        try (KafkaProducer<String, String> producer = new KafkaProducer<>(properties)) {
            runLoop(producer, httpClient, topicName, apiUrl, intervalSeconds);
        }
    }

    private static void runLoop(
            KafkaProducer<String, String> producer,
            HttpClient httpClient,
            String topicName,
            String apiUrl,
            int intervalSeconds
    ) {
        while (true) {
            try {
                String payload = fetchExchangeRates(httpClient, apiUrl);
                JsonNode data = MAPPER.readTree(payload);
                String base = data.path("base").asText("USD");

                ProducerRecord<String, String> record = new ProducerRecord<>(topicName, base, payload);
                producer.send(record).get(10, TimeUnit.SECONDS);

                System.out.printf("Taux envoyes a %s (Base: %s).%n",
                        LocalTime.now().format(TIME_FORMATTER),
                        base);
            } catch (Exception exception) {
                System.err.printf("Erreur producer : %s%n", exception.getMessage());
            }

            sleep(intervalSeconds);
        }
    }

    private static void createTopicIfNeeded(String kafkaServer, String topicName) {
        Properties properties = new Properties();
        properties.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaServer);

        try (AdminClient adminClient = AdminClient.create(properties)) {
            Set<String> topicNames = adminClient.listTopics().names().get(10, TimeUnit.SECONDS);

            if (!topicNames.contains(topicName)) {
                NewTopic topic = new NewTopic(topicName, 3, (short) 1);
                adminClient.createTopics(List.of(topic)).all().get(10, TimeUnit.SECONDS);
                System.out.printf("Topic '%s' cree.%n", topicName);
            }
        } catch (ExecutionException exception) {
            System.out.printf("Topic deja present ou creation impossible : %s%n",
                    rootMessage(exception));
        } catch (Exception exception) {
            System.out.printf("Verification du topic impossible : %s%n", exception.getMessage());
        }
    }

    private static String fetchExchangeRates(HttpClient httpClient, String apiUrl)
            throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(apiUrl))
                .timeout(Duration.ofSeconds(20))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("API HTTP " + response.statusCode());
        }

        return response.body();
    }

    private static void sleep(int intervalSeconds) {
        try {
            Thread.sleep(Duration.ofSeconds(intervalSeconds).toMillis());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Producer interrompu", exception);
        }
    }

    private static String env(String name, String defaultValue) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private static int envInt(String name, int defaultValue) {
        String value = System.getenv(name);

        if (value == null || value.isBlank()) {
            return defaultValue;
        }

        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            return defaultValue;
        }
    }

    private static String rootMessage(Throwable throwable) {
        Throwable cursor = throwable;

        while (cursor.getCause() != null) {
            cursor = cursor.getCause();
        }

        return cursor.getMessage();
    }
}
