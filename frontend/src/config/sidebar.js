const sidebarByRole = {
    rh: [
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
        {
            label: "Jours fériés",
            children: [
                { label: "Liste des jours fériés", to: "/si/app/rh/jours-feries" },
            ],
        },
        { label: "Liste des demandes", to: "/si/app/rh/demandes" },
        { label: "Recrutements", to: "/si/app/rh/recrutements" },
        { label: "Offres d'emploi", to: "/si/app/rh/offres" },
        { label: "Publications", to: "/si/app/rh/publications" },
        { label: "CV des candidats", to: "/si/app/rh/cv" }
    ],

    manager: [
        { label: "Liste des départements", to: "/si/app/manager/departements" },
        { label: "Créer un département", to: "/si/app/manager/departements/creation" },
        { label: "Liste des demandes", to: "/si/app/manager/demandes" },
        { label: "Créer une demande", to: "/si/app/manager/demandes/creation" },
        { label: "Notifications", to: "/si/app/manager/notifications" }
    ],

    finance: [
        { label: "Dépenses", to: "/si/app/finance/depenses" },
        { label: "Budget", to: "/si/app/finance/budget" },
        { label: "Demandes", to: "/si/app/finance/demandes" },
        { label: "Grille salariale", to: "/si/app/finance/grille-salaire" },
        { label: "Notifications", to: "/si/app/finance/notifications" }
    ],

    direction: [
        // Tous les liens du système
        { label: "Contrats", to: "/si/app/rh/contrats" },
        { label: "Onboarding", to: "/si/app/rh/onboarding" },
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
        {
            label: "Jours fériés",
            children: [
                { label: "Liste des jours fériés", to: "/si/app/rh/jours-feries" },
            ],
        },
        { label: "Liste des demandes RH", to: "/si/app/rh/demandes" },
        { label: "Recrutements", to: "/si/app/rh/recrutements" },
        { label: "Offres d'emploi", to: "/si/app/rh/offres" },
        { label: "Publications", to: "/si/app/rh/publications" },
        { label: "CV des candidats", to: "/si/app/rh/cv" },

        { label: "Liste des départements", to: "/si/app/manager/departements" },
        { label: "Créer un département", to: "/si/app/manager/departements/creation" },
        { label: "Liste des demandes Manager", to: "/si/app/manager/demandes" },
        { label: "Créer une demande", to: "/si/app/manager/demandes/creation" },

        { label: "Dépenses", to: "/si/app/finance/depenses" },
        { label: "Budget", to: "/si/app/finance/budget" },
        { label: "Demandes Finance", to: "/si/app/finance/demandes" },
        { label: "Grille salariale", to: "/si/app/finance/grille-salaire" },
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
