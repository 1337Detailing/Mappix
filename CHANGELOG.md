# Changelog

## 0.2.0 — 2026-11-02

### Ajouts
- Mise à jour dans les paramètres : GitHub Releases, comparaison de versions, description de release, téléchargement, vérification SHA-256, contrôle du package Android et installateur système.
- Publication d'APK signé avec checksum dans GitHub Actions (configuration des secrets nécessaire).
- Sélection de la manette dans le testeur, historique des boutons et commande de nettoyage.
- Recherche de profils par nom ou package et confirmation avant suppression.
- Tests unitaires de comparaison des versions.

### Corrections
- Filtrage des événements de manette dupliqués avant publication.
- Réinitialisation des états d'entrée lors du débranchement d'une manette dans le service.
- Validation des coordonnées, durées et valeurs numériques de l'import JSON.
- Suppression de l'injection de tap lorsque Mappix est au premier plan.
- Messages explicites en cas d'erreur de mise à jour (absence de release, APK manquant, intégrité défaillante).

### Limitations
- Aucun backend d'interception globale de sticks dans les jeux tiers.
- Build Android complet non vérifié localement faute de SDK Android / Gradle.
