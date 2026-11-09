const sidebarByRole = {
    rh: [
        { label: "Employés", to: "/si/app/rh/employes" },
        { label: "Documents à renouveler", to: "/si/app/rh/documents" },
        { label: "Entretiens", to: "/si/app/rh/entretiens" },
        {
            label: "Contrats",
            children: [
                { label: "Embaucher", to: "/si/app/rh/embauche" },
                { label: "Liste des contrats", to: "/si/app/rh/contrats" },
            ],
        },
        {
            label: "Onboarding",
            children: [
                { label: "Nouveaux employés", to: "/si/app/rh/onboarding" },
                { label: "Modèles de tâches", to: "/si/app/rh/onboarding/modeles" },
                { label: "Mes tâches", to: "/si/app/taches-onboarding" },
            ],
        },
        {
            label: "Régimes de travail",
            children: [
                { label: "Liste des régimes", to: "/si/app/rh/regimes-travail" },
                { label: "Nouveau régime", to: "/si/app/rh/regimes-travail/formulaire" },
            ],
        },
        {
            label: "Horaires de travail",
            children: [
                { label: "Liste des horaires", to: "/si/app/rh/horaires-travail" },
                { label: "Nouvel horaire", to: "/si/app/rh/horaires-travail/formulaire" },
            ],
        },
        { label: "Jours fériés", to: "/si/app/rh/jours-feries" },
        {
            label: "Planning",
            children: [
                { label: "Liste des plannings", to: "/si/app/rh/plannings" },
                { label: "Nouvelle affectation", to: "/si/app/rh/plannings/nouvelle-affectation" },
            ],
        },
        { label: "Variables de paie", to: "/si/app/rh/variables-paie" },
        { label: "Liste des demandes", to: "/si/app/rh/demandes" },
        { label: "Recrutements", to: "/si/app/rh/recrutements" },
        { label: "Organigramme", to: "/si/app/rh/organigramme" },
        { label: "Offres d'emploi", to: "/si/app/rh/offres" },
        { label: "Publications", to: "/si/app/rh/publications" },
        { label: "CV des candidats", to: "/si/app/rh/cv" },

        {
        label: "Congés",
        children: [
            { label: "Tableau de bord", to: "/si/app/rh/conges" },
            { label: "Demandes de congé", to: "/si/app/rh/conges/demandes" },
            { label: "Nouvelle demande", to: "/si/app/rh/conges/nouvelle" },
          ],
        },

        { label: "Congés à valider", to: "/si/app/conges/validations" },
        { label: "Tableau de bord congés", to: "/si/app/rh/conges" },
        { label: "Notifications", to: "/si/app/rh/notifications" },
        { label: "Pointage", to: "/si/app/rh/pointage" },
        { label: "Heures Sup", to: "/si/app/rh/heures-sup" },
        { label: "Anomalies", to: "/si/app/rh/anomalies" },
        { label: "Récap Mensuel", to: "/si/app/rh/recap-mensuel" }
    ],

    manager: [
        { label: "Liste des départements", to: "/si/app/manager/departements" },
        { label: "Créer un département", to: "/si/app/manager/departements/creation" },
        { label: "Liste des demandes", to: "/si/app/manager/demandes" },
        { label: "Créer une demande", to: "/si/app/manager/demandes/creation" },
        { label: "Notifications", to: "/si/app/manager/notifications" },
        { label: "Congés à valider", to: "/si/app/conges/validations" },
        { label: "Mes tâches d'onboarding", to: "/si/app/taches-onboarding" },
        { label: "Organigramme", to: "/si/app/rh/organigramme" },
        { label: "Notifications", to: "/si/app/manager/notifications" },
        { label: "Notifications", to: "/si/app/manager/notifications" },
        { label: "Heures Sup", to: "/si/app/rh/heures-sup" },
        { label: "Anomalies", to: "/si/app/rh/anomalies" },
        { label: "Jours fériés", to: "/si/app/rh/jours-feries" },
        { label: "Planning", to: "/si/app/rh/plannings" },
        { label: "Variables de paie", to: "/si/app/rh/variables-paie" },
    ],

    finance: [
        { label: "Dépenses", to: "/si/app/finance/depenses" },
        { label: "Budget", to: "/si/app/finance/budget" },
        { label: "Demandes", to: "/si/app/finance/demandes" },
        { label: "Grille salariale", to: "/si/app/finance/grille-salaire" },
        { label: "Mes tâches d'onboarding", to: "/si/app/taches-onboarding" },
        { label: "Variables de paie", to: "/si/app/rh/variables-paie" },
        { label: "Notifications", to: "/si/app/finance/notifications" }
    ],

    direction: [
        // Tous les liens du système
        { label: "Employés", to: "/si/app/rh/employes" },
        { label: "Documents à renouveler", to: "/si/app/rh/documents" },
        { label: "Contrats", to: "/si/app/rh/contrats" },
        { label: "Onboarding", to: "/si/app/rh/onboarding" },
        { label: "Mes tâches d'onboarding", to: "/si/app/taches-onboarding" },
        { label: "Liste des demandes RH", to: "/si/app/rh/demandes" },
        { label: "Recrutements", to: "/si/app/rh/recrutements" },
        { label: "Organigramme", to: "/si/app/rh/organigramme" },
        { label: "Offres d'emploi", to: "/si/app/rh/offres" },
        { label: "Publications", to: "/si/app/rh/publications" },
        { label: "CV des candidats", to: "/si/app/rh/cv" },
        { label: "Régimes de travail", to: "/si/app/rh/regimes-travail" },
        { label: "Horaires de travail", to: "/si/app/rh/horaires-travail" },
        { label: "Jours fériés", to: "/si/app/rh/jours-feries" },
        { label: "Planning", to: "/si/app/rh/plannings" },
        { label: "Variables de paie", to: "/si/app/rh/variables-paie" },

        { label: "Liste des départements", to: "/si/app/manager/departements" },
        { label: "Créer un département", to: "/si/app/manager/departements/creation" },
        { label: "Liste des demandes Manager", to: "/si/app/manager/demandes" },
        { label: "Créer une demande", to: "/si/app/manager/demandes/creation" },

        { label: "Dépenses", to: "/si/app/finance/depenses" },
        { label: "Budget", to: "/si/app/finance/budget" },
        { label: "Demandes Finance", to: "/si/app/finance/demandes" },
        { label: "Grille salariale", to: "/si/app/finance/grille-salaire" },
        { label: "Tableau de bord congés", to: "/si/app/rh/conges" },
        { label: "Journal d'audit", to: "/si/app/direction/audit" },
        { label: "Notifications", to: "/si/app/direction/notifications" }
    ]
};

/**
 * Retourne les liens du sidebar autorisés pour un rôle.
 * @param {string} role - Rôle de l'utilisateur connecté
 * @returns {{ label: string, to: string }[]}
 */

export function getSidebarLinks(role) {
    return sidebarByRole[role?.toLowerCase()] ?? [];
}
