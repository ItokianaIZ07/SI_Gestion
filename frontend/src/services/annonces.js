import { clientApi } from "./ressources";
import { aujourdhui } from "@/config/libelles";

export async function chargerAnnonces() {
  const [publications, offres] = await Promise.all([
    clientApi.publications(),
    clientApi.offres(),
  ]);
  const offreParId = new Map((offres ?? []).map((o) => [o.id, o]));
  const jour = aujourdhui();

  const parOffre = new Map();
  for (const p of publications ?? []) {
    const offre = offreParId.get(p.offreId);
    if (!offre || offre.statut !== "VALIDEE" || p.statut !== "PUBLIEE")
      continue;
    if (p.dateExpiration && p.dateExpiration < jour) continue;
    const existante = parOffre.get(offre.id);
    if (
      !existante ||
      (p.datePublication ?? "") > (existante.publication.datePublication ?? "")
    ) {
      parOffre.set(offre.id, { publication: p, offre });
    }
  }
  return [...parOffre.values()].sort((a, b) =>
    (b.publication.datePublication ?? "").localeCompare(
      a.publication.datePublication ?? "",
    ),
  );
}
