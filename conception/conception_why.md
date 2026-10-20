Voici donc l’explication détaillée des 30 tables de ton script PostgreSQL, en suivant leur rôle dans le système. Pour chacune, nous allons voir :

- Qu’est-ce que c’est ? Sa signification dans l’entreprise.
- À quoi sert-elle ? Les opérations qu’elle permet de gérer.
- Pourquoi est-elle structurée ainsi ? Le rôle des colonnes, des clés et des contraintes.

# 1. Les tables de référence

Ces tables définissent des listes de valeurs utilisées par les autres tables. Elles évitent de répéter les mêmes informations partout et facilitent les modifications.

## 1. `role`

Qu’est-ce que c’est ?

Cette table définit les rôles des utilisateurs de l’application.

Exemples : `MANAGER`, `RH`, `FINANCE`, `DIRECTION`.

À quoi sert-elle ?

Elle permet au système de savoir quelles opérations un utilisateur est autorisé à effectuer. Par exemple, un manager peut soumettre une demande de recrutement, tandis que le service RH peut gérer les candidatures et les contrats.

Pourquoi cette structure ?

- `id` : identifiant technique du rôle.
- `code` : identifiant stable utilisé par le backend.
- `libelle` : nom lisible du rôle.

La contrainte `UNIQUE` sur `code` empêche d'enregistrer deux fois le même rôle.

Le rôle stocké dans la base ne suffit pas, à lui seul, à sécuriser l'application : le backend doit vérifier les permissions à chaque opération protégée.

## 2. `type_besoin`

Qu’est-ce que c’est ?

La liste des catégories de demandes de personnel.

Exemples : `REMPLACEMENT`, `RENFORT`, `CREATION`.

À quoi sert-elle ?

Elle permet de comprendre pourquoi un département demande de nouveaux employés.

Pourquoi cette structure ?

Les colonnes `id`, `code` et `libelle` séparent l'identifiant technique, le code métier et le texte affiché à l'utilisateur.

Ainsi, si le libellé « Renforcement de l'effectif » change, les relations avec les autres tables ne changent pas.

## 3. `etat_validation`

Qu’est-ce que c’est ?

Cette table décrit les étapes du circuit de validation d'un besoin.

Dans ton script, les étapes sont :

1. RH ;
2. Finance ;
3. Direction.

À quoi sert-elle ?

Elle permet de définir les différentes étapes que chaque demande doit traverser avant d'être acceptée.

Pourquoi cette structure ?

- `code` identifie l'étape.
- `libelle` explique son rôle.
- `ordre` indique sa position dans le processus.

La contrainte `UNIQUE` sur `ordre` empêche deux étapes d'avoir le même numéro.

Attention : cette table définit l'ordre, mais le backend doit encore empêcher une validation financière avant la validation RH.

## 4. `decision`

Qu’est-ce que c’est ?

La liste des décisions possibles pour une étape de validation d'un besoin.

Dans ton script : `EN_ATTENTE`, `ACCEPTE` et `REFUSE`.

À quoi sert-elle ?

Elle permet de représenter le résultat de chaque étape de validation.

Pourquoi cette structure ?

Cette table est distincte de `etat_validation`, car une étape et une décision ne représentent pas la même chose.

Par exemple, `FINANCE` est une étape, tandis que `ACCEPTE` est une décision que Finance peut prendre.

## 5. `statut_contract`

Qu’est-ce que c’est ?

La liste des états possibles d'un contrat de travail.

Exemples : `BROUILLON`, `EN_ATTENTE_SIGNATURE`, `ACTIF`, `TERMINE`, `RESILIE`.

À quoi sert-elle ?

Elle permet de suivre le cycle de vie d'un contrat, de sa préparation à sa fin.

Pourquoi cette structure ?

La table `contrat` référence un statut par son identifiant au lieu de répéter les libellés dans chaque contrat.

Cette séparation facilite la gestion des statuts et évite les différences d'orthographe entre les enregistrements.

## 6. `type_contrat`

Qu’est-ce que c’est ?

Le catalogue des types de contrats proposés par l'entreprise.

Exemples : CDI, CDD et stage.

À quoi sert-elle ?

Elle permet de savoir quelle catégorie de contrat est associée à une embauche.

Pourquoi cette structure ?

