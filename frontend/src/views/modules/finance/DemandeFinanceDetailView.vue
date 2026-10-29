<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import '@/assets/finance.css'
import { financeApi } from '@/api/financeApi'
import { ariary, dateFr, messageErreur } from '@/utils/format'

const route = useRoute()
const router = useRouter()

const d = ref(null)            // détail renvoyé par GET /api/finance/demandes/{id}
const chargement = ref(true)
const erreur = ref('')
const remarque = ref('')
const erreurDecision = ref('')
const envoi = ref(false)

// ----- Libellés -----
const STATUTS = {
  EN_ATTENTE_RH: ['En attente RH', 'warning'],
  EN_ATTENTE_FINANCE: ['En attente Finance', 'warning'],
  EN_ATTENTE_DIRECTION: ['En attente Direction', 'warning'],
  ACCEPTE: ['Accepté', 'success'],
  REFUSE: ['Refusé', 'danger'],
  ANNULE: ['Annulé', 'neutral']
}
const PRIORITES = { URGENTE: ['Urgente', 'danger'], HAUTE: ['Haute', 'warning'], NORMALE: ['Normale', 'neutral'], BASSE: ['Basse', 'neutral'] }
const ETATS = {
  ACCEPTE: { classe: 'ok', symbole: '✓', texte: 'Accepté' },
  REFUSE: { classe: 'ko', symbole: '✕', texte: 'Refusé' },
  EN_ATTENTE: { classe: 'wait', symbole: '…', texte: 'En attente' },
  BLOQUE: { classe: 'none', symbole: '–', texte: 'Pas encore atteinte' }
}

const statut = computed(() => STATUTS[d.value?.statut] ?? [d.value?.statut, 'neutral'])
const priorite = computed(() => PRIORITES[d.value?.priorite] ?? [d.value?.priorite, 'neutral'])
const salaireDefini = computed(() => d.value?.salaireEstime != null)
const budgetOk = computed(() => d.value?.budget.couvert)
const etatFinance = computed(() => d.value?.etapes.find(e => e.code === 'FINANCE')?.etat)

// ----- Barre du budget : salaires | dépenses | cette demande -----
const barre = computed(() => {
  if (!d.value) return { masse: 0, dep: 0, dem: 0 }
  const b = d.value.budget
  const pct = (x) => (b.budget > 0 ? Math.max(0, (x / b.budget) * 100) : 0)
  const masse = pct(b.masseSalariale)
  const dep = pct(b.depenses)
  const dem = salaireDefini.value ? Math.min(pct(d.value.coutTotal), Math.max(0, 100 - masse - dep)) : 0
  return { masse, dep, dem }
})

// ----- Barre de la grille : fourchette + position du salaire estimé -----
const grilleBarre = computed(() => {
  const g = d.value?.grille
  if (!g?.definie) return null
  const etendue = (g.max - g.min) || g.min * 0.2 || 1
  const bas = g.min - etendue * 0.3
  const haut = g.max + etendue * 0.3
  const sur100 = (x) => Math.min(100, Math.max(0, ((x - bas) / (haut - bas)) * 100))
  return {
    gauche: sur100(g.min),
    droite: 100 - sur100(g.max),
    position: salaireDefini.value ? sur100(d.value.salaireEstime) : null
  }
})

// ----- Chargement -----
async function charger() {
  d.value = await financeApi.getDemande(route.params.id)
}

onMounted(async () => {
  try { await charger() }
  catch (e) { erreur.value = messageErreur(e) }
  finally { chargement.value = false }
})

const retour = () => router.push('/si/app/finance/demandes')

// ----- Décision -----
async function decider(valider) {
  erreurDecision.value = ''
  const texte = remarque.value.trim()
  if (!valider && !texte) {
    erreurDecision.value = 'La remarque est obligatoire pour refuser une demande.'
    return
  }
  envoi.value = true
  try {
    if (valider) await financeApi.valider(d.value.id, texte || null)
    else await financeApi.refuser(d.value.id, texte)
    router.push({ path: '/si/app/finance/demandes', query: { resultat: valider ? 'validee' : 'refusee' } })
  } catch (e) {
    erreurDecision.value = messageErreur(e)   // le backend revérifie workflow, rôle, grille et budget
    try { await charger() } catch { /* on garde l'affichage actuel */ }
  } finally {
    envoi.value = false
  }
}
</script>

