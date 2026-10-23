import { ref } from "vue";

export function useExperiences() {
  const experiences = ref([
    {
      poste: "Data & Business Analyst",
      entreprise: "FREELANCE - Paris",
      debut: 2015,
      fin: 2017,
      mission:
        "Exploration des données et visualisation\nConception de reporting opérationnel et décisionnel\nOptimisation des processus de collecte des données",
    },
    {
      poste: "Formateur Business Intelligence",
      entreprise: "FREELANCE - Paris",
      debut: 2016,
      fin: 2017,
      mission:
        "Import et transformation des données ETL Power Query\nArchitecture et modélisation des tables\nFonctionnalités des outils de visualisation",
    },
    {
      poste: "Contrôleur de Gestion",
      entreprise: "ENTREPRISE - Paris",
      debut: 2009,
      fin: 2015,
      mission:
        "Analyse de la performance commerciale et logistique\nSuivi de la rentabilité de chaque Business Unit\nCréation et mise à jour de tableaux de bord",
    },
  ]);

  function ajouterExperience() {
    experiences.value.push({
      poste: "",
      entreprise: "",
      debut: "",
      fin: "",
      mission: "",
    });
  }

  function supprimerExperience(index) {
    experiences.value.splice(index, 1);
  }

  function dureeExperience(exp) {
    const debut = Number(exp.debut);
    const fin = Number(exp.fin);
    if (!debut || !fin || fin < debut) return "";
    const duree = fin - debut;
    return duree === 1 ? "1 an" : `${duree} ans`;
  }

  function totalAnneesTravail() {
    const periodes = experiences.value
      .filter(
        (exp) => exp.debut && exp.fin && Number(exp.fin) >= Number(exp.debut),
      )
      .map((exp) => ({
        debut: Number(exp.debut),
        fin: Number(exp.fin),
      }))
      .sort((a, b) => a.debut - b.debut);

    if (periodes.length === 0) return 0;

    let debut = periodes[0].debut;
    let fin = periodes[0].fin;
    let total = 0;

    for (let i = 1; i < periodes.length; i++) {
      const periode = periodes[i];

      if (periode.debut <= fin) {
        // Les périodes se chevauchent : fusionner.
        fin = Math.max(fin, periode.fin);
      } else {
        // Additionner la période terminée.
        total += fin - debut;
        debut = periode.debut;
        fin = periode.fin;
      }
    }

    total += fin - debut;

    return total;
  }

  function experiencesEnTexte() {
    return experiences.value
      .map((exp) => {
        const duree = Number(exp.fin) - Number(exp.debut);

        return `${exp.poste} à ${exp.entreprise} en tant que ${exp.mission.replace(/\n/g, ", ")} du ${exp.debut} au ${exp.fin} : ${duree} ans`;
      })
      .join("\n");
  }

  return {
    experiences,
    ajouterExperience,
    supprimerExperience,
    dureeExperience,
    experiencesEnTexte,
    totalAnneesTravail
  };
}
