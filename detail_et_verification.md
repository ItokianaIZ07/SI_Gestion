# Fonctionnalités à réaliser et vérifications, par bloc

## Bloc 5 : dossier administratif du salarié

### Fonctionnalités
- [ ] **Organigramme** : afficher la hiérarchie en arbre ; sur la fiche de chaque salarié, montrer son supérieur et les personnes sous sa responsabilité (directes et indirectes).
- [ ] **Fiche salarié centralisée** : identité, coordonnées, affectation actuelle, contrat actif, salaire, documents, historique de carrière.
- [ ] **Changement d'affectation** : muter un salarié (département, poste, supérieur, date, motif) avec conservation de l'ancienne affectation.
- [ ] **Historique de carrière** : frise chronologique des affectations d'un salarié.
- [ ] **Contrats** : créer et suivre les contrats, avec un seul contrat actif par salarié.
- [ ] **Documents administratifs** : déposer un document avec sa date d'expiration, son délai d'alerte et sa date de renouvellement ; indicateur valide / bientôt expiré / expiré.
- [ ] **Alertes de renouvellement** : notification envoyée au salarié s'il a un espace personnel, sinon aux RH ; page de notifications avec marquage « lu ».
- [ ] **Traçabilité des tâches** : historique de chaque tâche d'onboarding (ancien/nouveau statut, échéance, remarque, auteur, date).
- [ ] **Journal d'audit** : consultation en lecture seule, filtrable par utilisateur, table et enregistrement.
- [ ] **Départ d'un salarié** : marquer comme parti avec une date de départ.

### Vérifications
| # | Scénario | Résultat attendu |
|---|---|---|
| 5.1 | Ouvrir la fiche d'un salarié qui a un supérieur et deux subordonnés | Le supérieur et les deux subordonnés sont affichés |
| 5.2 | Ouvrir l'organigramme | Chaque salarié non parti apparaît sous son supérieur, avec son nombre de subordonnés directs |
| 5.3 | Désigner A comme supérieur de B, puis B comme supérieur de A (ou de C, qui est sous B) | Refus avec message de boucle hiérarchique |
| 5.4 | Désigner comme supérieur un salarié déjà parti | Refus |
| 5.5 | Muter un salarié vers un autre département sans choisir de supérieur | Le manager du nouveau département est pris par défaut |
| 5.6 | Muter un salarié, puis ouvrir sa frise de carrière | Ancienne affectation clôturée à la date de mutation, nouvelle ouverte, ancienne toujours visible avec son motif |
| 5.7 | Muter un salarié parti | Refus |
| 5.8 | Créer un deuxième contrat actif pour le même salarié | Refus |
| 5.9 | Déposer un document expirant dans 30 jours ou moins, non renouvelé | Une alerte apparaît |
| 5.10 | Relancer la génération d'alertes dans les 7 jours | Aucune alerte en double |
| 5.11 | Document d'un salarié **sans** compte actif | L'alerte arrive chez tous les utilisateurs RH |
| 5.12 | Document d'un salarié **avec** compte actif | L'alerte arrive chez lui uniquement |
| 5.13 | Renseigner la date de renouvellement | Plus aucune alerte pour ce document |
| 5.14 | Déposer un document sans date d'expiration | Aucune alerte (comportement attendu, à connaître) |
| 5.15 | Marquer une notification comme lue | La date de lecture est enregistrée |
| 5.16 | Passer une tâche d'onboarding à « fait » sans date de réalisation | Refus |
| 5.17 | Modifier le statut, l'échéance ou la remarque d'une tâche | Une ligne d'historique avec l'auteur et l'ancienne/nouvelle valeur |
| 5.18 | Enregistrer une tâche sans rien changer | Aucune ligne d'historique |
| 5.19 | Modifier un employé puis consulter l'audit | Une entrée avec l'auteur, l'action, les valeurs avant/après ; aucun mot de passe visible |
| 5.20 | Tenter de modifier ou supprimer une entrée d'audit | Refus |
| 5.21 | Marquer un salarié parti sans date de départ | Refus |
| 5.22 | Marquer un salarié parti avec une date | Son affectation, ses droits de congé et son planning s'arrêtent à cette date ; il disparaît de l'organigramme |

## Bloc 6 : temps de travail et pointage

