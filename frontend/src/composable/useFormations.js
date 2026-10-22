import { ref } from 'vue'

export function useFormations() {
  const formations = ref([
    {
      ecole: 'ESSEC Business School - Paris',
      diplome: 'Master In Business Intelligence',
      debut: 2008,
      fin: 2011
    },
    {
      ecole: 'Université de Strasbourg - Strasbourg',
      diplome: 'Licence 2 de Science juridique',
      debut: 2005,
      fin: 2007
    }
  ])

  function ajouterFormation() {
    formations.value.push({
      ecole: '',
      diplome: '',
      debut: '',
      fin: ''
    })
  }

  function supprimerFormation(index) {
    formations.value.splice(index, 1)
  }

  return {
    formations,
    ajouterFormation,
    supprimerFormation
  }
}