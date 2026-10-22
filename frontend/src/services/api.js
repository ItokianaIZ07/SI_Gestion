import axios from "axios";

const BASE = import.meta.env.VITE_API_URL ?? "";

const http = axios.create({
  baseURL: `${BASE}`,
  withCredentials: true,
  headers: {
    Accept: "application/json",
  },
});

export class ApiError extends Error {
  constructor(message, status = 0) {
    super(message);
    this.name = "ApiError";
    this.status = status;
  }
}

let csrfToken = null;
let csrfHeaderName = "X-XSRF-TOKEN";
let csrfPromise = null;

function lireCookie(nom) {
  const trouve = document.cookie
    .split("; ")
    .find((cookie) => cookie.startsWith(`${nom}=`));

  return trouve
    ? decodeURIComponent(trouve.slice(nom.length + 1))
    : null;
}

function recupererCookieCsrf() {
  return lireCookie("XSRF-TOKEN");
}

async function getCsrfToken() {
  // Priorité au cookie si Spring utilise CookieCsrfTokenRepository.
  const cookieToken = recupererCookieCsrf();

  if (cookieToken) {
    csrfToken = cookieToken;
    csrfHeaderName = "X-XSRF-TOKEN";
    return csrfToken;
  }

  // Évite plusieurs demandes CSRF simultanées.
  if (csrfToken && csrfHeaderName) {
    return csrfToken;
  }

  if (!csrfPromise) {
    csrfPromise = http
      .get("/auth/csrf")
      .then((response) => {
        const data = response.data;

        // Méthode 1 : jeton renvoyé en JSON.
        if (data?.token) {
          csrfToken = data.token;
          csrfHeaderName = data.headerName || "X-XSRF-TOKEN";
        } else {
          // Méthode 2 : jeton fourni par le cookie.
          csrfToken = recupererCookieCsrf();
          csrfHeaderName = "X-XSRF-TOKEN";
        }

        return csrfToken;
      })
      .finally(() => {
        csrfPromise = null;
      });
  }

  return csrfPromise;
}

http.interceptors.request.use(async (config) => {
  const method = (config.method || "get").toLowerCase();
  const methodsToProtect = ["post", "put", "patch", "delete"];

  if (methodsToProtect.includes(method)) {
    await getCsrfToken();

    // Le cookie peut avoir été renouvelé entre-temps.
    const cookieToken = recupererCookieCsrf();
    const token = cookieToken || csrfToken;

    if (token) {
      const headerName = cookieToken
        ? "X-XSRF-TOKEN"
        : csrfHeaderName;

      config.headers.set
        ? config.headers.set(headerName, token)
        : (config.headers[headerName] = token);
    }
  }

  return config;
});

let redirectionEnCours = false;

async function sessionExpiree() {
  if (redirectionEnCours) return;

  redirectionEnCours = true;

  try {
    // Imports dynamiques pour éviter les dépendances circulaires.
    const [{ useAuthStore }, { default: router }] = await Promise.all([
      import("@/stores/authStore"),
      import("@/router"),
    ]);

    useAuthStore().logout();
    await router.replace({ name: "login" });
  } finally {
    redirectionEnCours = false;
  }
}

http.interceptors.response.use(
  (response) => {
    return response;
  },
  async (error) => {
    const status = error.response?.status;

    if (
      status === 401 &&
      window.location.pathname.startsWith("/si/app")
    ) {
      await sessionExpiree();
    }

    const message =
      error.response?.data?.message ||
      (status === 403
        ? "Action refusée : droits insuffisants ou session expirée."
        : status
          ? `Erreur ${status}`
          : "Le serveur est injoignable. Vérifiez votre connexion.");

    return Promise.reject(new ApiError(message, status || 0));
  }
);

export default http;

async function requete(methode, url, options = {}) {
  const config = {
    method: methode,
    url,
  };

  if (options.formData !== undefined) {
    // Ne pas fixer manuellement le Content-Type multipart.
    config.data = options.formData;
  } else if (options.json !== undefined) {
    config.data = options.json;
  }

  const response = await http.request(config);
  const contenu = response.data;

  if (contenu?.success === false) {
    throw new ApiError(
      contenu.message || "L'opération a échoué.",
      response.status
    );
  }

  // Compatible avec les réponses { success, message, data }
  // et avec les API qui renvoient directement leurs données.
  if (
    contenu !== null &&
    typeof contenu === "object" &&
    Object.prototype.hasOwnProperty.call(contenu, "data")
  ) {
    return contenu.data;
  }

  return contenu ?? null;
}

export const api = {
  get: (url) => requete("GET", url),

  post: (url, json) =>
    requete("POST", url, { json }),

  put: (url, json) =>
    requete("PUT", url, { json }),

  patch: (url, json) =>
    requete("PATCH", url, { json }),

  delete: (url, config = {}) =>
    requete("DELETE", url, config.data !== undefined
      ? { json: config.data }
      : {}),

  upload: (url, formData) =>
    requete("POST", url, { formData }),

  getBlob: async (url) => {
    const response = await http.get(url, {
      headers: {
        Accept: "application/pdf",
      },
      responseType: "blob",
    });

    return response.data;
  },
};

// ============================================================
// 8. FONCTION ORIGINALE : RÉCUPÉRATION DES BESOINS
// ============================================================

// Conserve exactement le comportement attendu par l'ancien code.

export async function getBesoins() {
  const response = await http.get("/rh/besoins");
  return response.data;
}