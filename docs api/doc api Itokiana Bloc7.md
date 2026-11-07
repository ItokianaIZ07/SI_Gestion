# Documentation API — Module Congés

**Base URL :** `http://localhost:8080/api/conge`

**Format des échanges :** JSON (`application/json`)

---

## Acquisition mensuelle automatique

Le backend appelle la procédure PostgreSQL
`sp_acquerir_conges_mensuels(date)` tous les premiers du mois à minuit, heure
de Madagascar (`Indian/Antananarivo`), en lui passant le premier jour du mois.
La procédure ajoute les mouvements
`ACQUISITION` dans `mouvement_conge` pour les employés au statut `ACTIF` embauchés
au plus tard le premier jour du mois.

Pour chaque type de congé actif, seule la règle active la plus récente et
applicable au premier du mois est retenue. L'acquisition n'a lieu que si
`acquisition_autorisee` est vrai et `jours_acquis_mois` est supérieur à zéro.
Le plafond `plafond_jours`, lorsqu'il existe, est respecté en calculant le
solde à partir des mouvements déjà enregistrés. La période d'acquisition est
stockée au premier du mois dans `periode_debut`; un index unique empêche une
double acquisition pour un même employé, un type et une période.

La migration
`backend/database/tables/Suite Module Rh/Itokiana_07-11-2026-Bloc7_acquisition_conge_mensuelle_06.sql`
doit être appliquée à la base existante avant activation de la tâche planifiée.
Elle met à jour la procédure et garantit l'index d'idempotence. Un lancement
manuel est possible avec :

```sql
CALL sp_acquerir_conges_mensuels(CURRENT_DATE);
```

---

## 1. Récupérer tous les soldes de congés

**Endpoint :** `GET /api/conge/soldes`

**Description :** Récupère les soldes de congés de tous les employés, répartis par type de congé.

**Paramètres :** Aucun.

**Réponse — `200 OK`**

```json
[
  {
    "idEmploye": 1,
    "nomEmploye": "Rakoto",
    "prenomEmploye": "Jean",
    "matriculeEmploye": "EMP001",
    "emailEmploye": "jean@example.com",
    "typeConge": "Congé annuel",
    "solde": 15.0
  },
  {
    "idEmploye": 1,
    "nomEmploye": "Rakoto",
    "prenomEmploye": "Jean",
    "matriculeEmploye": "EMP001",
    "emailEmploye": "jean@example.com",
    "typeConge": "Congé maladie",
    "solde": 5.0
  }
]
```

---

## 2. Récupérer toutes les demandes de congés

**Endpoint :** `GET /api/conge/demandes`

**Description :** Récupère la liste de toutes les demandes de congés enregistrées.

**Paramètres :** Aucun.

**Réponse — `200 OK`**

```json
[
  {
    "idDemande": 1,
    "idEmploye": 1,
    "nomEmploye": "Rakoto",
    "prenomEmploye": "Jean",
    "matriculeEmploye": "EMP001",
    "emailEmploye": "jean@example.com",
    "dateDebutConge": "2026-11-10",
    "dateFinConge": "2026-11-14",
    "typeConge": "Congé annuel",
    "nombreJourConge": 5.0,
    "motif": "Repos annuel"
  }
]
```

---

## 3. Récupérer les demandes d'un employé

**Endpoint :** `GET /api/conge/demandes/{id}`

**Description :** Récupère toutes les demandes de congés d'un employé précis.

**Paramètre de chemin :**

| Paramètre | Type | Description |
|---|---|---|
| `id` | Integer | Identifiant de l'employé |

**Exemple :**

```http
GET /api/conge/demandes/1
```

**Réponse — `200 OK`**

```json
[
  {
    "idDemande": 1,
    "idEmploye": 1,
    "nomEmploye": "Rakoto",
    "prenomEmploye": "Jean",
    "matriculeEmploye": "EMP001",
    "emailEmploye": "jean@example.com",
    "dateDebutConge": "2026-11-10",
    "dateFinConge": "2026-11-14",
    "typeConge": "Congé annuel",
    "nombreJourConge": 5.0,
    "motif": "Repos annuel"
  }
]
```

