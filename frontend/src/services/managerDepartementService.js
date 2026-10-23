import api from '@/services/api.js'

export async function getVisionDepartements() {
  const { data } = await api.get('/manager/departements/vision-globale')
  return data
}

export async function getVisionDepartement(departementId) {
  const { data } = await api.get(`/manager/departements/vision-globale/${departementId}`)
  return data
}
