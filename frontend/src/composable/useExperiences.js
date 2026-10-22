import { ref } from 'vue'

export function useExperiences() {
  const experiences = ref([
    {
      poste: 'Data & Business Analyst',
      entreprise: 'FREELANCE - Paris',
      debut: 2015,
      fin: 2017,
      mission: 'Exploration des données et visualisation\nConception de reporting opérationnel et décisionnel\nOptimisation des processus de collecte des données'
    },
    {
      poste: 'Formateur Business Intelligence',
      entreprise: 'FREELANCE - Paris',
      debut: 2016,
      fin: 2017,
      mission: 'Import et transformation des données ETL Power Query\nArchitecture et modélisation des tables\nFonctionnalités des outils de visualisation'
    },
    {
      poste: 'Contrôleur de Gestion',
      entreprise: 'ENTREPRISE - Paris',
      debut: 2009,
      fin: 2015,
      mission: 'Analyse de la performance commerciale et logistique\nSuivi de la rentabilité de chaque Business Unit\nCréation et mise à jour de tableaux de bord'
    }
  ])

  function ajouterExperience() {
    experiences.value.push({
      poste: '',
      entreprise: '',
      debut: '',
      fin: '',
      mission: ''
    })
  }

  function supprimerExperience(index) {
    experiences.value.splice(index, 1)
  }

  
  function dureeExperience(exp) {
    const debut = Number(exp.debut)
    const fin = Number(exp.fin)
    if (!debut || !fin || fin < debut) return ''
    const duree = fin - debut
    return duree === 1 ? '1 an' : `${duree} ans`
  }

  return {
    experiences,
    ajouterExperience,
    supprimerExperience,
    dureeExperience
  }
}