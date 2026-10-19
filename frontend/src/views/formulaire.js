(function () {
  var entree = document.getElementById('cv');
  var apercu = document.getElementById('apercu');
  var cadre = document.getElementById('apercu-cadre');
  var nomFichier = document.getElementById('apercu-nom');
  var lien = document.getElementById('apercu-lien');
  var erreur = document.getElementById('cv-erreur');
  var cin = document.getElementById('cin');
  var urlCourante = null;

  // CIN : uniquement des chiffres
  cin.addEventListener('input', function () {
    cin.value = cin.value.replace(/\D/g, '');
  });

  function afficherErreur(message) {
    erreur.textContent = message;
    erreur.hidden = !message;
  }

  function viderApercu() {
    if (urlCourante) {
      URL.revokeObjectURL(urlCourante);
      urlCourante = null;
    }
    cadre.removeAttribute('src');
    apercu.hidden = true;
  }

  entree.addEventListener('change', function () {
    var fichier = entree.files[0];
    viderApercu();
    afficherErreur('');

    if (!fichier) return;

    var estPdf = fichier.type === 'application/pdf' || /\.pdf$/i.test(fichier.name);
    if (!estPdf) {
      entree.value = '';
      afficherErreur('Le fichier doit être un PDF.');
      return;
    }

    urlCourante = URL.createObjectURL(fichier);
    cadre.src = urlCourante;
    lien.href = urlCourante;
    nomFichier.textContent = fichier.name;
    apercu.hidden = false;

    var reduit = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
    apercu.scrollIntoView({ behavior: reduit ? 'auto' : 'smooth', block: 'start' });
  });
})();