### Fonctionnalités
- [ ] **Régimes de travail** : temps plein, temps partiel, heures hebdomadaires.
- [ ] **Horaires** : heures de début et de fin, pause, tolérance de retard, jours travaillés.
- [ ] **Jours fériés** : liste gérable, exclue du temps de travail prévu.
- [ ] **Planning** : affecter un horaire et un régime à un salarié sur une période.
- [ ] **Pointage** : entrée, début de pause, fin de pause, sortie ; liste des pointages du jour.
- [ ] **Temps réellement travaillé** : calcul automatique par jour.
- [ ] **Retards** : calcul en minutes, avec tolérance.
- [ ] **Heures supplémentaires** : calcul, puis validation ou refus.
- [ ] **Anomalies de pointage** : détection, suivi (à traiter, en cours, résolue, ignorée), justification, responsable.
- [ ] **Variables de paie** : récapitulatif mensuel par salarié, exportable.

### Vérifications
Données de départ pour les tests : horaire 08h00‑17h00, pause 12h00‑13h00, tolérance 10 min (soit 480 min prévues).

| # | Scénario | Résultat attendu |
|---|---|---|
| 6.1 | Entrée 08h00, pause 12h00‑13h00, sortie 17h00 | Réel 480 min, retard 0, heures sup 0 |
| 6.2 | Entrée 08h08 | Retard 0 (dans la tolérance) |
| 6.3 | Entrée 08h25 | Retard 15 min |
| 6.4 | Même journée, sortie à 18h00 | Heures sup 60 min, statut « à valider » |
| 6.5 | Valider ces heures sup | Statut « validée », validateur et date enregistrés |
| 6.6 | Recalculer la journée sans rien changer | La décision déjà prise est conservée |
| 6.7 | Modifier un pointage après validation (volume différent) | Les heures sup repassent à « à valider » |
| 6.8 | Pointer un jour férié | Temps prévu 0, tout le temps travaillé compte en heures sup à valider |
| 6.9 | Salarié en congé, absence ou mission ce jour-là | Temps prévu 0, aucune anomalie « jour non pointé » |
| 6.10 | Jour travaillé passé sans aucun pointage | Anomalie « jour non pointé » |
| 6.11 | Journée passée qui finit sans sortie | Anomalie « sortie manquante » |
| 6.12 | Pointer une sortie sans entrée, ou deux entrées de suite | Anomalie « séquence invalide » |
| 6.13 | Pointage du jour même, journée non terminée | Pas d'anomalie « sortie manquante » ni « jour non pointé » |
| 6.14 | Recalculer plusieurs fois la même journée | Les anomalies ne sont pas dupliquées |
| 6.15 | Enregistrer deux pointages identiques (même salarié, instant et type) | Refus |
| 6.16 | Affecter à un salarié deux plannings qui se chevauchent | Refus |
| 6.17 | Créer un horaire avec fin avant début, ou pause hors de la journée | Refus |
| 6.18 | Ouvrir le récapitulatif mensuel | Totaux prévus, travaillés, retard, nombre de retards, heures sup validées et à valider cohérents avec les journées |
| 6.19 | Refuser des heures sup | Statut « refusée », elles ne comptent pas dans les heures sup validées |
| 6.20 | Valider des heures sup déjà traitées | Refus |

## Bloc 7 : congés et absences

### Fonctionnalités

- [ ] **Types de congé** : annuel, maladie, exceptionnel, sans solde, avec leurs règles (déduit du solde, justificatif, remplaçant obligatoire).
- [ ] **Droits et acquisition** : 2,5 jours par mois, plafond de 90 jours.
- [ ] **Solde** : acquis, pris, ajustement, disponible, avec l'historique des mouvements.
- [ ] **Demande de congé** : formulaire avec nombre de jours calculé automatiquement.
- [ ] **Contrôle du solde** : refus automatique si les jours demandés dépassent le solde restant.
- [ ] **Circuit de validation** : manager puis RH, avec commentaire et refus possible à chaque étape.
- [ ] **Annulation** : par le salarié ou le RH, avec restitution des jours.
- [ ] **Remplaçants** : désignation, responsabilités, acceptation ou refus.
- [ ] **Absences** : suivi par type et statut ; dépôt et vérification de justificatifs.
- [ ] **Missions** : création et suivi, sans impact sur le solde.
- [ ] **Planning des départs** : calendrier des congés, absences et missions avec le remplaçant.
- [ ] **Indemnité de congé non pris** : calcul au départ et historique.