La colonne `duree` contient une durée indicative en jours. Elle peut être `NULL`, notamment pour un CDI à durée indéterminée.

Le fait d'enregistrer cette durée ne calcule pas automatiquement la date de fin du contrat : cette règle doit être appliquée par le backend ou par une logique SQL complémentaire.

# 2. Les tables des utilisateurs et de l'organisation

## 7. `utilisateur`

Qu’est-ce que c’est ?

Cette table contient les comptes qui permettent aux personnes de se connecter à l'application.

À quoi sert-elle ?

Elle permet de gérer l'authentification, le rôle de chaque utilisateur, la désactivation d'un compte et l'identification de la personne qui réalise une action.

Pourquoi cette structure ?

- `id_role` relie le compte à un rôle.
- `email` sert d'identifiant de connexion et doit être unique.
- `mdp` contient le hash du mot de passe, jamais le mot de passe en clair.
- `actif` permet de désactiver un compte sans supprimer son historique.
- `date_creation` indique quand le compte a été créé.

Un utilisateur n'est pas forcément un employé : un compte technique ou un intervenant externe peut aussi être autorisé à accéder à certaines fonctions.

## 8. `departement`

Qu’est-ce que c’est ?

Cette table représente les unités organisationnelles de l'entreprise.

Exemples : Informatique, Finance, Ressources humaines.

À quoi sert-elle ?

Elle permet de regrouper les employés, les postes, les budgets et les dépenses par département.

Pourquoi cette structure ?

- `id_utilisateur_manager` identifie le responsable du département.
- `nom` est unique pour éviter les doublons.
- `effectif_max` représente le plafond d'effectif prévu.

L'effectif réel n'est pas stocké ici : il peut être calculé à partir de la table `employe`. Cela évite de conserver un compteur qui pourrait devenir incorrect lorsque des employés arrivent ou partent.

## 9. `poste`

Qu’est-ce que c’est ?

Cette table contient les postes de travail disponibles dans chaque département.

Exemples : Développeur Java au département Informatique, Comptable au département Finance.

À quoi sert-elle ?

Elle permet de définir les fonctions pour lesquelles l'entreprise peut recruter et de rattacher les employés à un poste.

Pourquoi cette structure ?

- `id_departement` indique à quel département appartient le poste.
- `nom` désigne le poste.
- `actif` permet de retirer un poste du catalogue sans supprimer les données historiques.

La contrainte `UNIQUE (id_departement, nom)` permet à plusieurs départements d'avoir un poste portant le même nom, tout en interdisant les doublons dans un même département.

La contrainte `UNIQUE (id, id_departement)` permet aussi à `employe` de vérifier que le poste et le département correspondent bien.

## 10. `grille_salaire`

Qu’est-ce que c’est ?

Cette table définit les fourchettes de rémunération autorisées pour les différents postes, sur des périodes déterminées.

À quoi sert-elle ?

Elle aide le service RH à vérifier si le salaire proposé pour un recrutement ou un contrat respecte la politique de rémunération de l'entreprise.

Pourquoi cette structure ?

- `id_poste` indique le poste concerné.
- `valeur_min` et `valeur_max` définissent la fourchette salariale.
- `date_debut_validite` et `date_fin_validite` définissent la période d'application.

La contrainte d'exclusion utilisant `btree_gist` empêche deux grilles du même poste d'avoir des périodes qui se chevauchent.

Par exemple, une grille peut s'appliquer en 2026 et une autre en 2027, mais deux grilles du même poste ne peuvent pas couvrir simultanément les mêmes dates.

La base vérifie les périodes et les valeurs, mais le backend doit aussi vérifier que le salaire d'un contrat correspond à la grille valable à la date concernée.

# 3. Les tables de l'expression du besoin

## 11. `employe`

Qu’est-ce que c’est ?

Cette table représente les personnes qui travaillent ou ont travaillé dans l'entreprise.

À quoi sert-elle ?

Elle permet de gérer les informations personnelles et professionnelles des employés, leur affectation et leur situation actuelle.

Pourquoi cette structure ?

- `matricule` est l'identifiant interne de l'employé.
- `nom`, `prenom`, `email`, `telephone`, `numero_cin`, `date_naissance` et `adresse` décrivent son identité.
- `id_departement` indique son département.
- `id_poste` indique son poste.
- `id_candidature` conserve le lien avec la candidature qui a conduit à son embauche, s'il y en a une.
- `id_utilisateur` relie l'employé à son compte applicatif, s'il en possède un.
- `date_embauche`, `date_depart` et `statut` permettent de suivre sa situation.

