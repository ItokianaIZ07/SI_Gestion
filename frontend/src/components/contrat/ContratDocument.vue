<!-- BY : Brandon -->
<script setup>
import { computed } from "vue";
import { ENTREPRISE } from "@/config/entreprise";

const props = defineProps({
  contrat: { type: Object, required: true },
  signatureRh: { type: String, default: "" },       // URL blob ou dataURL
  signatureCandidat: { type: String, default: "" }, // URL blob ou dataURL
});

// Helpers d'affichage tolérants : null -> ""
const v = (x) => (x === null || x === undefined || x === "" ? "" : x);

function formatDate(d) {
  if (!d) return "";
  return new Intl.DateTimeFormat("fr-FR", { dateStyle: "long" }).format(new Date(d));
}

function formatMontant(m) {
  if (m === null || m === undefined || m === "") return "";
  return new Intl.NumberFormat("fr-FR", {
    style: "currency", currency: "MGA", maximumFractionDigits: 0,
  }).format(m);
}

const estCdd = computed(() => {
  const t = props.contrat?.typeContrat;
  return t === "CDD" || t === "STAGE";
});

const titreContrat = computed(() =>
  estCdd.value ? "CONTRAT DE TRAVAIL À DURÉE DÉTERMINÉE"
               : "CONTRAT DE TRAVAIL À DURÉE INDÉTERMINÉE"
);

const salarie = computed(() => {
  const c = props.contrat;
  const nomComplet = [v(c?.employePrenom), v(c?.employeNom)].filter(Boolean).join(" ");
  return {
    nomComplet: nomComplet || "—",
    cin: v(c?.employeCin),
    dateNaissance: formatDate(c?.employeDateNaissance),
    adresse: v(c?.employeAdresse),
    email: v(c?.employeEmail),
    telephone: v(c?.employeTelephone),
  };
});
</script>

<template>
  <article class="contrat-doc" aria-label="Contrat de travail">
    <!-- En-tête entreprise -->
    <header class="contrat-doc__entete">
      <div>
        <h1>{{ titreContrat }}</h1>
        <p class="contrat-doc__reference">Référence : {{ v(contrat?.reference) }}</p>
      </div>
      <div class="contrat-doc__entreprise">
        <strong>{{ ENTREPRISE.nom }}</strong>
        <p>{{ ENTREPRISE.adresse }}</p>
        <p>NIF : {{ ENTREPRISE.nif }} — STAT : {{ ENTREPRISE.stat }}</p>
        <p>{{ ENTREPRISE.telephone }} — {{ ENTREPRISE.email }}</p>
      </div>
    </header>

    <!-- Parties -->
    <section class="contrat-doc__parties">
      <h2>Entre les soussignés</h2>
      <p>
        <strong>{{ ENTREPRISE.nom }}</strong>, dont le siège social est situé
        {{ ENTREPRISE.adresse }}, représentée par {{ ENTREPRISE.representant }},
        ci-après dénommée « l'Employeur »,
      </p>
      <p class="contrat-doc__et">ET</p>
      <p>
        <strong>{{ salarie.nomComplet }}</strong><template v-if="salarie.dateNaissance">, né(e) le {{ salarie.dateNaissance }}</template><template v-if="salarie.cin">, titulaire de la CIN n° {{ salarie.cin }}</template><template v-if="salarie.adresse">, demeurant à {{ salarie.adresse }}</template><template v-if="salarie.email">, e-mail : {{ salarie.email }}</template><template v-if="salarie.telephone">, tél. : {{ salarie.telephone }}</template>,
        ci-après dénommé(e) « le Salarié ».
      </p>
      <p>Il a été convenu ce qui suit :</p>
    </section>

    <!-- Articles -->
    <section class="contrat-doc__article">
      <h2>Article 1 — Objet</h2>
      <p>
        Le Salarié est engagé en qualité de
        <strong>{{ v(contrat?.poste) || "—" }}</strong><template v-if="contrat?.departement">, au sein du département <strong>{{ contrat.departement }}</strong></template>.
      </p>
    </section>

    <section class="contrat-doc__article">
      <h2>Article 2 — Durée</h2>
      <p>Type de contrat : <strong>{{ v(contrat?.typeContrat) || "—" }}</strong>.</p>
      <p>Date de début : <strong>{{ formatDate(contrat?.dateDebut) || "—" }}</strong>.</p>
      <p v-if="contrat?.dateFin">Date de fin : <strong>{{ formatDate(contrat.dateFin) }}</strong>.</p>
      <p v-else>Le contrat est conclu pour une durée indéterminée.</p>
      <p v-if="contrat?.dureeContrat">Durée initiale : {{ contrat.dureeContrat }}.</p>
    </section>

    <section class="contrat-doc__article">
      <h2>Article 3 — Période d'essai</h2>
      <p v-if="contrat?.periodeEssaiJours">
        Le contrat comporte une période d'essai de
        <strong>{{ contrat.periodeEssaiJours }} jours</strong>, renouvelable une seule fois avec accord écrit des deux parties.
      </p>
      <p v-else>Aucune période d'essai n'est prévue.</p>
    </section>

    <section class="contrat-doc__article">
      <h2>Article 4 — Rémunération</h2>
      <p>
        Le Salarié percevra un salaire de base mensuel brut de
        <strong>{{ formatMontant(contrat?.salaire) || "—" }}</strong>, payable mensuellement.
      </p>
    </section>

    <section class="contrat-doc__article">
      <h2>Article 5 — Durée du travail</h2>
      <p>
        La durée hebdomadaire de travail est fixée à 40 heures, réparties sur
        cinq jours ouvrables, avec un jour de repos hebdomadaire.
      </p>
    </section>

    <section class="contrat-doc__article">
      <h2>Article 6 — Congés payés</h2>
      <p>
        Le Salarié bénéficie de 2,5 jours calendaires de congés payés par mois
        de service effectif, soit 30 jours ouvrables par an.
      </p>
    </section>

    <section class="contrat-doc__article">
      <h2>Article 7 — Protection sociale</h2>
      <p>
        Le Salarié sera affilié à la Caisse Nationale de Prévoyance Sociale (CNAPS)
        et au Service Médical Interentreprises (SMIE), conformément à la législation en vigueur.
      </p>
    </section>

    <section class="contrat-doc__article">
      <h2>Article 8 — Confidentialité</h2>
      <p>
        Le Salarié s'engage à ne pas divulguer, pendant la durée du contrat et
        après son expiration, les informations confidentielles de l'Employeur.
      </p>
    </section>

    <section class="contrat-doc__article">
      <h2>Article 9 — Rupture du contrat</h2>
      <p>
        La rupture du contrat est soumise aux dispositions du Code du travail
        malgache. Le préavis applicable dépend de l'ancienneté et de la catégorie
        professionnelle du Salarié.
      </p>
    </section>

    <!-- Signatures -->
    <section class="contrat-doc__signatures">
      <p class="contrat-doc__fait">
        Fait à Antananarivo, le
        <strong>{{ formatDate(contrat?.dateSignature) || "________________" }}</strong>.
      </p>
      <div class="contrat-doc__signature-grid">
        <div class="contrat-doc__signature">
          <p class="contrat-doc__signature-label">L'Employeur (RH)</p>
          <div class="contrat-doc__signature-box">
            <img v-if="signatureRh" :src="signatureRh" alt="Signature RH" />
            <span v-else class="contrat-doc__signature-vide">En attente</span>
          </div>
        </div>
        <div class="contrat-doc__signature">
          <p class="contrat-doc__signature-label">Le Salarié</p>
          <div class="contrat-doc__signature-box">
            <img v-if="signatureCandidat" :src="signatureCandidat" alt="Signature du salarié" />
            <span v-else class="contrat-doc__signature-vide">En attente</span>
          </div>
        </div>
      </div>
      <p class="contrat-doc__mention">« Lu et approuvé »</p>
    </section>
  </article>
