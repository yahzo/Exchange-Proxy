Utilise l'ia pour t'aider à configurer le tout, j'ai mis 1H max pour sortir des données, je me suis arrêté à la visualisation ok ?

# 💱 Proxy Taux de Change - Streaming Data Pipeline

## 📝 Description
Ce projet implémente un pipeline de données en temps réel. Il récupère les taux de change USD via une API externe et les distribue via Kafka à plusieurs consommateurs, avant de les stocker dans Elasticsearch pour visualisation.

## 🏗️ Architecture
- **Producer** : Script Python qui interroge l'API et publie sur le topic Kafka `exchange-rates`.
- **Kafka** : Broker de messages pour le streaming.
- **Consumer** : Script Python qui lit Kafka et indexe les données dans Elasticsearch.
- **ELK Stack** : Elasticsearch pour le stockage et Kibana pour le dashboarding.

## 🚀 Lancement Rapide

1. **Démarrer l'infrastructure :**
_Je l'ai lancé sur un terminal windows à part_

   ```bash
   docker compose up -d

2. **Installer les dépendances :**
_lance le sur ton interface VSCODE_

pip install -r producer/requirements.txt
pip install -r consumer/requirements.txt

3. **Lancer le pipeline :**
_Sur deux terminal vscode différents_

Lancer le producer : python producer/main.py

Lancer le consumer : python consumer/main.py

4. **Visualisation : **

Rendez-vous sur http://localhost:5601 (Kibana).




Utilise l'ia pour t'aider à configurer le tout, j'ai mis 1H max pour sortir des données, je me suis arrêté à la visualisation ok ?

🛠️ Spécifications Techniques & Architecture
1. Stack Infrastructure (Docker)
Kafka (Broker 3.9.0) : Utilise le mode KRaft (sans Zookeeper) pour plus de légèreté.

Elasticsearch (8.12.0) : Moteur de stockage et d'indexation.

Kibana (8.12.0) : Interface de visualisation et de management des données.

2. Flux de données (Pipeline)
Source : API Exchange Rate (appel toutes les 60 secondes).

Transport : Le Producer envoie les JSON sur le topic Kafka nommé exchange-rates.

Persistance : Le Consumer lit le flux en continu et indexe les documents dans Elasticsearch sous l'index forex-data.

Visualisation : Création d'une Data View dans Kibana basée sur le champ @timestamp.

3. Points de vigilance (Fixes appliqués)
Pour éviter les erreurs que nous avons résolues, voici les configurations critiques :

Compatibilité Kafka : Les clients Python utilisent api_version=(3, 9, 0) pour s'aligner sur le broker Docker.

Librairie Elasticsearch : Utilisation stricte de la version 8.12.0 en Python pour correspondre à la version du serveur (évite les erreurs de headers/media-type).

Sérialisation :

Producer : value_serializer (encode en utf-8).

Consumer : value_deserializer (decode json).

4. Ports & Accès
Kafka : localhost:9092

Elasticsearch : localhost:9200

Kibana : localhost:5601

🤝 Guide de Collaboration
Ajout de fonctionnalités : Si tu modifies le format des données envoyées par le Producer, n'oublie pas de mettre à jour le Consumer pour qu'il puisse traiter les nouveaux champs.

Dashboards : Les configurations Kibana sont locales à ton instance Docker. Si tu crées un dashboard génial, on pourra l'exporter en JSON plus tard pour le présenter ensemble.

Logs : En cas de problème, vérifie les logs des containers avec docker compose logs -f.

Pourquoi ajouter ça ?
Gain de temps : S'il a une erreur de version, il saura tout de suite que c'est lié à api_version ou à la version d'Elasticsearch.

Autonomie : Il comprend les noms des index (forex-data) et des topics (exchange-rates), donc il peut faire ses propres tests sans te demander.

Note Finale : Les profs adorent voir que vous maîtrisez les ports, les versions et la logique de sérialisation.