La clé étrangère composée `(id_poste, id_departement)` garantit que l'employé ne peut pas être affecté à un poste appartenant à un autre département.

Le lien vers `candidature` est facultatif pour permettre, par exemple, d'enregistrer un employé déjà présent dans l'entreprise ou embauché sans candidature enregistrée dans le système.

## 12. `besoin`

Qu’est-ce que c’est ?

Cette table enregistre une demande de personnel formulée par un manager.

À quoi sert-elle ?

Elle constitue le point de départ du processus de recrutement. Elle permet de savoir qui demande du personnel, pour quel poste, en quelle quantité et pour quelle raison.

Pourquoi cette structure ?

- `id_type_besoin` indique s'il s'agit d'un remplacement, d'un renfort ou d'une création de poste.
- `id_poste` indique le poste demandé.
- `id_utilisateur_demandeur` identifie le demandeur.
- `id_employe_remplace` indique la personne remplacée, si nécessaire.
- `id_type_contrat_souhaite` précise le type de contrat envisagé.
- `effectif` indique le nombre de personnes demandées.
- `priorite` permet de classer les demandes.
- `date_arrivee_souhaitee` indique la date d'arrivée souhaitée.
- `motif`, `experience_requise`, `niveau_etude` et `competences_requises` décrivent le besoin.
- `salaire_estime` sert à préparer l'évaluation budgétaire.
- `statut_global` résume l'état général du processus.

La table contient un statut global pour faciliter les recherches, par exemple afficher toutes les demandes en attente de validation RH. Ce statut doit être synchronisé avec les étapes de validation par le backend.

## 13. `validation_besoin`

Qu’est-ce que c’est ?

Cette table conserve le résultat de chaque étape de validation d'un besoin.

À quoi sert-elle ?

Elle permet de savoir qui a validé ou refusé une demande, à quelle étape, à quelle date et avec quelle remarque.

Pourquoi cette structure ?

- `id_besoin` identifie la demande concernée.
- `id_etat_validation` identifie l'étape, comme RH ou Finance.
- `id_decision` indique le résultat de l'étape.
- `id_utilisateur_validateur` indique qui a pris la décision.
- `date_decision` enregistre la date de la décision.
- `remarque` permet d'expliquer la décision.

La contrainte `UNIQUE (id_besoin, id_etat_validation)` empêche d'enregistrer deux lignes pour la même étape d'un même besoin.

Les colonnes du validateur et de la date peuvent être nulles lorsque l'étape est en attente. En revanche, une étape terminée doit avoir un validateur et une date.

Pourquoi ne pas mettre toutes les validations directement dans `besoin` ?

Parce qu'un besoin peut passer par plusieurs étapes. Avec une table dédiée, on peut ajouter de nouvelles étapes sans ajouter une nouvelle colonne à chaque fois.

# 4. Les tables du recrutement

## 14. `recrutement`

Qu’est-ce que c’est ?

Cette table représente une campagne de recrutement ouverte à partir d'un besoin accepté.

À quoi sert-elle ?

Elle permet de suivre le recrutement comme une opération distincte de la demande initiale.

Par exemple, un besoin de deux développeurs peut donner lieu à une campagne de recrutement identifiée par la référence `REC-2026-007`.

Pourquoi cette structure ?

- `id_besoin` relie la campagne à la demande d'origine.
- `reference` donne un identifiant métier lisible.
- `titre` et `description` présentent le recrutement.
- `date_ouverture` et `date_cloture` définissent sa période.
- `statut` indique s'il est en brouillon, ouvert, suspendu, terminé ou annulé.
- `id_utilisateur_responsable` identifie le responsable.

La contrainte `UNIQUE` sur `id_besoin` impose ici une campagne par besoin. C'est un choix de conception de ton projet : si l'entreprise doit pouvoir relancer plusieurs campagnes pour une même demande, cette contrainte devra être retirée.

## 15. `offre_emploi`

Qu’est-ce que c’est ?

Cette table contient l'annonce détaillée qui sera présentée aux candidats.

À quoi sert-elle ?

