# Guide complet des dépendances et bibliothèques Vue.js 3

Ce document rassemble les principales dépendances, bibliothèques et outils utiles pour développer une application Vue.js 3 : routage, gestion d'état, appels API, communication en temps réel, formulaires, interface utilisateur, tests et outils complémentaires.

> **Important :** toutes ces dépendances ne sont pas nécessaires dans chaque projet. Installe uniquement celles qui répondent à un besoin réel.

## Sommaire

1. [Dépendances fondamentales](#1-dépendances-fondamentales)
2. [Vue Router : pages et navigation](#2-vue-router--pages-et-navigation)
3. [Pinia : état partagé](#3-pinia--état-partagé)
4. [Axios et Fetch : requêtes HTTP](#4-axios-et-fetch--requêtes-http)
5. [WebSocket, Socket.IO et SSE](#5-websocket-socketio-et-sse--communication-en-temps-réel)
6. [Bibliothèques utilitaires](#6-bibliothèques-utilitaires)
7. [Formulaires et validation](#7-formulaires-et-validation)
8. [Interface utilisateur et styles](#8-interface-utilisateur-et-styles)
9. [Tests et qualité du code](#9-tests-et-qualité-du-code)
10. [Autres bibliothèques selon les besoins](#10-autres-bibliothèques-selon-les-besoins)
11. [Exemple d'installation d'un projet](#11-exemple-dinstallation-dun-projet)
12. [Ordre conseillé pour apprendre](#12-ordre-conseillé-pour-apprendre)

---

## 1. Dépendances fondamentales

| Dépendance | Rôle | Obligatoire ? |
|---|---|---|
| `vue` | Créer les composants et gérer la réactivité | Oui pour une application Vue |
| `vite` | Serveur de développement et outil de construction | Généralement dans un projet Vue moderne |
| `@vitejs/plugin-vue` | Permettre à Vite de compiler les fichiers `.vue` | Avec Vite |
| `vue-router` | Gérer les pages et la navigation | Selon le projet |
| `pinia` | Partager l'état entre composants et pages | Selon le projet |
| `axios` | Envoyer des requêtes HTTP vers une API | Facultatif |
| `@vueuse/core` | Fournir des fonctions réutilisables pour Vue | Facultatif |
| `typescript` | Ajouter le typage statique à JavaScript | Facultatif |
| `vue-tsc` | Vérifier les types dans les fichiers Vue | Avec TypeScript |

### Créer un nouveau projet

```bash
npm create vue@latest
```

L'assistant de création permet de choisir des options telles que Vue Router, Pinia, les tests et les outils de qualité du code. Les options proposées peuvent évoluer selon les versions.

## 2. Vue Router : pages et navigation

**Documentation :** https://router.vuejs.org/

Vue Router permet de passer d'une page à une autre sans recharger entièrement l'application.

### Installation

```bash
npm install vue-router
```

### Notions importantes

- `createRouter()` : créer le routeur.
- `createWebHistory()` : utiliser des URL classiques.
- `RouterView` : afficher le composant correspondant à la route.
- `RouterLink` : créer un lien de navigation.
- `useRoute()` : récupérer les paramètres et la query string de la route courante.
- `useRouter()` : naviguer depuis JavaScript.
- Routes dynamiques, par exemple `/formations/:id`.
- Routes imbriquées : organiser les pages et sous-pages.
- Guards : protéger les pages qui nécessitent une authentification ou une autorisation.
- Lazy loading : charger les composants à la demande.
- Route 404 : afficher une page lorsque l'URL ne correspond à aucune route.

### Exemple de route dynamique

```js
{
  path: '/formations/:id',
  component: () => import('../views/FormationDetail.vue')
}
```

Dans cet exemple, `:id` est un paramètre dynamique et le composant est chargé à la demande.

## 3. Pinia : état partagé

**Documentation :** https://pinia.vuejs.org/

Pinia permet de partager des données entre plusieurs pages et composants sans transmettre constamment des props.

### Installation

```bash
npm install pinia
```

### Notions importantes

- `defineStore()` : créer un store.
- `state` : stocker les données.
- `getters` : calculer des valeurs à partir du state.
- `actions` : exécuter la logique et modifier l'état.
- `storeToRefs()` : extraire des propriétés en conservant leur réactivité.
- Persistance : conserver certaines données après actualisation, par exemple avec `localStorage` ou une extension adaptée.

### Exemple de store

```js
// src/stores/compteur.js
import { defineStore } from 'pinia'

export const useCompteurStore = defineStore('compteur', {
  state: () => ({
    nombre: 0
  }),

  getters: {
    double: (state) => state.nombre * 2
  },

  actions: {
    incrementer() {
      this.nombre++
    }
  }
})
```

Utilisation dans un composant :

```vue
<script setup>
import { useCompteurStore } from '@/stores/compteur'

const compteur = useCompteurStore()
</script>

<template>
  <p>{{ compteur.nombre }}</p>
  <p>{{ compteur.double }}</p>
  <button @click="compteur.incrementer()">
    Ajouter
  </button>
</template>
```

### Quand utiliser Pinia ?

- Conserver les informations de l'utilisateur connecté.
- Partager un panier entre plusieurs pages.
- Gérer des filtres communs.
- Partager les données d'un tableau de bord.

Pour une donnée utilisée uniquement dans un composant, `ref()` ou `reactive()` peut suffire.

## 4. Axios et Fetch : requêtes HTTP

### Axios

**Documentation :** https://axios-http.com/

Axios est une bibliothèque JavaScript qui permet d'envoyer des requêtes HTTP vers un serveur ou une API.

Installation :

```bash
npm install axios
```

Exemple :

```js
import axios from 'axios'

const reponse = await axios.get(
  'http://localhost:8080/api/formations'
)

console.log(reponse.data)
```

### Fetch

`fetch()` est intégré aux navigateurs : aucune installation n'est nécessaire.

```js
const reponse = await fetch(
  'http://localhost:8080/api/formations'
)

if (!reponse.ok) {
  throw new Error(`Erreur HTTP : ${reponse.status}`)
}

const formations = await reponse.json()
console.log(formations)
```

### Comparaison

| Fonctionnalité | Fetch | Axios |
|---|---|---|
| GET, POST, PUT, DELETE | Oui | Oui |
| Conversion de la réponse JSON | Appeler `response.json()` | Généralement automatique |
| Erreurs HTTP comme 404 ou 500 | Vérifier `response.ok` | Rejet par défaut pour les statuts en erreur |
| Intercepteurs | À programmer | Intégrés |
| Annulation de requêtes | Oui | Oui |

**Conseil :** apprends `fetch()` pour comprendre le fonctionnement natif des requêtes HTTP, puis Axios pour simplifier les projets comportant de nombreux appels API. Les deux solutions sont valables.

## 5. WebSocket, Socket.IO et SSE : communication en temps réel

### WebSocket natif

**Documentation :** https://developer.mozilla.org/fr/docs/Web/API/WebSockets_API

WebSocket permet au client et au serveur d'échanger des messages dans les deux sens à travers une connexion persistante.

Exemples d'utilisation :

- Messagerie instantanée.
- Notifications en direct.
- Jeux multijoueurs.
- Mise à jour de tableaux de bord.
- Suivi en temps réel d'une commande.

Aucune dépendance supplémentaire n'est nécessaire dans le navigateur pour utiliser l'API WebSocket native. Le serveur doit toutefois prendre en charge le protocole WebSocket.

```js
const socket = new WebSocket('ws://localhost:8080/ws')

socket.addEventListener('open', () => {
  socket.send('Bonjour serveur')
})

socket.addEventListener('message', (event) => {
  console.log(event.data)
})

socket.addEventListener('close', () => {
  console.log('Connexion fermée')
})

socket.addEventListener('error', (event) => {
  console.error('Erreur WebSocket', event)
})
```

### Socket.IO

**Documentation :** https://socket.io/docs/v4/

Socket.IO fournit un système d'événements, des mécanismes de reconnexion et des solutions de repli selon les conditions de connexion.

Installation du client pour Vue :

```bash
npm install socket.io-client
```

Exemple :

```js
import { io } from 'socket.io-client'

const socket = io('http://localhost:3000')

socket.on('connect', () => {
  console.log('Connecté au serveur', socket.id)
})

socket.on('nouveau-message', (message) => {
  console.log(message)
})

socket.emit('envoyer-message', {
  texte: 'Bonjour !'
})
```

**Attention :** le serveur doit utiliser Socket.IO lui aussi. Le client Socket.IO n'est pas interchangeable avec un client WebSocket natif.

### Server-Sent Events (SSE)

**Documentation :** https://developer.mozilla.org/fr/docs/Web/API/Server-sent_events

L'API native `EventSource` permet au serveur d'envoyer des événements au navigateur. Elle convient notamment aux notifications et aux flux d'informations lorsque la communication va essentiellement du serveur vers le client.

### Choisir entre les trois

- **WebSocket natif** : communication bidirectionnelle avec le protocole WebSocket.
- **Socket.IO** : communication événementielle avec des fonctionnalités supplémentaires, à condition que le serveur soit compatible.
- **SSE** : flux d'événements du serveur vers le navigateur lorsque le client n'a pas besoin d'envoyer des messages par le même canal.

## 6. Bibliothèques utilitaires

### VueUse — `@vueuse/core`

**Documentation :** https://vueuse.org/

Installation :

```bash
npm install @vueuse/core
```

VueUse propose des composables réutilisables pour Vue : stockage local, souris, réseau, temporisations, requêtes et bien d'autres besoins. Consulte la documentation pour connaître les fonctions disponibles.

### date-fns

Installation :

```bash
npm install date-fns
```

Bibliothèque pour formater, comparer et manipuler les dates.

### Lodash — `lodash-es`

Installation :

```bash
npm install lodash-es
```

Fournit des fonctions utilitaires pour les tableaux, les objets et d'autres opérations. Elle est facultative : JavaScript dispose déjà de nombreuses fonctions natives équivalentes.

## 7. Formulaires et validation

| Bibliothèque | Utilité | Documentation |
|---|---|---|
| VeeValidate | Gérer les formulaires, les règles et les erreurs de validation | https://vee-validate.logaretm.com/ |
| Zod | Définir des schémas et vérifier les données | https://zod.dev/ |
| Yup | Valider les données à l'aide de schémas | https://github.com/jquense/yup |
| FormKit | Construire des formulaires avec des composants dédiés | https://formkit.com/ |

Exemple de besoin : vérifier qu'un nom est obligatoire, qu'une adresse électronique est valide et qu'un mot de passe respecte les règles imposées avant d'envoyer les données à l'API.

La validation côté client améliore l'expérience utilisateur, mais les données doivent également être validées côté serveur.

## 8. Interface utilisateur et styles

### PrimeVue

**Documentation :** https://primevue.org/

Bibliothèque de composants prêts à l'emploi : tableaux, menus, fenêtres de dialogue, sélecteurs et formulaires.

### Vuetify

**Documentation :** https://vuetifyjs.com/

Framework de composants Vue qui suit les principes de Material Design.

### Tailwind CSS

**Documentation :** https://tailwindcss.com/

Framework CSS fondé sur des classes utilitaires pour construire rapidement le style d'une interface.

### Autres options

- Element Plus : https://element-plus.org/
- Naive UI : https://www.naiveui.com/

Tu n'as généralement pas besoin d'installer plusieurs bibliothèques complètes de composants d'interface dans le même projet. Choisis celle qui correspond à tes besoins et à ton design.

## 9. Tests et qualité du code

| Package | Installation | Utilité |
|---|---|---|
| Vitest | `npm install -D vitest` | Tests unitaires |
| Vue Test Utils | `npm install -D @vue/test-utils` | Tester les composants Vue |
| ESLint | `npm install -D eslint eslint-plugin-vue` | Détecter des problèmes et appliquer des règles de code |
| Prettier | `npm install -D prettier` | Formater automatiquement le code |
| Playwright | `npm install -D @playwright/test` | Tester l'application dans un navigateur |
| TypeScript | `npm install -D typescript vue-tsc` | Ajouter le typage et vérifier les fichiers Vue |

Guide officiel des tests Vue : https://vuejs.org/guide/scaling-up/testing

Remarque : certains outils peuvent demander une configuration supplémentaire. Le modèle de projet créé par `npm create vue@latest` peut déjà proposer d'en configurer certains.

## 10. Autres bibliothèques selon les besoins

| Besoin | Bibliothèque ou outil | Lien |
|---|---|---|
| Icônes | `lucide-vue-next` | https://lucide.dev/ |
| Graphiques | `vue-echarts` / Apache ECharts | https://echarts.apache.org/ |
| Tableaux de données avancés | `@tanstack/vue-table` | https://tanstack.com/table/latest |
| Glisser-déposer | `vuedraggable` | https://github.com/SortableJS/vue.draggable.next |
| Affichage de Markdown | `markdown-it` | https://github.com/markdown-it/markdown-it |
| Gestion des métadonnées HTML | Unhead | https://unhead.unjs.io/ |
| Traductions et multilingue | `vue-i18n` | https://vue-i18n.intlify.dev/ |
| Cartes interactives | Leaflet et `@vue-leaflet/vue-leaflet` | https://leafletjs.com/ |
| Authentification | Firebase Auth ou système du backend | https://firebase.google.com/docs/auth |
| Fichiers et images | Dépend du serveur et du stockage choisi | À choisir selon le projet |

Ces bibliothèques répondent à des besoins précis. Elles ne sont pas toutes indispensables dans une application Vue.

## 11. Exemple d'installation d'un projet

### Créer un nouveau projet

```bash
npm create vue@latest
```

Dans l'assistant, tu peux sélectionner :

- **Vue Router :** oui, si le projet comporte plusieurs pages.
- **Pinia :** oui, si tu veux apprendre la gestion d'état partagé.
- **Vitest :** oui, si tu souhaites apprendre les tests.
- **ESLint :** oui, pour contrôler la qualité du code.
- **Prettier :** oui, si tu veux formater automatiquement le code.

Les options exactes peuvent changer selon la version de l'outil.

### Installer les dépendances

Dans le dossier du projet :

```bash
cd nom-du-projet
npm install
npm install axios @vueuse/core socket.io-client
npm run dev
```

Cette base peut inclure le routage, Pinia, les requêtes HTTP, des fonctions utilitaires et le client Socket.IO.

Si le projet existe déjà, ne recrée pas le projet : installe uniquement les dépendances qui manquent. N'installe `socket.io-client` que si le serveur utilise Socket.IO.

## 12. Ordre conseillé pour apprendre

1. **Vue 3 et Composition API** : `ref`, `reactive`, `computed`, `watch`, props, événements, hooks du cycle de vie et composables.
2. **Vue Router** : pages, paramètres dynamiques, navigation, routes protégées et page 404.
3. **API et requêtes HTTP** : `fetch`, Axios, GET, POST, PUT, DELETE, erreurs et états de chargement.
4. **Pinia** : état partagé, getters, actions et persistance.
5. **Formulaires et validation** : règles, messages d'erreur et envoi des données au serveur.
6. **WebSocket et Socket.IO** : connexion, déconnexion, événements et messages en temps réel.
7. **Outils avancés** : VueUse, bibliothèque d'interface, TypeScript, tests et optimisation.

## Conclusion

Pour devenir autonome avec Vue.js, maîtrise d'abord Vue 3, Vue Router, les appels API avec `fetch()` ou Axios et Pinia. Apprends ensuite les formulaires, WebSocket, les bibliothèques d'interface et les outils de test.

**N'installe pas toutes les dépendances en même temps.** Chaque bibliothèque doit répondre à un besoin concret, et comprendre JavaScript et Vue reste plus important que d'accumuler des packages.