---

## 4. Enregistrer une demande de congé

**Endpoint :** `POST /api/conge/demandes`

**Description :** Enregistre une nouvelle demande de congé pour un employé.

**Header :**

```http
Content-Type: application/json
Accept: application/json
```

**Corps de la requête (Request Body) :**

| Champ | Type | Obligatoire | Description |
|---|---|---|---|
| `idEmploye` | Integer | Oui | Identifiant de l'employé |
| `idTypeConge` | Integer | Oui | Identifiant du type de congé |
| `dateDebut` | LocalDate | Oui | Date de début du congé |
| `dateFin` | LocalDate | Oui | Date de fin du congé |
| `nombreJours` | Number | Oui | Nombre de jours demandés |
| `motif` | String | Non | Motif de la demande |

**Exemple de requête :**

```json
{
  "idEmploye": 1,
  "idTypeConge": 1,
  "dateDebut": "2026-11-10",
  "dateFin": "2026-11-14",
  "nombreJours": 5,
  "motif": "Repos annuel"
}
```

**Réponse — `200 OK`**

```json
{
  "idDemande": 6,
  "idEmploye": 1,
  "nomEmploye": "Rakoto",
  "prenomEmploye": "Jean",
  "matriculeEmploye": "EMP001",
  "emailEmploye": "jean@example.com",
  "dateDebutConge": "2026-11-10",
  "dateFinConge": "2026-11-14",
  "typeConge": "Congé annuel",
  "nombreJourConge": 5.0,
  "motif": "Repos annuel"
}
```

### Validations effectuées

Lors de l'enregistrement, le backend vérifie notamment :

- L'existence de l'employé.
- L'existence du type de congé.
- La validité des dates (date de fin supérieure ou égale à la date de début).
- La positivité du nombre de jours.
- La disponibilité du solde de congés.

La demande créée est initialement placée dans l'état `EN_ATTENTE_MANAGER`, avec deux étapes de validation initialisées : Manager (ordre 1) et RH (ordre 2).

---

## 5. Récapitulatif des endpoints

| Méthode | Endpoint | Fonction |
|---|---|---|
| GET | `/api/conge/soldes` | Récupérer tous les soldes |
| GET | `/api/conge/demandes` | Récupérer toutes les demandes |
| GET | `/api/conge/demandes/{id}` | Récupérer les demandes d'un employé |
| POST | `/api/conge/demandes` | Créer une demande |
| GET | `/api/conge/demandes/en-attente` | Lister les demandes à valider selon le rôle connecté |
| POST | `/api/conge/demandes/{id}/validation` | Accepter ou refuser une demande à l'étape courante |
| GET | `/api/conge/demandes/{id}/validations` | Consulter l'historique des décisions |
| POST | `/api/conge/demandes/{id}/annulation` | Annuler une demande validée et journaliser l'annulation |

---

## 6. Formats des données

| Type | Format JSON | Exemple |
|---|---|---|
| Integer | Nombre entier | `1` |
| String | Chaîne de caractères | `"Congé annuel"` |
| LocalDate | `YYYY-MM-DD` | `"2026-11-10"` |
| double | Nombre décimal | `5.0` |
| List | Tableau JSON | `[{...}, {...}]` |

---

## 7. Connexion d'un employé (sans mot de passe)

**Endpoint :** `POST /api/auth/employe/login`

Le compte est identifié par la combinaison du matricule, de l'email et du
numéro CIN. La connexion est acceptée seulement si ces trois valeurs
correspondent au même employé actif, associé à un utilisateur actif portant le
rôle `EMPLOYE`. La connexion crée une session HTTP, comme le login existant.

**Corps de la requête :**

```json
{
  "email": "jean@example.com",
  "matricule": "EMP001",
  "numeroCin": "101010101010"
}
```

**Réponse — `200 OK`**