Elle permet de décrire les missions, les compétences recherchées, le niveau d'étude, l'expérience et éventuellement la rémunération proposée.

Pourquoi cette structure ?

- `id_recrutement` relie l'offre à sa campagne.
- `titre`, `description_poste` et `missions` décrivent le poste.
- `competences_requises`, `niveau_etude` et `experience_min_annees` précisent le profil recherché.
- `salaire_min` et `salaire_max` indiquent la fourchette salariale.
- `statut` permet de gérer le brouillon, la validation et l'archivage de l'offre.

La contrainte `UNIQUE` sur `id_recrutement` impose une seule offre par campagne. Les différentes plateformes de diffusion sont gérées séparément dans `publication_offre`.

## 16. `publication_offre`

Qu’est-ce que c’est ?

Cette table représente la diffusion d'une offre sur une plateforme particulière.

À quoi sert-elle ?

Elle permet de savoir où et quand une offre a été publiée, si elle est encore publiée et quelle source a amené une candidature.

Pourquoi cette structure ?

- `id_offre` identifie l'offre diffusée.
- `plateforme` indique le canal de diffusion : site carrière, LinkedIn, etc.
- `url_publication` conserve le lien de l'annonce.
- `date_publication` et `date_expiration` suivent sa période de diffusion.
- `statut` indique si la publication est en brouillon, publiée, expirée ou retirée.

Une offre peut être diffusée sur plusieurs plateformes. Cette relation est dite un-à-plusieurs entre `offre_emploi` et `publication_offre`.

# 5. Les tables de sélection des candidats

## 17. `candidat`

Qu’est-ce que c’est ?

Cette table contient les informations des personnes qui postulent à un recrutement.

À quoi sert-elle ?

Elle permet d'enregistrer un candidat une seule fois, même s'il postule à plusieurs offres.

Pourquoi cette structure ?

Les informations de contact et d'identité sont regroupées dans cette table : nom, prénom, email, téléphone, CIN et expérience.

L'email est unique pour limiter les doublons. Le CIN est également unique lorsqu'il est renseigné.

Les informations de candidature ne sont pas toutes enregistrées ici, car une personne peut avoir plusieurs candidatures avec des statuts différents.

## 18. `cv`

Qu’est-ce que c’est ?

Cette table conserve les CV transmis par les candidats.

À quoi sert-elle ?

Elle permet au service RH de consulter le document associé à une candidature et de suivre si le CV a déjà été lu.

Pourquoi cette structure ?

- `id_candidat` identifie le propriétaire du CV.
- `nom_fichier` et `mime_type` décrivent le fichier.
- `bytes` contient son contenu binaire.
- `date_depot` indique quand il a été transmis.
- `statut_lecture` permet de savoir s'il a été lu.

La contrainte `UNIQUE (id, id_candidat)` permet à la table `candidature` de vérifier que le CV sélectionné appartient bien au candidat concerné.

Le stockage en `BYTEA` permet de conserver le fichier dans PostgreSQL. Pour de gros volumes, un stockage de fichiers séparé peut être envisagé.

## 19. `candidature`

Qu’est-ce que c’est ?

Cette table représente la participation d'un candidat à une campagne de recrutement.

À quoi sert-elle ?

Elle permet de gérer le suivi de chaque candidature indépendamment du profil général du candidat.

Pourquoi cette structure ?

- `id_candidat` identifie la personne.
- `id_recrutement` identifie la campagne.
- `id_cv` indique le CV utilisé.
- `id_publication` conserve la source de la candidature, si elle est connue.
- `date_candidature` indique quand la candidature a été déposée.
- `statut` représente l'avancement du dossier.
- `score` peut servir au pré-tri.
- `remarque` conserve des observations.

La contrainte `UNIQUE (id_candidat, id_recrutement)` interdit à un candidat de déposer plusieurs candidatures pour le même recrutement.

Le statut de candidature et la décision finale sont également représentés dans `decision_recrutement`. Il faut donc les synchroniser dans une même transaction, ou revoir cette duplication si tu veux une seule source de vérité.

## 20. `entretien`

Qu’est-ce que c’est ?

Cette table enregistre les entretiens et les évaluations réalisés pendant la sélection.

À quoi sert-elle ?

Elle permet de planifier des entretiens, d'identifier les intervieweurs, d'enregistrer les notes et de conserver les commentaires.