<template>
  <section class="fin-page fd">
    <button type="button" class="fd-retour" @click="retour">← Retour aux demandes</button>

    <p v-if="chargement">Chargement…</p>
    <p v-else-if="erreur" class="fin-error" role="alert">{{ erreur }}</p>

    <template v-else-if="d">
      <!-- ===== En-tête ===== -->
      <header class="fd-entete">
        <div>
          <span class="fd-eyebrow">DEMANDE #{{ d.id }}</span>
          <h1>{{ d.poste }}</h1>
          <div class="fd-dep">{{ d.departement }}</div>
        </div>
        <div class="fd-badges">
          <span class="fd-badge" :class="'fd-b-' + priorite[1]">Priorité {{ priorite[0].toLowerCase() }}</span>
          <span class="fd-badge" :class="'fd-b-' + statut[1]">{{ statut[0] }}</span>
        </div>
      </header>

      <div class="fd-grid">
        <!-- ===== Colonne de gauche : la demande ===== -->
        <div class="fd-col">
          <div class="fd-card">
            <h2>Parcours de validation</h2>
            <div class="fd-parcours">
              <div v-for="e in d.etapes" :key="e.code" class="fd-etape"
                   :class="{ 'fd-cur': e.code === 'FINANCE' && e.etat === 'EN_ATTENTE' && d.peutRefuser }">
                <div class="fd-t">
                  <span class="fd-ico" :class="'fd-i-' + ETATS[e.etat].classe">{{ ETATS[e.etat].symbole }}</span>
                  {{ e.libelle }}
                </div>
                <div class="fd-s">
                  {{ ETATS[e.etat].texte }}
                  <template v-if="e.code === 'FINANCE' && e.etat === 'EN_ATTENTE' && d.peutRefuser"> · <strong>à votre décision</strong></template>
                </div>
                <div v-if="e.validateur" class="fd-m">
                  {{ e.validateur }} · {{ dateFr(e.dateDecision) }}
                  <template v-if="e.remarque"><br>« {{ e.remarque }} »</template>
                </div>
              </div>
            </div>
          </div>

          <div class="fd-card">
            <h2>Informations générales</h2>
            <div class="fd-champs">
              <div class="fd-champ"><span>Demandeur</span><strong>{{ d.demandeur }}</strong></div>
              <div class="fd-champ"><span>Type de besoin</span><strong>{{ d.typeBesoin ?? '—' }}</strong></div>
              <div class="fd-champ"><span>Nombre de postes</span><strong>{{ d.effectif }}</strong></div>
              <div class="fd-champ"><span>Type de contrat souhaité</span><strong>{{ d.typeContrat ?? '—' }}</strong></div>
              <div class="fd-champ"><span>Lieu de travail</span><strong>{{ d.lieuTravail ?? '—' }}</strong></div>
              <div class="fd-champ"><span>Date de la demande</span><strong>{{ dateFr(d.dateDemande) }}</strong></div>
              <div class="fd-champ"><span>Arrivée souhaitée</span><strong>{{ dateFr(d.dateArrivee) }}</strong></div>
            </div>
          </div>

          <div class="fd-card">
            <h2>Motif de la demande</h2>
            <p class="fd-motif">{{ d.motif }}</p>
          </div>

          <div class="fd-card">
            <h2>Profil recherché</h2>
            <div class="fd-champs">
              <div class="fd-champ"><span>Expérience</span><strong>{{ d.experience ?? '—' }}</strong></div>
              <div class="fd-champ"><span>Niveau d’étude</span><strong>{{ d.niveauEtude ?? '—' }}</strong></div>
              <div class="fd-champ fd-large"><span>Compétences requises</span><strong>{{ d.competences ?? '—' }}</strong></div>
            </div>
          </div>
        </div>

        <!-- ===== Colonne de droite : analyse et décision ===== -->
        <aside class="fd-col">
          <div class="fd-card">
            <h2>Analyse budgétaire</h2>

            <div class="fd-cout" :class="{ 'fd-ko': salaireDefini && !budgetOk }">
              <template v-if="salaireDefini">
                <div class="fd-formule">{{ ariary(d.salaireEstime) }} × {{ d.effectif }} poste{{ d.effectif > 1 ? 's' : '' }} × {{ d.nbMois }} mois</div>
                <div class="fd-total">{{ ariary(d.coutTotal) }}</div>
                <span class="fd-badge" :class="budgetOk ? 'fd-b-success' : 'fd-b-danger'">
                  {{ budgetOk ? 'Couvert par le budget' : 'Dépasse le budget' }}
                </span>
              </template>
              <template v-else>
                <div class="fd-formule">Salaire estimé non défini par les RH</div>
                <div class="fd-total">—</div>
              </template>
            </div>

            <div class="fd-lignes">
              <div class="fd-ligne"><span>Budget {{ d.departement }} {{ d.budget.annee }}</span><strong>{{ ariary(d.budget.budget) }}</strong></div>
              <div class="fd-ligne"><span>Masse salariale actuelle</span><strong>{{ ariary(d.budget.masseSalariale) }}</strong></div>
              <div class="fd-ligne"><span>Dépenses internes</span><strong>{{ ariary(d.budget.depenses) }}</strong></div>
              <div class="fd-ligne"><span>Budget restant</span><strong>{{ ariary(d.budget.restant) }}</strong></div>
              <div v-if="salaireDefini" class="fd-ligne fd-res">
                <span>Restant après cette demande</span>
                <strong :style="{ color: d.budget.restantApres < 0 ? '#b42323' : '#1f7a2a' }">
                  {{ d.budget.restantApres < 0 ? '− ' : '' }}{{ ariary(Math.abs(d.budget.restantApres)) }}
                </strong>
              </div>
            </div>

            <div class="fd-barre">
              <i :style="{ width: barre.masse + '%', background: '#015f09' }"></i>
              <i :style="{ width: barre.dep + '%', background: '#80af84' }"></i>
              <i :style="{ width: barre.dem + '%', background: budgetOk ? '#e0b84c' : '#b42323' }"></i>
            </div>
            <div class="fd-legende">
              <span><b style="background:#015f09"></b>Salaires</span>
              <span><b style="background:#80af84"></b>Dépenses</span>
              <span><b :style="{ background: budgetOk ? '#e0b84c' : '#b42323' }"></b>Cette demande</span>
            </div>
          </div>

          <div class="fd-card">
            <h2>Grille salariale du poste</h2>

            <template v-if="grilleBarre">
              <div class="fd-grille-barre">
                <div class="fd-plage" :style="{ left: grilleBarre.gauche + '%', right: grilleBarre.droite + '%' }"></div>
                <template v-if="grilleBarre.position !== null">
                  <div class="fd-rep" :style="{ left: grilleBarre.position + '%', color: d.grille.dansGrille ? '#1f7a2a' : '#b42323' }">{{ ariary(d.salaireEstime) }}</div>
                  <div class="fd-pt" :style="{ left: grilleBarre.position + '%', background: d.grille.dansGrille ? '#1f7a2a' : '#b42323' }"></div>
                </template>
              </div>
              <div class="fd-bornes"><span>{{ ariary(d.grille.min) }}</span><span>{{ ariary(d.grille.max) }}</span></div>
              <div v-if="!salaireDefini" class="fd-notice fd-n-info">Le salaire estimé n’a pas encore été défini par les RH.</div>
              <div v-else class="fd-notice" :class="d.grille.dansGrille ? 'fd-n-ok' : 'fd-n-err'">
                {{ d.grille.dansGrille ? 'Le salaire estimé ne dépasse pas le maximum de la grille.' : 'Le salaire estimé dépasse le maximum de la grille.' }}
              </div>
            </template>
            <div v-else class="fd-notice fd-n-warn">Aucune grille salariale en vigueur pour ce poste.</div>
          </div>

          <div class="fd-card fd-decision">
            <h2>Décision Finance</h2>

            <template v-if="d.peutRefuser">
              <label for="remarque">Remarque <span class="fd-facultatif">(obligatoire pour refuser)</span></label>
              <textarea id="remarque" v-model="remarque" placeholder="Motif de la décision…" :disabled="envoi"></textarea>

              <div v-if="d.motifBlocage" class="fd-notice fd-n-warn">
                {{ d.motifBlocage }}<br>La validation est bloquée, le refus reste possible.
              </div>
              <div v-else class="fd-notice fd-n-info">Le budget couvre cette demande : vous pouvez la valider.</div>
              <div v-if="erreurDecision" class="fd-notice fd-n-err" role="alert">{{ erreurDecision }}</div>

              <div class="fd-actions">
                <button type="button" class="fin-btn fin-btn-dan" :disabled="envoi" @click="decider(false)">Refuser</button>
                <button type="button" class="fin-btn fin-btn-pri" :disabled="!d.peutValider || envoi" @click="decider(true)">Valider la demande</button>
              </div>
            </template>

            <template v-else>
              <div class="fd-notice" :class="etatFinance === 'ACCEPTE' ? 'fd-n-ok' : etatFinance === 'REFUSE' ? 'fd-n-err' : 'fd-n-info'">
                {{ d.motifBlocage }}
              </div>
              <div class="fd-actions">
                <button type="button" class="fin-btn fin-btn-dan" disabled>Refuser</button>
                <button type="button" class="fin-btn fin-btn-pri" disabled>Valider la demande</button>
              </div>
            </template>
          </div>
        </aside>
      </div>
    </template>
  </section>
