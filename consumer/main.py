import json
from kafka import KafkaConsumer
from datetime import datetime
from elasticsearch import Elasticsearch

# Configuration
TOPIC_NAME = 'exchange-rates'
KAFKA_SERVER = 'localhost:9092'
ES_SERVER = 'http://localhost:9200'
INDEX_NAME = 'forex-data'

# 1. Connexion à Elasticsearch
es = Elasticsearch(ES_SERVER)

# 2. Connexion au Consumer Kafka
consumer = KafkaConsumer(
    TOPIC_NAME,
    bootstrap_servers=[KAFKA_SERVER],
    auto_offset_reset='earliest',
    value_deserializer=lambda x: json.loads(x.decode('utf-8')),
    api_version=(3, 9, 0)
)

def run():
    print(f"👂 En attente de messages sur le topic '{TOPIC_NAME}'...")
    for message in consumer:
        data = message.value
        
        # On ajoute un timestamp actuel si l'API n'en donne pas de précis
        # Cela aidera Kibana pour le graphique temporel
        doc = {
            # "timestamp": data.get("time_last_updated_utc", ""),
            "@timestamp": datetime.utcnow().isoformat(),
            "base": data.get("base"),
            "rates": data.get("rates")
        }
        
        # 3. Indexation dans Elasticsearch
        res = es.index(index=INDEX_NAME, document=doc)
        print(f"💾 Donnée stockée dans Elastic : {res['result']} (ID: {res['_id']})")

if __name__ == "__main__":
    run()