Pourquoi cette structure ?

- `id_candidature` relie l'entretien à un dossier précis.
- `id_utilisateur_intervieweur` identifie l'intervieweur.
- `type_entretien` précise la nature de l'entretien.
- `date_entretien` et `lieu` décrivent sa planification.
- `statut` indique s'il est planifié, réalisé ou annulé.
- `note` enregistre une évaluation sur 20.
- `commentaire` contient les observations.

Une candidature peut avoir plusieurs entretiens : un test technique, un entretien RH et un entretien avec la direction, par exemple. C'est pourquoi les entretiens possèdent leur propre table.

## 21. `decision_recrutement`

Qu’est-ce que c’est ?

Cette table conserve la décision finale prise à propos d'une candidature.

À quoi sert-elle ?

Elle permet d'enregistrer si le candidat est retenu, rejeté ou placé sur liste d'attente, et de conserver la justification de la décision.

Pourquoi cette structure ?

- `id_candidature` identifie le dossier évalué.
- `id_utilisateur_decideur` identifie la personne ayant pris la décision.
- `decision` contient le résultat.
- `motif` explique la décision.
- `date_decision` enregistre sa date.
- `date_creation` indique quand la ligne de décision a été créée.

La contrainte `UNIQUE` sur `id_candidature` impose une seule ligne de décision par candidature. Cette ligne peut d'abord être en attente, puis être mise à jour.

Une décision définitive exige une date et un décideur. La vérification du nombre de candidats retenus par rapport à l'effectif demandé doit également être effectuée côté backend dans une transaction adaptée.

# 6. Les tables des contrats

## 22. `contrat`

Qu’est-ce que c’est ?

Cette table contient les contrats de travail des employés.

À quoi sert-elle ?

Elle permet de gérer les informations contractuelles, de suivre l'état du contrat et de conserver le document correspondant.

Pourquoi cette structure ?

- `reference` identifie le contrat.
- `id_employe` désigne l'employé concerné.
- `id_type_contrat` indique s'il s'agit d'un CDI, d'un CDD ou d'un stage.
- `id_statut_contrat` indique l'état du contrat.
- `id_contrat_precedent` relie un contrat à un précédent contrat, par exemple lors d'un renouvellement.
- `date_debut`, `date_fin` et `date_signature` suivent les dates importantes.
- `periode_essai_jours` représente la durée de la période d'essai.
- `salaire` enregistre la rémunération.
- `contenu`, `nom_fichier` et `mime_type` permettent de stocker le document contractuel.

Les contraintes vérifient notamment la cohérence des dates et des métadonnées du fichier. Un contrôle supplémentaire empêche d'avoir deux contrats marqués `ACTIF` pour le même employé.

Le respect de la grille salariale et les règles juridiques applicables doivent être vérifiés séparément.

## 23. `historique_contrat`

Qu’est-ce que c’est ?

Cette table conserve la trace des modifications importantes apportées aux contrats.

À quoi sert-elle ?

Elle permet de savoir ce qui a changé, quand, pourquoi et par qui.

Pourquoi cette structure ?

- `id_contrat` désigne le contrat concerné.
- `id_ancien_statut` et `id_statut_contrat` permettent de conserver l'ancien et le nouveau statut.
- `ancien_salaire` et `nouveau_salaire` permettent de tracer une modification de rémunération.
- `ancienne_date_fin` et `nouvelle_date_fin` permettent de tracer un changement de date.
- `evenement` décrit la nature de la modification.
- `motif` explique la raison.
- `id_utilisateur_auteur` identifie l'auteur.

Cette table est utile pour l'audit et la traçabilité. Pour qu'elle joue réellement ce rôle, le backend doit y insérer les événements dans la même transaction que la modification du contrat et empêcher les modifications ou suppressions non autorisées de l'historique.

# 7. Les tables de l'onboarding et des documents

## 24. `modele_tache_onboarding`

Qu’est-ce que c’est ?

Cette table contient les modèles de tâches à effectuer lors de l'arrivée d'un nouvel employé.

À quoi sert-elle ?

Elle évite au service RH de recréer manuellement la même liste de tâches pour chaque nouvelle embauche.

Exemples : créer les comptes informatiques, remettre le matériel, récupérer les documents administratifs et organiser une formation d'intégration.

Pourquoi cette structure ?