</template>

<style scoped>
.fd { --fd-primary: var(--color-primary, #015f09); --fd-soft: var(--color-primary-soft, #eef6ee); --fd-line: var(--color-primary-line, #cfe3d1);
      --fd-ink: var(--color-ink, #17211a); --fd-muted: #6a766d; --fd-border: #e6ebe6; --fd-subtle: #f4f6f4; max-width: 1180px; }
.fd h1, .fd h2 { color: var(--fd-ink); margin: 0; }

.fd-retour { background: none; border: 0; color: var(--fd-primary); font: 600 14px inherit; font-family: inherit; cursor: pointer; padding: 0; margin-bottom: 14px; }
.fd-retour:hover { text-decoration: underline; }

.fd-entete { display: flex; justify-content: space-between; align-items: flex-start; gap: 16px; flex-wrap: wrap; margin-bottom: 20px; }
.fd-eyebrow { font-size: 12px; font-weight: 700; letter-spacing: .08em; color: var(--fd-primary); }
.fd-entete h1 { font-size: 1.65rem; margin: 2px 0; }
.fd-dep { color: var(--fd-muted); }
.fd-badges { display: flex; gap: 8px; flex-wrap: wrap; }

.fd-badge { display: inline-flex; align-items: center; gap: 6px; padding: 3px 10px; border-radius: 999px; font-size: 12px; font-weight: 600; white-space: nowrap; }
.fd-badge::before { content: ""; width: 6px; height: 6px; border-radius: 50%; background: currentColor; opacity: .75; }
.fd-b-success { background: #e8f4e8; color: #1f7a2a; }
.fd-b-warning { background: #fff5e0; color: #94610a; }
.fd-b-danger { background: #fdeeee; color: #b42323; }
.fd-b-neutral { background: var(--fd-subtle); color: var(--fd-muted); }

.fd-grid { display: grid; grid-template-columns: minmax(0, 1fr) 370px; gap: 20px; align-items: start; }
.fd-col { display: flex; flex-direction: column; gap: 20px; }
.fd-card { background: #fff; border: 1px solid var(--fd-border); border-radius: 12px; padding: 20px 22px; box-shadow: 0 1px 2px rgba(16, 40, 20, .05); }
.fd-card h2 { font-size: 15px; margin-bottom: 14px; }

.fd-parcours { display: grid; grid-template-columns: repeat(3, 1fr); gap: 12px; }
.fd-etape { border: 1px solid var(--fd-border); border-radius: 8px; padding: 12px; background: var(--fd-subtle); }
.fd-etape.fd-cur { border-color: var(--fd-primary); background: var(--fd-soft); }
.fd-t { font-weight: 600; color: var(--fd-ink); display: flex; align-items: center; gap: 8px; }
.fd-ico { width: 22px; height: 22px; border-radius: 50%; display: grid; place-items: center; font-size: 12px; color: #fff; font-weight: 700; }
.fd-i-ok { background: #1f7a2a; } .fd-i-ko { background: #b42323; } .fd-i-wait { background: #94610a; } .fd-i-none { background: #98a39b; }
.fd-s { font-size: 13px; margin-top: 6px; }
.fd-m { font-size: 12.5px; color: var(--fd-muted); margin-top: 4px; }

.fd-champs { display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 14px 20px; }
.fd-champ span { display: block; font-size: 12px; color: var(--fd-muted); margin-bottom: 2px; }
.fd-champ strong { color: var(--fd-ink); font-weight: 600; }
.fd-champ.fd-large { grid-column: 1 / -1; }
.fd-motif { margin: 0; line-height: 1.6; }

.fd-cout { background: var(--fd-soft); border: 1px solid var(--fd-line); border-radius: 8px; padding: 14px; margin-bottom: 16px; }
.fd-cout.fd-ko { background: #fdeeee; border-color: #f1c9c9; }
.fd-formule { font-size: 13px; color: var(--fd-muted); }
.fd-total { font-size: 1.5rem; font-weight: 700; color: var(--fd-ink); margin: 2px 0; }
.fd-lignes { display: grid; gap: 8px; margin: 14px 0; }
.fd-ligne { display: flex; justify-content: space-between; gap: 12px; font-size: 13.5px; }
.fd-ligne span { color: var(--fd-muted); }
.fd-ligne strong { color: var(--fd-ink); white-space: nowrap; }
.fd-ligne.fd-res { border-top: 1px solid var(--fd-border); padding-top: 8px; margin-top: 2px; }
.fd-barre { display: flex; height: 12px; border-radius: 99px; overflow: hidden; background: var(--fd-subtle); border: 1px solid var(--fd-border); }
.fd-barre i { display: block; height: 100%; }
.fd-legende { display: flex; gap: 12px; flex-wrap: wrap; font-size: 12px; color: var(--fd-muted); margin-top: 8px; }
.fd-legende b { display: inline-block; width: 10px; height: 10px; border-radius: 3px; margin-right: 4px; vertical-align: -1px; }

.fd-grille-barre { position: relative; height: 10px; background: var(--fd-subtle); border-radius: 99px; margin: 26px 0 8px; border: 1px solid var(--fd-border); }
.fd-plage { position: absolute; top: -1px; bottom: -1px; background: var(--fd-line); border-radius: 99px; }
.fd-rep { position: absolute; top: -22px; transform: translateX(-50%); font-size: 11.5px; font-weight: 600; white-space: nowrap; }
.fd-pt { position: absolute; top: -4px; width: 4px; height: 16px; border-radius: 2px; transform: translateX(-50%); }
.fd-bornes { display: flex; justify-content: space-between; font-size: 12px; color: var(--fd-muted); }

.fd-decision label { display: block; font-size: 13px; font-weight: 600; color: var(--fd-ink); margin-bottom: 6px; }
.fd-facultatif { font-weight: 400; color: var(--fd-muted); }
.fd-decision textarea { width: 100%; min-height: 84px; padding: 10px 12px; border: 1px solid #d3dad4; border-radius: 8px; font: 400 14px inherit; font-family: inherit; resize: vertical; box-sizing: border-box; }
.fd-decision textarea:focus { outline: 2px solid var(--fd-primary); outline-offset: 1px; }
.fd-decision textarea:disabled { background: var(--fd-subtle); }
.fd-notice { border-radius: 8px; padding: 10px 12px; font-size: 13px; margin: 12px 0 0; }
.fd-n-warn { background: #fff5e0; color: #94610a; }
.fd-n-err { background: #fdeeee; color: #b42323; }
.fd-n-ok { background: #e8f4e8; color: #1f7a2a; }
.fd-n-info { background: var(--fd-subtle); color: var(--fd-muted); }
.fd-actions { display: flex; gap: 10px; justify-content: flex-end; margin-top: 12px; }

@media (max-width: 960px) {
  .fd-grid { grid-template-columns: 1fr; }
  .fd-parcours { grid-template-columns: 1fr; }
}
</style>