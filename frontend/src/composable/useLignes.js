//utile pour utiliser pour les missions et les compétences (une par ligne
export function useLignes() {
  function lignes(texte) {
    return texte
      .split('\n')
      .map(l => l.trim())
      .filter(Boolean)
  }

  return { lignes }
}