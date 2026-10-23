<script setup>
import { computed } from 'vue'

const props = defineProps({
  departement: { type: Object, required: true },
})

const effectifActif = computed(() => props.departement.effectifActif ?? 0)
const effectifTotal = computed(
  () =>
    effectifActif.value +
    (props.departement.effectifSuspendu ?? 0) +
    (props.departement.effectifParti ?? 0),
)
const effectifMax = computed(() => props.departement.effectifMax ?? 0)
const tauxOccupation = computed(() => {
  if (effectifMax.value <= 0) return 0
  return Math.min(100, Math.round((effectifActif.value / effectifMax.value) * 100))
})

const libellesStatuts = {
  EN_ATTENTE_RH: 'En attente RH',
  EN_ATTENTE_FINANCE: 'En attente Finance',
  EN_ATTENTE_DIRECTION: 'En attente Direction',
  ACCEPTE: 'Acceptée',
  REFUSE: 'Refusée',
  ANNULE: 'Annulée',
}

function libelleStatut(statut) {
  return libellesStatuts[statut] ?? statut
}

function classeStatut(statut) {
  if (statut === 'ACCEPTE') return 'statut--accepte'
  if (statut === 'REFUSE' || statut === 'ANNULE') return 'statut--ferme'
  return 'statut--attente'
}

function formaterDate(date) {
  if (!date) return '—'
  return new Intl.DateTimeFormat('fr-FR', { dateStyle: 'medium' }).format(new Date(date))
}
</script>

<template>
  <section class="vision">
    <div class="indicateurs">
      <article class="indicateur">
        <span class="indicateur__libelle">Effectif actif</span>
        <strong class="indicateur__valeur">{{ departement.effectifActif }}</strong>
        <span class="indicateur__detail">sur {{ departement.effectifMax }} places autorisées</span>
        <div
          class="progression"
          role="progressbar"
          :aria-valuenow="tauxOccupation"
          aria-valuemin="0"
          aria-valuemax="100"
          :aria-label="`Occupation du département : ${tauxOccupation}%`"
        >
          <span :style="{ width: `${tauxOccupation}%` }"></span>
        </div>
        <span class="indicateur__detail">{{ tauxOccupation }} % de capacité occupée</span>
      </article>
      <article class="indicateur">
        <span class="indicateur__libelle">Effectif total</span>
        <strong class="indicateur__valeur">{{ effectifTotal }}</strong>
        <span class="indicateur__detail">
          {{ departement.effectifActif }} actif(s) · {{ departement.effectifSuspendu }} suspendu(s) ·
          {{ departement.effectifParti }} parti(s)
        </span>
      </article>
      <article class="indicateur">
        <span class="indicateur__libelle">Postes</span>
        <strong class="indicateur__valeur">{{ departement.postesActifs }}</strong>
        <span class="indicateur__detail">{{ departement.postesInactifs }} inactif(s)</span>
      </article>
      <article class="indicateur">
        <span class="indicateur__libelle">Demandes</span>
        <strong class="indicateur__valeur">{{ departement.demandes.length }}</strong>
        <span class="indicateur__detail">tous états confondus</span>
      </article>
    </div>

    <section class="bloc">
      <div class="bloc__entete">
        <div>
          <h2>État des demandes</h2>
          <p>Répartition des demandes de recrutement de ce département.</p>
        </div>
      </div>
      <div class="etats">
        <article
          v-for="(nombre, statut) in departement.demandesParStatut"
          :key="statut"
          class="etat"
        >
          <span>{{ libelleStatut(statut) }}</span>
          <strong>{{ nombre }}</strong>
        </article>
      </div>
    </section>

    <section class="bloc">
      <div class="bloc__entete">
        <div>
          <h2>Demandes récentes</h2>
          <p>Suivi des demandes liées aux postes du département.</p>
        </div>
        <span class="compte">{{ departement.demandes.length }} demande(s)</span>
      </div>

      <div v-if="departement.demandes.length" class="table-conteneur">
        <table>
          <thead>
            <tr>
              <th>Demande</th>
              <th>Poste</th>
              <th>Effectif demandé</th>
              <th>État</th>
              <th>Date</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="demande in departement.demandes.slice(0, 10)" :key="demande.id">
              <td>#{{ demande.id }}</td>
              <td>{{ demande.poste }}</td>
              <td>{{ demande.effectif }}</td>
              <td>
                <span class="statut" :class="classeStatut(demande.statut)">
                  {{ libelleStatut(demande.statut) }}
                </span>
              </td>
              <td>{{ formaterDate(demande.dateDemande) }}</td>
            </tr>
          </tbody>
        </table>
      </div>
      <p v-else class="vide">Aucune demande n’a encore été enregistrée pour ce département.</p>
    </section>
  </section>
</template>

<style scoped>
.vision {
  display: grid;
  gap: 24px;
  color: #1f2937;
}

.indicateurs {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

.indicateur,
.bloc {
  background: var(--color-card, #fff);
  border: 1px solid #e5e7eb;
  border-radius: 14px;
  box-shadow: 0 3px 12px rgba(15, 23, 42, 0.04);
}

.indicateur {
  display: flex;
  min-height: 130px;
  flex-direction: column;
  gap: 10px;
  padding: 20px;
}

.indicateur__libelle,
.indicateur__detail {
  color: #64748b;
  font-size: 0.88rem;
}

.indicateur__valeur {
  color: var(--color-primary, #015f09);
  font-size: 2rem;
  line-height: 1;
}

.progression {
  height: 8px;
  overflow: hidden;
  border-radius: 8px;
  background: #e8f3e9;
}

.progression span {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: var(--color-primary, #015f09);
  transition: width 0.2s ease;
}

.bloc {
  padding: 22px;
}

.bloc__entete {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 18px;
}

h2 {
  margin: 0;
  color: #1f2937;
  font-size: 1.15rem;
}

.bloc__entete p {
  margin: 6px 0 0;
  color: #64748b;
  font-size: 0.9rem;
}

.etats {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.etat {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 14px 16px;
  border-radius: 10px;
  background: #f8fafc;
  color: #475569;
}

.etat strong {
  color: #1f2937;
  font-size: 1.1rem;
}

.compte {
  color: #64748b;
  font-size: 0.88rem;
  white-space: nowrap;
}

.table-conteneur {
  overflow-x: auto;
}

table {
  width: 100%;
  border-collapse: collapse;
  text-align: left;
}

th,
td {
  padding: 13px 12px;
  border-bottom: 1px solid #e5e7eb;
  white-space: nowrap;
}

th {
  color: #64748b;
  font-size: 0.8rem;
  font-weight: 600;
  text-transform: uppercase;
}

td {
  color: #334155;
  font-size: 0.92rem;
}

.statut {
  display: inline-flex;
  padding: 5px 9px;
  border-radius: 999px;
  font-size: 0.8rem;
  font-weight: 600;
}

.statut--attente {
  background: #fff7dd;
  color: #8a5b00;
}

.statut--accepte {
  background: #e5f6e8;
  color: #176c22;
}

.statut--ferme {
  background: #f1f5f9;
  color: #475569;
}

.vide {
  margin: 0;
  padding: 18px 0 4px;
  color: #64748b;
}

@media (max-width: 1000px) {
  .indicateurs {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 620px) {
  .indicateurs,
  .etats {
    grid-template-columns: 1fr;
  }

  .bloc {
    padding: 16px;
  }

  .bloc__entete {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