```json
{
  "success": true,
  "message": "Authentification réussie",
  "role": "EMPLOYE",
  "employe": {
    "id": 1,
    "matricule": "EMP001",
    "nom": "Rakoto",
    "prenom": "Jean",
    "email": "jean@example.com"
  }
}
```

La requête est rejetée avec `400 Bad Request` si un champ est absent ou mal
formé, et avec `401 Unauthorized` si les informations ne correspondent pas, si
l'employé ou l'utilisateur est inactif, si le compte utilisateur n'est pas lié,
si l'email du compte utilisateur ne correspond pas à celui de l'employé, ou s'il
ne possède pas le rôle `EMPLOYE`.

**Récupérer l'employé de la session :** `GET /api/auth/employe/me`

Cette route renvoie le même format de réponse que la connexion et nécessite la
session créée par celle-ci. Le script ajoutant le rôle se trouve dans
`backend/database/tables/Suite Module Rh/Itokiana_06-11-2026-Bloc7_role_employe_01.sql`.

> Le matricule, l'email et le numéro CIN sont des données d'identification, pas
> des secrets. Ce mode sans mot de passe doit être considéré comme provisoire et
> ne fournit pas à lui seul une authentification forte.

---

## 8. Accepter ou refuser une demande de congé

Le circuit suit les étapes définies par `validation_conge` et les vues
`v_conge_demandes_en_attente` et `v_conge_validations` :

1. Le **MANAGER du département de l'employé** traite les demandes en
   `EN_ATTENTE_MANAGER`.
2. Si le manager accepte, le statut passe à `EN_ATTENTE_RH`.
3. Le **RH** traite ensuite ces demandes. Une acceptation RH fait passer le
   statut à `VALIDEE`; un refus à l'une ou l'autre étape le fait passer à
   `REFUSEE`.

### Lister les demandes à traiter

**Endpoint :** `GET /api/conge/demandes/en-attente`

Le manager ne reçoit que les demandes de son département en attente de son
étape. Le RH reçoit les demandes en attente de l'étape RH. Cette route exige
une session connectée avec le rôle `MANAGER` ou `RH`.

### Enregistrer une décision

**Endpoint :** `POST /api/conge/demandes/{id}/validation`

**Corps de la requête :**

```json
{
  "decision": "ACCEPTE",
  "commentaire": "D'accord pour cette période."
}
```

`decision` accepte uniquement `ACCEPTE` ou `REFUSE`; le commentaire est
facultatif (2 000 caractères maximum). La requête nécessite une session avec le
rôle correspondant à l'étape et un jeton CSRF valide.
Le jeton peut être obtenu avec `GET /api/auth/csrf`, puis renvoyé dans l'en-tête
`X-XSRF-TOKEN`.

**Réponse — `200 OK`**

```json
{
  "idDemande": 1,
  "etapeValidee": "MANAGER",
  "decision": "ACCEPTE",
  "statutGlobal": "EN_ATTENTE_RH",
  "commentaire": "D'accord pour cette période.",
  "dateDecision": "2026-11-06T10:15:00"
}
```

Chaque réponse est enregistrée avec l'utilisateur, l'étape, le commentaire et
la date dans `validation_conge`. Après l'acceptation finale du RH, les congés
dont la règle active prévoit la déduction génèrent un mouvement
`CONSOMMATION` négatif dans `mouvement_conge`. Le solde est revérifié à cette
étape afin d'éviter de valider une demande devenue insuffisamment provisionnée.
Une seule consommation est autorisée par demande. La contrainte est créée par
`backend/database/tables/Suite Module Rh/Itokiana_06-11-2026-Bloc7_mouvement_conge_unique_consommation_01.sql`
après la création de `mouvement_conge`; ce script s'arrête explicitement si des
consommations en double existent déjà. Les vues
`v_conge_solde_employe` et `v_conge_historique_mouvements` reflètent ensuite le
mouvement depuis la table, sans actualisation manuelle.

### Consulter les décisions prises

**Endpoint :** `GET /api/conge/demandes/{id}/validations`

