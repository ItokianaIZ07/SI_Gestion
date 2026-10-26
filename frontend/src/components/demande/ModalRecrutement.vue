<script setup>
import { aujourdhui } from "@/config/libelles";
import { reactive, computed } from "vue";

const props = defineProps({
  demande: {
    type: Object,
    required: true
  },
  statutsRecrutement: {
    type: Array,
    default: () => []
  }
});

const emit = defineEmits(["fermer", "valider"]);

const form = reactive({
  reference: "",
  statut: "",
  titre: props.demande.poste?.nom
    ?? props.demande.posteNom
    ?? props.demande.poste
    ?? "",
  description: props.demande.motif
    ?? props.demande.motifDetaille
    ?? "",
  dateOuverture: aujourdhui(),
  dateCloture: ""
});

const poste = computed(() => {
  const valeur = props.demande.poste;

  if (typeof valeur === "object" && valeur !== null) {
    return valeur.nom ?? valeur.libelle ?? "Non renseigné";
  }

  return valeur ?? props.demande.posteNom ?? "Non renseigné";
});

const departement = computed(() => {
  const valeur = props.demande.departement;

  if (typeof valeur === "object" && valeur !== null) {
    return valeur.nom ?? valeur.libelle ?? "Non renseigné";
  }

  return valeur ?? props.demande.departementNom ?? "Non renseigné";
});

const typeBesoin = computed(() => {
  const valeur = props.demande.typeBesoin;

  if (typeof valeur === "object" && valeur !== null) {
    return valeur.libelle ?? valeur.nom ?? "Non renseigné";
  }

  return valeur ?? props.demande.typeBesoinLibelle ?? "Non renseigné";
});

const date = (valeur) => {
  if (!valeur) return "Non renseignée";

  const resultat = String(valeur).slice(0, 10);
  const morceaux = resultat.split("-");

  if (morceaux.length !== 3) return resultat;

  return `${morceaux[2]}/${morceaux[1]}/${morceaux[0]}`;
};

const valider = () => {
  emit("valider", {
    demandeId: props.demande.id,
    reference: form.reference.trim(),
    statut: form.statut,
    titre: form.titre.trim(),
    description: form.description.trim(),
    dateOuverture: form.dateOuverture || aujourdhui(),
    dateCloture: form.dateCloture || null
  });
};
</script>

