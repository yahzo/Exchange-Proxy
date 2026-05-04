# Exchange Proxy - Pipeline taux de change

## Important : migration Java

Le projet a ete migre de **Python vers Java**.

Il ne faut donc plus lancer :

```bash
python producer/main.py
python consumer/main.py
pip install -r requirements.txt
```

Le projet se lance maintenant avec **Java 17**, **Maven** et **Docker**.

## Objectif du projet

Exchange Proxy est un pipeline de donnees temps reel :

1. Le **producer Java** appelle une API de taux de change USD.
2. Il envoie les donnees JSON dans Kafka sur le topic `exchange-rates`.
3. Le **consumer Java** lit les messages Kafka.
4. Il indexe les donnees dans Elasticsearch dans l'index `forex-data`.
5. Kibana permet de visualiser les donnees.

## Architecture

```text
API Exchange Rate
        |
        v
Producer Java
        |
        v
Kafka topic: exchange-rates
        |
        v
Consumer Java
        |
        v
Elasticsearch index: forex-data
        |
        v
Kibana
```

## Prerequis

Installer sur la machine :

- Docker Desktop
- Java JDK 17
- Maven

Verifier l'installation :

```powershell
java -version
mvn -version
docker version
```

Si `mvn` n'est pas reconnu, Maven n'est pas installe ou son dossier `bin` n'est pas dans le `Path`.

## Installation

Cloner le projet :

```powershell
git clone https://github.com/yahzo/Exchange-Proxy.git
cd Exchange-Proxy
```

Compiler le projet :

```powershell
mvn clean package
```

Maven telecharge automatiquement les dependances Java declarees dans les fichiers `pom.xml`.

## Lancement

### 1. Demarrer Docker Desktop

Ouvrir Docker Desktop et attendre qu'il soit pret.

### 2. Demarrer Kafka, Elasticsearch et Kibana

Depuis le dossier `Exchange-Proxy` :

```powershell
docker compose up -d
```

Verifier les conteneurs :

```powershell
docker compose ps
```

Les services attendus :

- `kafka-broker` sur `localhost:9092`
- `elasticsearch` sur `http://localhost:9200`
- `kibana` sur `http://localhost:5601`

### 3. Lancer le producer

Dans un premier terminal :

```powershell
mvn -pl producer exec:java
```

Le producer recupere les taux de change toutes les 60 secondes et les envoie dans Kafka.

### 4. Lancer le consumer

Dans un deuxieme terminal :

```powershell
mvn -pl consumer exec:java
```

Le consumer lit Kafka et stocke les donnees dans Elasticsearch.

### 5. Ouvrir Kibana

Dans le navigateur :

```text
http://localhost:5601
```

Creer une Data View avec :

```text
Index pattern: forex-data
Time field: @timestamp
```

## Configuration

Les valeurs par defaut peuvent etre surchargees avec des variables d'environnement.

| Variable | Defaut | Utilisation |
| --- | --- | --- |
| `KAFKA_SERVER` | `localhost:9092` | Adresse Kafka |
| `TOPIC_NAME` | `exchange-rates` | Topic Kafka |
| `API_URL` | `https://api.exchangerate-api.com/v4/latest/USD` | API appelee par le producer |
| `FETCH_INTERVAL_SECONDS` | `60` | Delai entre deux appels API |
| `ES_SERVER` | `http://localhost:9200` | Adresse Elasticsearch |
| `INDEX_NAME` | `forex-data` | Index Elasticsearch |
| `CONSUMER_GROUP_ID` | `exchange-proxy-consumer` | Groupe du consumer Kafka |

Exemple pour tester plus vite :

```powershell
$env:FETCH_INTERVAL_SECONDS="10"
mvn -pl producer exec:java
```

## Commandes utiles

Voir les logs Docker :

```powershell
docker compose logs -f
```

Voir uniquement les logs Kafka :

```powershell
docker compose logs -f kafka
```

Arreter les services :

```powershell
docker compose down
```

Recompiler :

```powershell
mvn clean package
```

## Depannage

### `localhost:9092` ne s'ouvre pas dans le navigateur

C'est normal. Kafka n'est pas une page web et n'utilise pas HTTP.

Tester le port avec :

```powershell
Test-NetConnection localhost -Port 9092
```

La valeur attendue est :

```text
TcpTestSucceeded : True
```

### `Topic exchange-rates not present in metadata after 60000 ms`

Kafka n'est pas lance ou pas encore pret.

Verifier :

```powershell
docker compose ps
```

Puis relancer si besoin :

```powershell
docker compose up -d
```

### `mvn: The term 'mvn' is not recognized`

Maven n'est pas installe ou le terminal n'a pas recharge le `Path`.

Solutions :

1. Fermer puis rouvrir le terminal.
2. Verifier `mvn -version`.
3. Installer Maven si besoin.

### Conflit de nom Docker avec `elasticsearch`, `kibana` ou `kafka-broker`

Il peut rester d'anciens conteneurs arretes avec les memes noms.

Lister les conteneurs :

```powershell
docker ps -a
```

Supprimer uniquement les anciens conteneurs qui bloquent :

```powershell
docker rm elasticsearch kibana kafka-broker
```

Puis relancer :

```powershell
docker compose up -d
```

### Warning SLF4J

Ce message n'est pas bloquant :

```text
SLF4J: Failed to load class "org.slf4j.impl.StaticLoggerBinder"
```

Le producer et le consumer peuvent quand meme fonctionner.

## Structure du projet

```text
Exchange-Proxy/
|-- docker-compose.yml
|-- pom.xml
|-- producer/
|   |-- pom.xml
|   `-- src/main/java/com/yahzo/exchangeproxy/producer/
|       `-- ExchangeRateProducerApp.java
`-- consumer/
    |-- pom.xml
    `-- src/main/java/com/yahzo/exchangeproxy/consumer/
        `-- ExchangeRateConsumerApp.java
```

## Versions principales

- Java 17
- Maven 3.x
- Kafka Docker `apache/kafka:3.9.0`
- Elasticsearch `8.12.0`
- Kibana `8.12.0`
- Kafka client Java `3.9.0`
- Elasticsearch Java client `8.12.0`