- `code` identifie le modèle.
- `libelle` décrit la tâche.
- `categorie` classe la tâche.
- `delai_jours` indique le délai prévu après le début de l'intégration.
- `actif` permet de désactiver un modèle sans supprimer les anciennes données.

Les modèles servent de catalogue. Ils ne représentent pas encore les tâches réellement attribuées à un employé.

## 25. `onboarding`

Qu’est-ce que c’est ?

L'onboarding est le processus organisé d'accueil et d'intégration d'un nouvel employé.

Cette table représente le dossier général d'intégration d'une personne.

À quoi sert-elle ?

Elle permet de suivre l'intégration d'un employé après son embauche : démarrage, responsable, date prévue de fin et état général.

Pourquoi cette structure ?

- `id_employe` désigne la personne intégrée.
- `id_utilisateur_responsable` identifie le responsable de l'intégration.
- `date_debut` indique le début du processus.
- `date_fin_prevue` fixe un objectif de fin.
- `statut` indique si l'intégration est à faire, en cours ou terminée.

La contrainte `UNIQUE (id_employe)` impose un seul dossier d'onboarding par employé.

## 26. `tache_onboarding`

Qu’est-ce que c’est ?

Cette table contient les tâches concrètes d'intégration affectées à un employé.

À quoi sert-elle ?

Elle permet de suivre l'exécution réelle du processus : une tâche peut être à faire, en cours, terminée ou annulée.

Pourquoi cette structure ?

- `id_onboarding` rattache la tâche au dossier d'intégration.
- `id_modele` indique de quel modèle elle provient, s'il y en a un.
- `id_utilisateur_responsable` identifie la personne chargée de la tâche.
- `libelle` et `categorie` décrivent le travail à réaliser.
- `date_echeance` fixe la date limite.
- `statut` indique l'avancement.
- `date_realisation` indique quand la tâche a été terminée.
- `remarque` permet d'ajouter des précisions.

La relation avec `modele_tache_onboarding` est facultative, car une tâche peut aussi être ajoutée manuellement.

La contrainte de suppression en cascade supprime les tâches lorsque leur dossier d'onboarding est supprimé. En pratique, si tu veux conserver l'historique RH, il est préférable de clôturer les dossiers plutôt que de les supprimer.

## 27. `document_employe`

Qu’est-ce que c’est ?

Cette table conserve les documents administratifs associés au dossier d'un employé.

À quoi sert-elle ?

Elle permet de gérer les pièces nécessaires à l'embauche et au suivi administratif : CIN, diplômes, RIB, CV, certificat médical, photo ou autres documents.

Pourquoi cette structure ?

- `id_employe` indique le propriétaire du document.
- `type_document` précise sa catégorie.
- `nom_fichier` et `mime_type` décrivent le fichier.
- `bytes` contient le document.
- `statut` indique s'il a été fourni, validé ou refusé.
- `date_depot` indique quand il a été transmis.

La séparation dans une table spécifique permet à un employé de posséder plusieurs documents de types différents.

Point important : les CIN, RIB et certificats médicaux sont des données sensibles. L'accès à ces fichiers doit être limité aux personnes autorisées.

# 8. Les tables de budget, de dépenses et de notifications

## 28. `budget_departement`

Qu’est-ce que c’est ?

Cette table enregistre le budget attribué à chaque département pour une année donnée.

À quoi sert-elle ?

Elle permet au service Finance de connaître l'enveloppe budgétaire disponible pour les dépenses du département, notamment les dépenses liées au recrutement.

Pourquoi cette structure ?

- `id_departement` identifie le département bénéficiaire.
- `annee` précise l'exercice budgétaire.
- `montant` indique le budget attribué.
- `devise` précise la monnaie utilisée, `MGA` par défaut dans ton script.

La contrainte `UNIQUE (id_departement, annee)` impose un seul budget par département et par année.

Le budget restant peut être calculé en soustrayant les dépenses enregistrées du budget attribué. Le backend doit vérifier ce montant lors de l'approbation d'une dépense ou d'un besoin.

## 29. `depense_departement`

Qu’est-ce que c’est ?

Cette table enregistre les dépenses imputées aux départements.

À quoi sert-elle ?

Elle permet de suivre les montants dépensés, de les rattacher à leur origine et de calculer la consommation budgétaire.