<template>
  <Teleport to="body">
    <Transition name="modal">
      <div
        class="modal-fond"
        @click.self="emit('fermer')"
      >
        <section
          class="modal"
          role="dialog"
          aria-modal="true"
          aria-labelledby="modal-titre"
        >
          <!-- En-tête -->
          <header class="modal-entete">
            <div>
              <span class="surtitre">RESSOURCES HUMAINES</span>
              <h2 id="modal-titre">Créer un recrutement</h2>
              <p>
                Vérifiez le besoin validé, puis renseignez
                les informations du recrutement.
              </p>
            </div>

            <button
              type="button"
              class="bouton-fermer"
              aria-label="Fermer"
              @click="emit('fermer')"
            >
              &times;
            </button>
          </header>

          <form @submit.prevent="valider">
            <div class="modal-corps">

              <!-- Détails du besoin validé -->
              <section class="section-demande">
                <div class="section-titre">
                  <div>
                    <h3>Demande d'origine</h3>
                    <p>
                      Ces informations proviennent du besoin validé.
                    </p>
                  </div>

                  <span class="badge-valide">
                    <span class="point"></span>
                    Validée
                  </span>
                </div>

                <div class="demande-grille">
                  <div class="detail">
                    <span>Référence de la demande</span>
                    <strong>
                      {{ demande.reference
                        ?? demande.numero
                        ?? `DEM-${demande.id}` }}
                    </strong>
                  </div>

                  <div class="detail">
                    <span>Département</span>
                    <strong>{{ departement }}</strong>
                  </div>

                  <div class="detail">
                    <span>Poste demandé</span>
                    <strong>{{ poste }}</strong>
                  </div>

                  <div class="detail">
                    <span>Type de besoin</span>
                    <strong>{{ typeBesoin }}</strong>
                  </div>

                  <div class="detail">
                    <span>Nombre de personnes</span>
                    <strong>
                      {{ demande.nombrePersonnes
                        ?? demande.nombre
                        ?? demande.nombrePersonne
                        ?? "Non renseigné" }}
                    </strong>
                  </div>

                  <div class="detail">
                    <span>Expérience requise</span>
                    <strong>
                      {{ demande.experienceRequise
                        ?? demande.experience
                        ?? "Non renseignée" }}
                    </strong>
                  </div>

                  <div class="detail">
                    <span>Date souhaitée d'arrivée</span>
                    <strong>
                      {{ date(
                        demande.dateSouhaiteeArrivee
                        ?? demande.dateArriveeSouhaitee
                        ?? demande.dateArrivee
                      ) }}
                    </strong>
                  </div>

                  <div class="detail">
                    <span>Lieu de travail</span>
                    <strong>
                      {{ demande.lieuTravail
                        ?? demande.lieu
                        ?? "Non renseigné" }}
                    </strong>
                  </div>

                  <div class="detail detail-large">
                    <span>Motif de la demande</span>
                    <p>
                      {{ demande.motifDetaille
                        ?? demande.motif
                        ?? "Aucun motif renseigné." }}
                    </p>
                  </div>
                </div>
              </section>

              <!-- Informations du recrutement -->
              <section class="section-recrutement">
                <div class="section-titre">
                  <div>
                    <h3>Informations du recrutement</h3>
                    <p>
                      Complétez les informations nécessaires à sa création.
                    </p>
                  </div>
                </div>

                <div class="form-grille">

                  <div class="champ">
                    <label for="reference" class="obligatoire">
                      Référence du recrutement
                    </label>

                    <input
                      id="reference"
                      v-model="form.reference"
                      type="text"
                      maxlength="100"
                      placeholder="Ex. REC-2026-001"
                      required
                    />
                  </div>

                  <div class="champ">
                    <label for="statut" class="obligatoire">
                      Statut initial
                    </label>

                    <select
                      id="statut"
                      v-model="form.statut"
                      required
                    >
                      <option value="" disabled>
                        Sélectionner un statut
                      </option>

                      <option
                        v-for="statut in statutsRecrutement"
                        :key="statut.value"
                        :value="statut.value"
                      >
                        {{ statut.label }}
                      </option>
                    </select>
                  </div>

                  <div class="champ champ-large">
                    <label for="titre" class="obligatoire">
                      Intitulé du recrutement
                    </label>

                    <input
                      id="titre"
                      v-model="form.titre"
                      type="text"
                      maxlength="200"
                      placeholder="Ex. Recrutement d'un développeur"
                      required
                    />
                  </div>

                  <div class="champ champ-large">
                    <label for="description" class="obligatoire">
                      Description
                    </label>

                    <textarea
                      id="description"
                      v-model="form.description"
                      rows="4"
                      placeholder="Décrivez le recrutement et les missions..."
                      required
                    ></textarea>
                  </div>

                  <div class="champ">
                    <label for="dateOuverture" class="obligatoire">
                      Date d'ouverture
                    </label>

                    <input
                      id="dateOuverture"
                      v-model="form.dateOuverture"
                      type="date"
                      required
                    />
                  </div>

                  <div class="champ">
                    <label for="dateCloture">
                      Date de clôture
                    </label>

                    <input
                      id="dateCloture"
                      v-model="form.dateCloture"
                      type="date"
                      :min="form.dateOuverture || undefined"
                    />
                  </div>

                </div>
              </section>
            </div>

            <!-- Pied du modal -->
            <footer class="modal-pied">
              <button
                type="button"
                class="bouton bouton-secondaire"
                @click="emit('fermer')"
              >
                Annuler
              </button>

              <button
                type="submit"
                class="bouton bouton-primaire"
              >
                Créer le recrutement
              </button>
            </footer>
          </form>
        </section>
      </div>
    </Transition>
  </Teleport>
</template>

<style scoped>
.modal-fond {
  position: fixed;
  inset: 0;
  z-index: 9999;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
  overflow-y: auto;
  background: rgb(0 0 0 / 45%);
  backdrop-filter: blur(3px);
}

