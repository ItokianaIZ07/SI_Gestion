const sidebarByRole = {
    rh: [
        { label: "Entretiens", to: "/si/app/rh/entretiens" },
        { label: "Contrats", to: "/si/app/rh/contrats" },
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
        { label: "Créer une demande", to: "/si/app/manager/demandes/creation" }
    ],

    finance: [
        { label: "Dépenses", to: "/si/app/finance/depenses" },
        { label: "Budget", to: "/si/app/finance/budget" },
        { label: "Demandes", to: "/si/app/finance/demandes" }
    ],

    direction: [
        // Tous les liens du système
        { label: "Contrats", to: "/si/app/rh/contrats" },
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
        { label: "Demandes Finance", to: "/si/app/finance/demandes" }
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
