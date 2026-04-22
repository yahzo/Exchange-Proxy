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


### 4. Envoyer le tout sur GitHub (La partie technique)

1.  Va sur [github.com](https://github.com/) et crée un **nouveau dépôt** (New repository) nommé `Exchange-Proxy`. Ne coche pas "Initialize with README".
2.  Dans ton terminal (à la racine du projet), tape ces commandes une par une :

```powershell
# Initialiser Git
git init

# Ajouter tous les fichiers
git add .

# Créer ton premier message de sauvegarde
git commit -m "Initial commit: Architecture complète fonctionnelle"

# Faire le lien avec ton GitHub (remplace l'URL par la tienne)
git branch -M main
git remote add origin https://github.com/TON_PSEUDO/Exchange-Proxy.git

# Envoyer !
git push -u origin main