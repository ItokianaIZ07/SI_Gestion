# Todo final du projet

**Doneli**:

- création script SQL: Doneli
- création des modèles(entités) pour spring
- configuration spring-securité et rôle (permission pour chaque url-page)
- Configurer la séparation stricte des routes de l'application (`/si/client` pour le public et `/si/app` pour l'application de gestion interne).(ohatra reo lien reo)
- mise en place session ou token(à discuter)
- développement du service pour la transformation des fichiers.pdf en binaire pour la base de donnée
- Création de la logique de déconversion du binaire en blob via javascript(ty no réutilisena ho anze mila hoe mampiasa anle binaire pdf avy any anaty page): associena @url ray ohatra /api/pdf

**Harena**:

`Module Manager`

- Créer la vue du formulaire de manifestation de besoin (champs : département, poste, nombre de personnes, type de besoin, motif détaillé, date souhaitée d'arrivée, lieu de travail, date de la manifestation, expérience requise).
- Implémenter le contrôle de saisie (champs obligatoires `*`) et l'appel API pour l'insertion en base de données.
- Développer la page de vision globale du département pour le manager (effectifs, postes, état des demandes).

`Page du candidat`

- Développer la logique de la transformation de la div preview du cv (le page ataon'ny Brandon iny) en pdf: via javascript comme html2pdf.js à télecharger  

**Itokiana**:

- le resaka notification(atao farany | mety misy manampy)

`Module RH`

- Créer la page liste des demandes
- Implémentation de la logique de validation ou refus d'une demande
- Intégrer la saisie du salaire estimé pour chaque demande spécifique.
- Page pour liste des CV des candidats:
  - Intégrer le composant Vue.js basé sur `iframe` et `URL.createObjectURL(response.data)` avec le type de réponse `blob` pour charger et prévisualiser les CVs stockés en base(utilisation de l'api créer par doneli).

**Brandon**:

`Module RH`

- **Gestion des Entretiens et Contrats**
- Mettre en place le système d'envoi d'e-mails automatisés pour les convocations d'entretiens et la notification des résultats(mila manao recherche kely eto d sady mety hamorona compte gmail hanaovana test ihany koa).
- Gérer la transition post-recrutement vers la création du contrat et de l'employé dans la base (`employe`, `contrat`).
- Rehefa ny validé ny contrat ny roa tonta vo miditra eo.

**Fenosoa**:

`Module Finance`

- Développer l'interface de définition de la grille salariale par poste (`grille_salaire`).
- Créer la vue de liste des demandes avec le montant défini par les RH, incluant les boutons "Valider" ou "Refuser" basés sur l'analyse budgétaire.
- Implémenter le calcul automatique du budget annuel de l'entreprise (basé sur le salaire des employés de la table `contrat`/`employe` et des dépenses internes de la table `depense_departement`).
- Gérer l'état dynamique (`enable`/`disable`) des boutons de validation selon le workflow séquentiel et les codes de statut attribués.(eto @ty ny table ef misy resaka statut zay ny demande d ref mi-approuvé ny RH zay vo enable ny bouton any @ direction sy finance)

## NB

- Afaka manisy touche perso fa ezahina mba tsy hivoahana @le objectif le izy
- Verifiena tsara ny table ao @ [init_tables.sql](../backend/database/tables/init_tables.sql) rehefa misy zavatra atao.
- ``Fonctionnalité = branche ray``, tsy manao push direct dev
- manomana jeu de donnée ny tsirairay @le fonctionnalité ataony
- ref misy zavatra kitihana anaty base de donnée dia mamorona fichier vaovao.sql en respectant le nommage suivant: **prenom_date_numero.sql**, de ao anatin'ilay fichier asiana commentaire ny antony namoronana anazy.
- d ze classe créena koa asiana commentaire kely ny prénom anle olona namorona anazy @zay mora ny mitady ny olona hanontaniana ref misy zavatra
- misy table ao hahafahana mamantatra ny état anle demande hoe aiza ho aiza(refus, en attente, etc...)