.modal {
  display: flex;
  flex-direction: column;
  width: min(100%, 820px);
  max-height: calc(100vh - 40px);
  overflow: hidden;
  background: var(--couleur-card, #fff);
  border-radius: 16px;
  box-shadow: 0 20px 60px rgb(0 0 0 / 20%);
}

.modal-entete {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 20px;
  padding: 24px 28px;
  border-bottom: 1px solid #e8e8e8;
}

.surtitre {
  display: block;
  margin-bottom: 8px;
  color: var(--couleur-primaire, #015f09);
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 1.4px;
}

.modal-entete h2 {
  margin: 0;
  color: #222;
  font-size: 23px;
  font-weight: 750;
}

.modal-entete p,
.section-titre p {
  margin: 8px 0 0;
  color: #777;
  font-size: 13px;
  line-height: 1.5;
}

.bouton-fermer {
  display: grid;
  flex-shrink: 0;
  width: 36px;
  height: 36px;
  place-items: center;
  color: #555;
  font-size: 26px;
  background: #f3f3f3;
  border: none;
  border-radius: 9px;
  cursor: pointer;
}

.bouton-fermer:hover {
  background: #e8e8e8;
}

.modal form {
  display: flex;
  flex: 1;
  min-height: 0;
  flex-direction: column;
  overflow: hidden;
}

.modal-corps {
  padding: 24px 28px;
  overflow-y: auto;
}

.section-demande {
  padding: 20px;
  background: #f7f9f7;
  border: 1px solid #e1e9e1;
  border-radius: 12px;
}

.section-recrutement {
  margin-top: 28px;
}

.section-titre {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 20px;
}

.section-titre h3 {
  margin: 0;
  color: #252525;
  font-size: 16px;
  font-weight: 750;
}

.badge-valide {
  display: inline-flex;
  flex-shrink: 0;
  align-items: center;
  gap: 7px;
  padding: 7px 10px;
  color: #176b24;
  font-size: 11px;
  font-weight: 700;
  background: #e4f6e6;
  border-radius: 20px;
}

.point {
  width: 7px;
  height: 7px;
  background: #16812a;
  border-radius: 50%;
}

.demande-grille,
.form-grille {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18px;
}

.detail {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 7px;
}

.detail > span {
  color: #737373;
  font-size: 12px;
}

.detail > strong {
  overflow-wrap: anywhere;
  color: #292929;
  font-size: 13px;
  font-weight: 650;
  line-height: 1.5;
}

.detail-large,
.champ-large {
  grid-column: 1 / -1;
}

.detail-large p {
  margin: 0;
  color: #333;
  font-size: 13px;
  line-height: 1.6;
  overflow-wrap: anywhere;
  white-space: pre-wrap;
}

.champ {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 8px;
}

.champ label {
  color: #333;
  font-size: 13px;
  font-weight: 650;
}

.obligatoire::after {
  content: " *";
  color: var(--couleur-danger, #cd3d3d);
}

.champ input,
.champ select,
.champ textarea {
  box-sizing: border-box;
  width: 100%;
  min-height: 43px;
  padding: 10px 12px;
  color: #252525;
  font: inherit;
  font-size: 13px;
  background: #fff;
  border: 1px solid #dcdcdc;
  border-radius: 8px;
  outline: none;
  transition:
    border-color 0.2s ease,
    box-shadow 0.2s ease;
}

.champ textarea {
  min-height: 110px;
  resize: vertical;
}

.champ input:focus,
.champ select:focus,
.champ textarea:focus {
  border-color: var(--couleur-primaire, #015f09);
  box-shadow: 0 0 0 3px rgb(1 95 9 / 9%);
}

.modal-pied {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 12px;
  padding: 18px 28px;
  background: #fff;
  border-top: 1px solid #e8e8e8;
}

.bouton {
  display: inline-flex;
  min-height: 42px;
  align-items: center;
  justify-content: center;
  padding: 10px 18px;
  font: inherit;
  font-size: 13px;
  font-weight: 650;
  border: 1px solid transparent;
  border-radius: 8px;
  cursor: pointer;
}

.bouton-primaire {
  color: #fff;
  background: var(--couleur-primaire, #015f09);
}

.bouton-primaire:hover {
  background: #014a07;
}

.bouton-secondaire {
  color: #444;
  background: #fff;
  border-color: #ddd;
}

.bouton-secondaire:hover {
  background: #f5f5f5;
}

.modal-enter-active,
.modal-leave-active {
  transition: opacity 0.2s ease;
}

.modal-enter-active .modal,
.modal-leave-active .modal {
  transition:
    transform 0.2s ease,
    opacity 0.2s ease;
}

.modal-enter-from,
.modal-leave-to {
  opacity: 0;
}

.modal-enter-from .modal,
.modal-leave-to .modal {
  transform: translateY(12px) scale(0.98);
  opacity: 0;
}

@media (max-width: 600px) {
  .modal-fond {
    align-items: flex-start;
    padding: 12px;
  }

  .modal {
    max-height: calc(100vh - 24px);
    border-radius: 12px;
  }

  .modal-entete,
  .modal-corps {
    padding: 20px;
  }

  .section-demande {
    padding: 15px;
  }

  .demande-grille,
  .form-grille {
    grid-template-columns: 1fr;
    gap: 16px;
  }

  .detail-large,
  .champ-large {
    grid-column: auto;
  }

  .section-titre {
    flex-wrap: wrap;
  }

  .modal-pied {
    flex-wrap: wrap;
    padding: 16px 20px;
  }

  .modal-pied .bouton {
    flex: 1;
  }
}
</style>
