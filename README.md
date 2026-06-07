# RecruitAI - Plateforme Intelligente de Recrutement Basée sur l'IA 🚀

Bienvenue sur **RecruitAI**, une plateforme web complète de recrutement développée dans le cadre d'un projet académique (JEE/Angular/Python). Cette application permet d'automatiser et de faciliter la présélection des candidats grâce à l'Intelligence Artificielle (Google Gemini).

## 🌟 Fonctionnalités Principales

- **Gestion des Rôles** : Interface dédiée pour les **Candidats** et les **Recruteurs**.
- **Recruteurs** : Publication d'offres d'emploi, consultation des candidatures, téléchargement des CV (PDF), et gestion du statut des candidatures (Accepter/Refuser).
- **Candidats** : Création de profil, téléversement de CV, consultation des offres, et suivi des candidatures.
- **Intelligence Artificielle (IA)** :
  - Analyse approfondie des CV pour extraire les points forts et proposer des recommandations d'amélioration générales.
  - Calcul automatique d'un **Score de Compatibilité (Match Score)** entre les compétences d'un candidat et les prérequis d'une offre d'emploi.

---

## 🛠️ Stack Technique

### 1. Backend (Java Enterprise Edition)
- **Frameworks** : Jakarta EE 10, JAX-RS (REST API), JPA / Hibernate, CDI.
- **Serveur** : Payara Micro.
- **Base de données** : H2 Database (En mémoire pour le développement actuel - prêt à être migré vers PostgreSQL).
- **Authentification** : JSON Web Tokens (JWT).

### 2. Microservice IA (Python)
- **Framework** : FastAPI, Uvicorn.
- **IA / NLP** : `google-genai` (Google Gemini 2.5 Flash API) pour le matching et l'analyse.
- **Traitement PDF** : `pdfplumber` pour l'extraction de texte.

### 3. Frontend (Angular)
- **Framework** : Angular 17/18 (Standalone Components).
- **Style** : Vanilla CSS moderne (Glassmorphism, animations fluides).

---

## 🚀 Guide d'Installation et de Lancement (Pour l'équipe de dev)

Pour faire tourner le projet complet en local, il est nécessaire de démarrer **les 3 services** en parallèle.

### Étape 1 : Le Microservice IA (Python)
1. Ouvrez un terminal dans le dossier `python-ai-service`.
2. Créez un environnement virtuel et activez-le :
   ```bash
   python3 -m venv venv
   source venv/bin/activate  # Sur Windows: venv\Scripts\activate
   ```
3. Installez les dépendances :
   ```bash
   pip install fastapi uvicorn pdfplumber google-genai
   ```
4. **⚠️ TRÈS IMPORTANT** : Définissez votre clé API Google Gemini.
   ```bash
   export GEMINI_API_KEY="votre_cle_api_ici"
   ```
5. Lancez le serveur :
   ```bash
   python3 main.py
   ```
   *(Le service IA tournera sur `http://localhost:8000`)*

### Étape 2 : Le Backend JEE (Java)
1. Ouvrez un nouveau terminal dans le dossier `CV_project`.
2. Compilez et démarrez le serveur Payara Micro avec Maven :
   ```bash
   mvn clean package payara-micro:start
   ```
   *(Le serveur Java tournera sur `http://localhost:8080/CV_project-1.0-SNAPSHOT`)*

### Étape 3 : Le Frontend (Angular)
1. Ouvrez un troisième terminal dans le dossier `frontend`.
2. Installez les dépendances Node.js :
   ```bash
   npm install
   ```
3. Démarrez le serveur de développement Angular :
   ```bash
   npm start
   ```
   *(L'interface utilisateur sera accessible sur `http://localhost:4200`)*

---

## ⚙️ Migration PostgreSQL & Résolution CORS (Branche `aymane`)

La migration de la base de données de **H2** (en mémoire) vers **PostgreSQL** (persistant) ainsi que la résolution des problèmes CORS ont été réalisées avec succès.

### 1. Structure de la Base de Données (PostgreSQL)
- **Nom de la base** : `recruit_ai`
- **Schéma appliqué** : [schema_postgres.sql](file:///c:/Users/pc/Desktop/projet_jee/schema_postgres.sql)
- **Ajustements de type** : Les colonnes `role` (table `app_users`) et `status` (table `applications`) ont été configurées en `VARCHAR(50)` standard au lieu de types ENUM personnalisés pour assurer une compatibilité native et transparente avec JPA sans erreurs de conversion de type.

### 2. Configuration Backend (Java EE / Jakarta EE)
- **Source de données** : Définie par programmation via l'annotation `@DataSourceDefinition` dans [JAXRSConfiguration.java](file:///c:/Users/pc/Desktop/projet_jee/CV_project/src/main/java/org/example/config/JAXRSConfiguration.java) pointant vers la base locale avec l'utilisateur `postgres`.
- **Unité de persistance** : Configuration dans [persistence.xml](file:///c:/Users/pc/Desktop/projet_jee/CV_project/src/main/resources/META-INF/persistence.xml) pour utiliser EclipseLink `PostgreSQLPlatform`.
- **Mappage Objet-Relationnel (ORM)** :
  - Ajout des annotations `@Column(name = "...")` sur les champs en camelCase (`firstName`, `lastName`, `companyName`, `fileName`, etc.) pour correspondre précisément aux colonnes en snake_case de PostgreSQL (`first_name`, `last_name`, `company_name`, `file_name`).
  - Changement de la stratégie de génération des clés primaires de `GenerationType.AUTO` à `GenerationType.IDENTITY` pour supporter l'auto-incrémentation native des colonnes de type `BIGSERIAL`.
- **Dépendances** : Retrait complet de la dépendance H2 dans [pom.xml](file:///c:/Users/pc/Desktop/projet_jee/CV_project/pom.xml) et ajout du pilote JDBC PostgreSQL (`org.postgresql:postgresql`).

### 3. Résolution du CORS (Sécurité)
- Le fichier [CorsFilter.java](file:///c:/Users/pc/Desktop/projet_jee/CV_project/src/main/java/org/example/security/CorsFilter.java) a été restructuré en tant que filtre `@PreMatching` qui intercepte les requêtes `OPTIONS` de preflight et répond dynamiquement à l'origine du client (ex: `http://localhost:4200`) afin d'autoriser l'envoi de cookies et d'en-têtes de sécurité (`Access-Control-Allow-Credentials: true`).

