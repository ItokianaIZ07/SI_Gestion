# Structure des tables — Système RH

## 1. Gestion des utilisateurs et des rôles

`role`

- `id`
- `libelle`

`utilisateur`

- `id`
- `id_role` (FK → role.id)
- `nom`
- `prenom`
- `email` (unique)
- `mdp` (hash du mot de passe)
- `actif`
- `date_creation`

## 2. Organisation de l'entreprise

`departement`

- `id`
- `nom`
- `effectif_max`
- `id_utilisateur_manager` (FK → utilisateur.id)

L'effectif actuel sera calculé à partir des employés actifs, plutôt que stocké manuellement.

`poste`

- `id`
- `nom`
- `id_departement` (FK → departement.id)
- `id_grille_salaire` (FK → grille_salaire.id)

`grille_salaire`

- `id`
- `valeur_min`
- `valeur_max`
- `ìd_poste`
- `date_debut_validite`
- `date_fin_validite` (facultatif)

## 3. Gestion des besoins de recrutement

`type_besoin`

- `id`
- `libelle`

`besoin`

- `id`
- `id_type_besoin` (FK → type_besoin.id)
- `id_poste` (FK → poste.id)
- `id_utilisateur_demandeur` (FK → utilisateur.id)
- `effectif`
- `date_arrivee_souhaitee`
- `date_demande`
- `lieu_travail`
- `motif`
- `experience_requis`
- `salaire_estime`
<!-- - `statut_global` -->
- `date_modification`

## 4. Circuit de validation

`etat_validation`

- `id`
- `code` (ex. : RH, FINANCE, DIRECTION)
- `libelle`
- `ordre`
id	code	libelle	ordre
1	RH	Validation RH	1
2	FINANCE	Validation Finance	2
3	DIRECTION	Validation Direction	3

`validation_besoin` (remplace `rel_besoin_etat`)

- `id`
- `id_besoin` (FK → besoin.id)
- `id_etat_validation` (FK → etat_validation.id)
- `id_utilisateur_validateur` (FK → utilisateur.id, nullable tant que personne n'a décidé)
- `decision` (EN_ATTENTE, ACCEPTE, REFUSE)
- `date_decision` (nullable)
- `remarque` (facultatif)

Prévoir une contrainte d'unicité sur `(id_besoin, id_etat_validation)` si chaque étape ne doit avoir qu'une seule validation par demande.

## 5. Gestion des candidats et des CV

`candidat`

- `id`
- `nom`
- `prenom`
- `email`
- `telephone`
- `numero_cin` (unique si renseigné)
- `date_creation`

`cv`

- `id`
- `id_candidat` (FK → candidat.id)
- `details` (informations structurées du CV)
- `nom_fichier`
- `type_mime`
- `chemin_fichier` (ou `data` si tu stockes le document en base)
- `date_depot`
- `statut_lecture` (LU, NON_LU)

`candidature` (recommandée si un candidat peut postuler à plusieurs recrutements)

- `id`
- `id_candidat` (FK → candidat.id)
- `id_besoin` (FK → besoin.id)
- `id_cv` (FK → cv.id, facultatif)
- `date_candidature`
- `statut` (EN_ATTENTE, PRESELECTIONNE, RETENU, REJETE)
- `remarque`

## 6. Gestion des employés

`employe`

- `id`
- `id_candidat` (FK → candidat.id, unique si un candidat ne peut devenir employé qu'une seule fois)
- `id_departement` (FK → departement.id)
- `id_poste` (FK → poste.id)
- `date_embauche`
- `statut` (ACTIF, INACTIF, PARTI)

Cette table servira à calculer l'effectif actuel des départements et à alimenter les tableaux de bord RH.

## 7. Gestion des contrats

`contrat`

- `id`
- `id_employe` (FK → employe.id)
- `type_contrat`
- `date_debut`
- `date_fin` (nullable pour un contrat sans date de fin prévue)
- `salaire`
- `statut` (BROUILLON, EN_ATTENTE_SIGNATURE, SIGNE, ANNULE)
- `chemin_document` (facultatif)
- `date_creation`

Pour l'envoi futur d'un lien au candidat, tu pourras ajouter une table dédiée aux invitations de contrat, avec un jeton sécurisé, une date d'expiration et une date d'utilisation. Évite de stocker un lien public permanent donnant accès au contrat.

## 8. Gestion des budgets et des dépenses

`budget_departement`

- `id`
- `id_departement` (FK → departement.id)
- `annee`
- `montant`
- `devise`

`depense_departement`

- `id`
- `id_departement` (FK → departement.id)
- `montant`
- `date_depense`
- `libelle`
- `categorie`
- `statut`

Le calcul budgétaire devra distinguer les dépenses enregistrées, les coûts salariaux et les recrutements prévus afin d'éviter de compter deux fois une même dépense.

## 9. Gestion des notifications

`notification`

- `id`
- `id_utilisateur` (FK → utilisateur.id)
- `id_besoin` (FK → besoin.id, nullable)
- `type_notification`
- `titre`
- `message`
- `date_creation`
- `date_lecture` (nullable)
- `statut` (NON_LUE, LUE)

Cette table permet notamment de notifier les RH lorsqu'une demande est créée, Finance lorsqu'une demande est prête à être analysée, ou le manager lorsque la décision finale est prise.

## Points à retenir pour la conception

1. Effectif actuel : calculé depuis `employe`, avec une règle précise sur les statuts comptabilisés.
2. Validation : chaque étape possède sa propre décision, tandis que `besoin.statut_global` représente l'état général de la demande.
3. Candidat et employé : un candidat peut exister sans être recruté ; l'employé est créé lorsque l'embauche est confirmée.
4. CV et candidature : le CV est un document ; la candidature représente la participation à un recrutement précis.
5. Sécurité : les mots de passe sont hachés et les liens de formulaire de contrat doivent être temporaires et protégés.