Renvoie l'historique des étapes et des décisions depuis la vue
`v_conge_validations`. Le RH peut consulter tout l'historique; un manager est
limité aux demandes des employés de son département.

### Validations et erreurs

- `400 Bad Request` : décision absente/invalide ou commentaire trop long.
- `401 Unauthorized` : aucune session valide.
- `403 Forbidden` : rôle inadapté à l'étape ou manager d'un autre département.
- `404 Not Found` : demande inexistante.
- `409 Conflict` : étape déjà terminée, règle de congé manquante ou solde
  insuffisant à l'approbation finale.
- Une demande refusée ou définitivement validée ne peut plus être décidée.

---

## 9. Annuler une demande de congé validée

**Endpoint :** `POST /api/conge/demandes/{id}/annulation`

Le salarié concerné ou un utilisateur `RH` actif peut annuler une demande au
statut `VALIDEE`. La requête requiert une session et le jeton CSRF obtenu avec
`GET /api/auth/csrf` (en-tête `X-XSRF-TOKEN`).

**Corps de la requête :**

```json
{
  "motif": "Retour anticipé du congé."
}
```

Le motif est obligatoire et limité à 2 000 caractères. La réponse contient le
statut et le nombre de jours restitués :

```json
{
  "idDemande": 1,
  "statutGlobal": "ANNULEE",
  "joursRestitues": 3.0,
  "dateAnnulation": "2026-11-06T11:30:00",
  "motif": "Retour anticipé du congé."
}
```

L'annulation est transactionnelle et verrouille la demande. Elle ne modifie ni
les validations du manager/RH, ni le mouvement initial `CONSOMMATION`. La
demande passe à `ANNULEE`. Lorsqu'une règle applicable déduit le solde :

- avant le début, tous les jours du mouvement de consommation sont restitués;
- après la fin, aucun jour n'est restitué;
- pendant la période, seuls les jours non encore consommés sont restitués, par
  l'ajout d'un mouvement positif `RESTITUTION`.

Le calcul des jours écoulés pendant une période en cours utilise la fonction
SQL existante `fn_calculer_jours_conge` (jours du lundi au vendredi hors dates
de `jour_ferie`). Le projet ne relie pas encore les pointages/présences aux
demandes de congé; il s'agit donc d'une estimation en jours ouvrés, et non
d'une mesure de présence réelle. Si la règle ne déduit pas le solde, ou si
aucune consommation initiale n'existe, aucun mouvement de restitution n'est
créé.

Aucune table n'est ajoutée : chaque annulation crée aussi un mouvement
`ANNULATION` dans `mouvement_conge`, avec `nombre_jours = 0`, le motif, l'auteur,
la demande et la date générée par la base. La vue
`v_conge_historique_mouvements` l'affiche, tandis que `v_conge_solde_employe`
ne modifie pas le solde puisque la ligne d'annulation vaut zéro. Si des jours
doivent être rendus, une deuxième ligne `RESTITUTION` positive est ajoutée. Le
mouvement initial `CONSOMMATION` n'est jamais modifié ni supprimé.

Le script
`backend/database/tables/Suite Module Rh/Itokiana_06-11-2026-Bloc7_restitution_conge_unique_04.sql`
ajoute uniquement un index partiel unique sur la table existante `mouvement_conge`
afin d'empêcher plusieurs restitutions pour une demande. Il détecte les doublons
existants et interrompt la migration s'il en trouve.
La migration
`backend/database/tables/Suite Module Rh/Itokiana_06-11-2026-Bloc7_annulation_mouvement_05.sql`
autorise le type `ANNULATION` à zéro jour et garantit une seule trace
d'annulation par demande. Exécuter cette migration avant de déployer le backend
mis à jour.

### Erreurs principales

- `400 Bad Request` : motif absent ou trop long.
- `401 Unauthorized` : session absente.
- `403 Forbidden` : l'utilisateur n'est ni RH ni le salarié concerné.
- `404 Not Found` : demande inexistante.
- `409 Conflict` : statut différent de `VALIDEE`, règle manquante, mouvement de
  consommation invalide ou restitution déjà enregistrée.
