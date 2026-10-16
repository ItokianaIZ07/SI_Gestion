# Conception SI de gestion

Ce projet a pour but de concevoir un logiciel de gestion d'entreprise

## Conventions de développement

### Technologies

- Base de données : PostgreSQL
- Backend : Spring Boot
- Frontend : Vue.js

### Nommage

- Les fonctions métier sont nommées en français, en camelCase, par exemple `maFonction`.
- Les tables de la base de données sont nommées en snake_case, par exemple `ma_table`.
- Les vues portent le préfixe `v_`.
- Les fonctions SQL portent le préfixe `f_`.

### Fichiers SQL

Chaque nouveau fichier SQL suit le format `nomDuDev_AAAA-MM-JJ_numero.sql`.
Un commentaire placé au début du fichier explique la raison de sa création.