### Vérifications
| # | Scénario | Résultat attendu |
|---|---|---|
| 7.1 | Lancer l'acquisition mensuelle pour un salarié actif | +2,5 jours sur son solde ; mouvement « acquisition » visible |
| 7.2 | Relancer l'acquisition du même mois | Aucun jour ajouté |
| 7.3 | Solde à 89 jours, lancer l'acquisition | +1 jour seulement (plafond 90 respecté) |
| 7.4 | Solde à 90 jours, lancer l'acquisition | Aucun ajout |
| 7.5 | Salarié parti | N'acquiert plus rien après sa date de départ |
| 7.6 | Demander du lundi au vendredi sans férié | 5 jours décomptés |
| 7.7 | Demander une période qui contient un week-end ou un jour férié | Ces jours ne sont pas décomptés |
| 7.8 | Demander une période sans aucun jour travaillé | Refus |
| 7.9 | Solde de 5 jours, demander 6 jours | Refus avec message « Solde insuffisant » |
| 7.10 | Solde de 5 jours, une demande en attente de 3 jours, demander 3 jours de plus | Refus (les jours en attente sont réservés) |
| 7.11 | Deux demandes qui se chevauchent pour le même salarié | Refus de la seconde |
| 7.12 | Créer une demande | Les deux étapes (manager, RH) sont créées en attente |
| 7.13 | Valider par le manager | La demande passe à « en attente RH » |
| 7.14 | Refuser à l'étape manager, puis à l'étape RH (sur deux demandes) | La demande est refusée dans les deux cas, le solde n'est pas touché |
| 7.15 | Un manager valide sa propre demande | Refus |
| 7.16 | Un utilisateur sans lien hiérarchique valide à l'étape manager | Refus |
| 7.17 | Un non-RH valide à l'étape RH | Refus |
| 7.18 | Valider une étape déjà traitée | Refus |
| 7.19 | Valider côté RH un congé dont le type exige un remplaçant, sans remplaçant accepté | Refus |
| 7.20 | Valider côté RH avec un remplaçant ayant accepté | Demande acceptée, solde débité, absence créée, mouvement « prise » enregistré |
| 7.21 | Désigner comme remplaçant le demandeur lui-même | Refus |
| 7.22 | Désigner un remplaçant inactif ou lui-même absent sur la période | Refus |
| 7.23 | Solde réparti sur deux années | Les jours sont pris d'abord sur l'année la plus ancienne |
| 7.24 | Valider un congé maladie, exceptionnel ou sans solde | Aucun jour déduit du solde |
| 7.25 | Annuler un congé accepté non commencé | Jours restitués (mouvement « annulation »), absence annulée |
| 7.26 | Annuler un congé déjà commencé | Refus |
| 7.27 | Annuler une demande déjà refusée ou annulée | Refus |
| 7.28 | Annulation par un tiers qui n'est ni le salarié ni le RH | Refus |
| 7.29 | Créer une mission pendant un congé ou une absence | Refus |
| 7.30 | Créer deux missions actives qui se chevauchent | Refus |
| 7.31 | Faire une mission de plusieurs jours | Le solde de congés ne bouge pas |
| 7.32 | Créer une absence de type « congé » sans demande liée | Refus |
| 7.33 | Créer deux absences qui se chevauchent | Refus |
| 7.34 | Déposer un justificatif sur une absence | Statut « à vérifier », puis validé ou refusé |
| 7.35 | Ouvrir le planning des départs | Congés acceptés, absences et missions affichés avec le remplaçant |
| 7.36 | Calculer l'indemnité d'un salarié parti : salaire 3 000 000, 12 jours non pris | Montant 1 200 000 (3 000 000 × 12 / 30) |
| 7.37 | Calculer l'indemnité d'un salarié sans contrat avec salaire | Refus avec message |
| 7.38 | Enregistrer l'indemnité, puis ouvrir l'historique | Une ligne avec le contrat, le salaire, les jours et le montant utilisés |
| 7.39 | Salarié avec des congés maladie ou sans solde | Seuls les jours des congés qui déduisent du solde comptent dans l'indemnité |

## Validation de bout en bout

Un parcours complet à jouer une fois tous les blocs prêts :

1. [ ] Embaucher un salarié : son affectation, son droit au congé annuel (2,5 jours/mois) et son planning standard sont créés.
2. [ ] Lui donner un supérieur : il apparaît dans l'organigramme et chez son supérieur.
3. [ ] Lancer l'acquisition mensuelle : son solde augmente.
4. [ ] Pointer sur plusieurs jours avec un retard et des heures sup : valider les heures sup, puis vérifier le récapitulatif mensuel.
5. [ ] Déposer un document qui expire bientôt : l'alerte arrive à la bonne personne.
6. [ ] Désigner un remplaçant qui accepte, puis faire une demande de congé : valider côté manager puis côté RH.
7. [ ] Vérifier le solde débité, l'absence dans le planning des départs, et l'absence de pointage anormal ces jours-là.
8. [ ] Muter le salarié : la frise de carrière conserve l'ancienne affectation.
9. [ ] Marquer le salarié comme parti, calculer son indemnité et vérifier le montant à la main.
10. [ ] Consulter le journal d'audit : chaque étape précédente y figure avec son auteur.