</template>

<style scoped>
.contrat-doc {
  max-width: 780px;
  margin: 0 auto;
  padding: 40px 48px;
  background: #fff;
  color: #1f2421;
  font-family: "Times New Roman", Georgia, serif;
  font-size: 13.5px;
  line-height: 1.65;
}

.contrat-doc__entete {
  display: flex;
  justify-content: space-between;
  gap: 24px;
  padding-bottom: 18px;
  border-bottom: 2px solid #1f2421;
  margin-bottom: 24px;
}

.contrat-doc__entete h1 {
  margin: 0 0 6px;
  font-size: 17px;
  letter-spacing: 0.5px;
  text-transform: uppercase;
}

.contrat-doc__reference { margin: 0; color: #555; font-size: 12px; }

.contrat-doc__entreprise { text-align: right; font-size: 12px; color: #444; }
.contrat-doc__entreprise strong { display: block; color: #1f2421; margin-bottom: 4px; }
.contrat-doc__entreprise p { margin: 1px 0; }

.contrat-doc__parties { margin-bottom: 22px; }
.contrat-doc__parties h2 { font-size: 13px; text-transform: uppercase; margin: 0 0 10px; }
.contrat-doc__parties p { margin: 6px 0; text-align: justify; }
.contrat-doc__et { text-align: center; font-weight: 700; margin: 10px 0; }

.contrat-doc__article { margin-bottom: 18px; }
.contrat-doc__article h2 {
  margin: 0 0 6px;
  font-size: 13px;
  font-weight: 700;
  text-transform: uppercase;
}
.contrat-doc__article p { margin: 4px 0; text-align: justify; }

.contrat-doc__signatures {
  margin-top: 34px;
  padding-top: 20px;
  border-top: 1px solid #ccc;
}

.contrat-doc__fait { margin: 0 0 22px; font-style: italic; }

.contrat-doc__signature-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 28px;
}

.contrat-doc__signature-label {
  margin: 0 0 6px;
  font-weight: 700;
  font-size: 12px;
  text-transform: uppercase;
}

.contrat-doc__signature-box {
  min-height: 90px;
  border: 1px dashed #bbb;
  border-radius: 4px;
  display: grid;
  place-items: center;
  padding: 6px;
  background: #fafafa;
}

.contrat-doc__signature-box img { max-height: 80px; max-width: 100%; }
.contrat-doc__signature-vide { color: #999; font-style: italic; font-size: 12px; }

.contrat-doc__mention {
  margin: 22px 0 0;
  text-align: center;
  font-style: italic;
  font-size: 12px;
  color: #555;
}

@media print {
  .contrat-doc { padding: 0; box-shadow: none; }
}
</style>