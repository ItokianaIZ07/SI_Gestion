# Documentation API — Module Congés

**Base URL :** `http://localhost:8080/api/conge`

**Format des échanges :** JSON (`application/json`)

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

---

## 6. Formats des données

| Type | Format JSON | Exemple |
|---|---|---|
| Integer | Nombre entier | `1` |
| String | Chaîne de caractères | `"Congé annuel"` |
| LocalDate | `YYYY-MM-DD` | `"2026-11-10"` |
| double | Nombre décimal | `5.0` |
| List | Tableau JSON | `[{...}, {...}]` |