Pourquoi cette structure ?

- `id_departement` désigne le département concerné.
- `id_besoin` permet de rattacher une dépense à un besoin de recrutement.
- `id_contrat` permet de rattacher une dépense à un contrat.
- `libelle` et `categorie` décrivent la dépense.
- `montant` indique la somme dépensée.
- `date_depense` indique la date de la dépense.
- `annee` est une colonne générée à partir de `date_depense`.

La colonne `annee` est générée automatiquement par PostgreSQL pour éviter qu'une dépense datée de 2026 soit enregistrée manuellement dans l'exercice 2025.

Le script interdit aussi de rattacher une même dépense simultanément à un besoin et à un contrat. Si ton entreprise souhaite autoriser ce cas, cette contrainte devra être adaptée.

## 30. `notification`

Qu’est-ce que c’est ?

Cette table enregistre les notifications internes envoyées aux utilisateurs de l'application.

À quoi sert-elle ?

Elle permet d'informer les utilisateurs des événements importants : nouveau besoin à valider, décision prise, nouvelle candidature ou changement de statut.

Pourquoi cette structure ?

- `id_utilisateur` indique le destinataire.
- `id_besoin` permet de rattacher la notification à un besoin.
- `id_candidature` permet de la rattacher à une candidature.
- `type_notification` classe la notification.
- `titre` et `message` décrivent l'information.
- `date_creation` indique quand elle a été créée.
- `date_lecture` indique quand elle a été lue.

Lorsque `date_lecture` est `NULL`, la notification n'a pas encore été marquée comme lue.

Les index sur l'utilisateur et la date permettent de retrouver rapidement ses notifications récentes et non lues.

# 9. Pourquoi le script contient aussi des index, des contraintes et des données initiales ?

Ces éléments ne sont pas des tables métier supplémentaires, mais ils sont essentiels au bon fonctionnement de la base.

| Élément                  | À quoi sert-il ?                                                                | Pourquoi est-il utile ?                                     |
| ------------------------ | ------------------------------------------------------------------------------- | ----------------------------------------------------------- |
| `PRIMARY KEY`            | Identifie chaque ligne de façon unique.                                         | Permet de référencer précisément un enregistrement.         |
| `FOREIGN KEY`            | Relie une table à une autre.                                                    | Évite les références vers des enregistrements inexistants.  |
| `UNIQUE`                 | Interdit les doublons sur certaines colonnes.                                   | Évite, par exemple, deux comptes avec le même email.        |
| `CHECK`                  | Vérifie une condition sur les données.                                          | Interdit un effectif nul ou une note supérieure à 20.       |
| `CREATE INDEX`           | Accélère certaines recherches et jointures.                                     | Améliore les performances des listes et filtres fréquents.  |
| `INSERT ... ON CONFLICT` | Insère les valeurs de référence sans recréer un code déjà présent.              | Facilite la réinitialisation des référentiels.              |
| `CREATE TRIGGER`         | Exécute un contrôle automatiquement lors d'une insertion ou d'une modification. | Permet de vérifier certaines règles entre plusieurs tables. |

Les index accélèrent certaines requêtes, mais chaque index supplémentaire utilise de l'espace et peut ralentir les insertions ou mises à jour. Ils doivent donc correspondre aux recherches réellement effectuées par l'application.

## 10. Comment les tables travaillent ensemble ?

Voici le parcours général d'une demande jusqu'à l'intégration de la personne recrutée.

1\. Demande

besoin

2\. Validation

validation_besoin

3\. Campagne et offre

recrutement → offre_emploi → publication_offre

4\. Candidature

candidat → cv → candidature

5\. Sélection

entretien → decision_recrutement

6\. Embauche

employe → contrat → historique_contrat

7\. Intégration

onboarding → tache_onboarding → document_employe

Les tables `role`, `utilisateur`, `departement`, `poste`, `grille_salaire`, `budget_departement`, `depense_departement` et `notification` soutiennent plusieurs étapes de ce parcours.

À retenir : une bonne base de données ne sert pas seulement à stocker des informations. Elle organise les relations entre les informations, protège leur cohérence et facilite les opérations de l'entreprise. Les règles métier plus complexes — comme l'ordre des validations, le plafond de recrutement ou la disponibilité du budget — nécessitent aussi une implémentation correcte dans le backend.