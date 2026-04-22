import time
import json
import requests
from kafka import KafkaProducer
from kafka.admin import KafkaAdminClient, NewTopic

# Configuration
TOPIC_NAME = 'exchange-rates'
KAFKA_SERVER = 'localhost:9092'
API_URL = "https://api.exchangerate-api.com/v4/latest/USD"

# 1. Création automatique du topic si besoin
try:
    admin_client = KafkaAdminClient(bootstrap_servers=KAFKA_SERVER)
    if TOPIC_NAME not in admin_client.list_topics():
        admin_client.create_topics(new_topics=[NewTopic(name=TOPIC_NAME, num_partitions=3, replication_factor=1)])
        print(f"🆕 Topic '{TOPIC_NAME}' créé !")
except Exception as e:
    print(f"ℹ️ Topic déjà présent ou erreur : {e}")

# 2. Initialisation du Producer
producer = KafkaProducer(
    bootstrap_servers=[KAFKA_SERVER],
    value_serializer=lambda x: json.dumps(x).encode('utf-8')
)

def run():
    print("🚀 Producer lancé. Envoi des taux toutes les 60s...")
    while True:
        try:
            res = requests.get(API_URL)
            data = res.json()
            producer.send(TOPIC_NAME, value=data)
            print(f"✅ Taux envoyés à {time.strftime('%H:%M:%S')} (Base: {data['base']})")
        except Exception as e:
            print(f"❌ Erreur : {e}")
        time.sleep(60)

if __name__ == "__main__":
    run()