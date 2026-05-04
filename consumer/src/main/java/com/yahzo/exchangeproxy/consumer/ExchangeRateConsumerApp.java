package com.yahzo.exchangeproxy.consumer;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.IndexResponse;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpHost;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.elasticsearch.client.RestClient;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

public class ExchangeRateConsumerApp {
    private static final String DEFAULT_TOPIC_NAME = "exchange-rates";
    private static final String DEFAULT_KAFKA_SERVER = "localhost:9092";
    private static final String DEFAULT_ES_SERVER = "http://localhost:9200";
    private static final String DEFAULT_INDEX_NAME = "forex-data";
    private static final String DEFAULT_GROUP_ID = "exchange-proxy-consumer";

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    public static void main(String[] args) throws IOException {
        String topicName = env("TOPIC_NAME", DEFAULT_TOPIC_NAME);
        String kafkaServer = env("KAFKA_SERVER", DEFAULT_KAFKA_SERVER);
        String elasticsearchServer = env("ES_SERVER", DEFAULT_ES_SERVER);
        String indexName = env("INDEX_NAME", DEFAULT_INDEX_NAME);
        String groupId = env("CONSUMER_GROUP_ID", DEFAULT_GROUP_ID);

        Properties properties = new Properties();
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaServer);
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());

        try (
                KafkaConsumer<String, String> consumer = new KafkaConsumer<>(properties);
                RestClient restClient = RestClient.builder(HttpHost.create(elasticsearchServer)).build();
                ElasticsearchTransport transport = new RestClientTransport(restClient, new JacksonJsonpMapper())
        ) {
            ElasticsearchClient elasticsearchClient = new ElasticsearchClient(transport);

            consumer.subscribe(Collections.singletonList(topicName));
            System.out.printf("En attente de messages sur le topic '%s'.%n", topicName);

            runLoop(consumer, elasticsearchClient, indexName);
        }
    }

    private static void runLoop(
            KafkaConsumer<String, String> consumer,
            ElasticsearchClient elasticsearchClient,
            String indexName
    ) {
        while (true) {
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(1));

            for (ConsumerRecord<String, String> record : records) {
                try {
                    Map<String, Object> document = buildDocument(record.value());
                    IndexResponse response = elasticsearchClient.index(index -> index
                            .index(indexName)
                            .document(document));

                    System.out.printf("Donnee stockee dans Elastic : %s (ID: %s).%n",
                            response.result(),
                            response.id());
                } catch (Exception exception) {
                    System.err.printf("Erreur consumer offset %d : %s%n",
                            record.offset(),
                            exception.getMessage());
                }
            }
        }
    }

    private static Map<String, Object> buildDocument(String json) throws IOException {
        JsonNode data = MAPPER.readTree(json);
        Map<String, Object> document = new LinkedHashMap<>();

        document.put("@timestamp", Instant.now().toString());
        document.put("timestamp", data.path("time_last_updated_utc").asText(""));
        document.put("base", data.path("base").isMissingNode() ? null : data.path("base").asText());
        document.put("rates", jsonNodeToMap(data.path("rates")));

        return document;
    }

    private static Map<String, Object> jsonNodeToMap(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return Collections.emptyMap();
        }

        return MAPPER.convertValue(node, MAP_TYPE);
    }

    private static String env(String name, String defaultValue) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? defaultValue : value;
    }
}
