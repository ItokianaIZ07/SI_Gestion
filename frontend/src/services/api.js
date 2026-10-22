import axios from "axios";

const api = axios.create({
  baseURL: "/api",
  withCredentials: true
});

let csrfToken = null;
let csrfHeaderName = null;
let csrfPromise = null;

// Récupère le jeton CSRF une seule fois à la fois.
async function getCsrfToken() {
  if (csrfToken && csrfHeaderName) {
    return;
  }

  if (!csrfPromise) {
    csrfPromise = axios
      .get("/api/auth/csrf", {
        withCredentials: true
      })
      .then((response) => {
        csrfToken = response.data.token;
        csrfHeaderName = response.data.headerName;
      })
      .finally(() => {
        csrfPromise = null;
      });
  }

  await csrfPromise;
}

// Ajoute automatiquement le jeton aux requêtes concernées.
api.interceptors.request.use(async (config) => {
  const method = (config.method || "get").toLowerCase();

  const methodsToProtect = ["post", "put", "patch", "delete"];

  if (methodsToProtect.includes(method)) {
    await getCsrfToken();

    config.headers.set(csrfHeaderName, csrfToken);
  }

  return config;
});

export default api;


export async function getBesoins() {
  const response = await api.get("/rh/besoins");
  return response.data;
}