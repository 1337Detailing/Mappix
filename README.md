# Mappix — open source Android gamepad mapper

Application Android Kotlin / Jetpack Compose Material 3 permettant de détecter et tester des manettes, de configurer des profils et de convertir les entrées reçues en actions logiques ou en taps tactiles via le service d'accessibilité.

**Version : 0.2.0** · minSdk 26 · targetSdk 35 · licence MIT · [GitHub](https://github.com/1337Detailing/Mappix)

## Fonctionnalités

- Manettes USB / Bluetooth détectées en direct avec nom, identifiants constructeur/produit et ID Android.
- Test interactif (boutons, D-pad, sticks, gâchettes), filtre par manette et historique des 20 derniers changements de boutons.
- Éditeur de mapping : boutons, gâchettes analogiques, sticks, actions bouton logique / tap tactile / directions, seuil de zone morte et sensibilité.
- Profils stockés dans Room : ajout, recherche, édition, duplication, renommage, suppression avec confirmation, import/export JSON (SAF).
- Affectation d'un profil à une application ou configuration d'un profil par défaut.
- Service au premier plan, notification d'arrêt, autorisations guidées, réglages DataStore FR/EN, thème sombre/clair.
- Overlay facultatif de marqueurs tactiles déplaçables avec sauvegarde des coordonnées.
- **Nouveau : mise à jour depuis Paramètres → Mises à jour**, via les versions GitHub Releases du dépôt, notes de version, téléchargement avec barre de progression, contrôle SHA-256 et installation après validation Android.
- Tests unitaires du moteur et du comparateur de versions, workflow GitHub Actions de debug et publication d'APK de release signé.

## Limites importantes

Le moteur ne peut résoudre que les événements de manette **que Mappix reçoit réellement**. Android standard ne fournit pas aux applications tierces une API publique permettant de capturer tous les boutons et axes pendant l'exécution d'un jeu tiers. Les touches virtuelles sont **des sorties logiques**, pas une injection globale. Les taps tactiles nécessitent le service d'accessibilité, lequel ne permet pas de garantir la capture de tous les contrôles (notamment les sticks) pendant un jeu tiers. Les overlays mobiles peuvent intercepter les touchers qui tombent sur leurs marqueurs.

Mappix n'utilise pas de root, ne prétend pas utiliser un backend Shizuku/ADB et **n'installe pas silencieusement de mises à jour**.

## Compiler

Prérequis : Android Studio récent, JDK 17, Gradle 8.9, Android SDK API 35. L'environnement de cette génération ne possédait ni Android SDK ni Gradle : aucune compilation complète d'APK n'a pu être vérifiée ici.

1. Ouvrir le dossier contenant `settings.gradle.kts` dans Android Studio.
2. Installer les composants SDK API 35 si Android Studio le demande.
3. Synchroniser Gradle. L'archive contient les paramètres du wrapper mais pas son JAR : utiliser Gradle 8.9 via Android Studio ou générer un wrapper complet (`gradle wrapper --gradle-version 8.9`).
4. Exécuter `gradle --no-daemon :app:testDebugUnitTest :app:assembleDebug`.
5. L'APK debug est produit sous `app/build/outputs/apk/debug/app-debug.apk` **après un build réussi**.

### Build automatique GitHub Actions

Le workflow `.github/workflows/android.yml` (onglet **Actions → Android CI → Run workflow**) compile et teste Mappix sur Ubuntu, puis fournit l'APK en tant qu'**artifact** de workflow. Il s'exécute aussi à chaque `push` sur `main` et lors des PR.

### Mises à jour via GitHub Releases

Le dépôt de publication est configuré dans `gradle.properties` :

```properties
mappixUpdateRepository=1337Detailing/Mappix
```

Il peut être surchargé avec `-PmappixUpdateRepository=owner/repo`. L'application interroge `https://api.github.com/repos/<owner>/<repo>/releases/latest` **seulement lorsque l'utilisateur appuie sur Rechercher**. Elle ne récupère qu'une version stable plus récente, choisit un APK `mappix*.apk`, télécharge l'asset, vérifie son hash SHA-256 (digest de l'API GitHub ou fichier `.apk.sha256` associé), puis vérifie que le nom de package Android est identique à Mappix. Elle lance ensuite l'installeur système via `FileProvider`.

**Important :** si aucune release GitHub n'a été publiée, l'application affiche qu'aucune version n'est disponible. Le fichier APK doit être **signé avec la même clé** que l'application déjà installée. Android refuse une mise à jour signée avec une autre clé. Un APK debug local ne pourra donc pas être mis à jour en APK release officiel sans désinstallation préalable (penser à exporter les profils).

### Publication signée automatique

Le workflow `.github/workflows/release.yml` se déclenche lors d'un `push` d'un tag `vX.Y.Z` et publie un **APK signé** avec sa somme SHA-256 dans GitHub Releases. Il nécessite quatre secrets GitHub Actions, définis dans le dépôt :

- `MAPPIX_KEYSTORE_BASE64` — fichier JKS encodé en Base64 (une ligne).
- `MAPPIX_KEYSTORE_PASSWORD` — mot de passe du keystore.
- `MAPPIX_KEY_ALIAS` — alias de la clé.
- `MAPPIX_KEY_PASSWORD` — mot de passe de la clé.

La clé de signature est indispensable pour les mises à jour. Ne jamais publier le keystore ou ses mots de passe dans Git. Il faut augmenter `versionCode` et `versionName` dans `app/build.gradle.kts` avant de publier chaque version. Le tag Git doit correspondre à `versionName`.

## Vérification fonctionnelle sur appareil

1. Installer l'APK sur un appareil Android 8+ puis brancher ou jumeler une manette.
2. Ouvrir **Test** et observer les axes, les boutons, les déconnexions et l'historique.
3. Créer un profil dans **Profils**, configurer une entrée et exporter/importer en JSON.
4. Activer les autorisations nécessaires dans **Paramètres** et démarrer le service depuis **Accueil**.
5. Vérifier un profil associé à une application et les marqueurs de superposition (si activés).
6. Publier une release signée dans le dépôt puis utiliser **Paramètres → Mises à jour** pour tester la détection et l'installation.

## Architecture

- `data/` : Room, DataStore, implémentations de repositories, client sécurisé de mise à jour.
- `domain/` : modèles, moteur de mapping indépendant d'Android.
- `input/` : InputManager, périphériques et événements.
- `service/` : service au premier plan, bridge d'accessibilité, overlay, état d'exécution.
- `ui/` : écrans Compose, ViewModels et Material 3.
- `di/` : injections Hilt.

## Roadmap

- [ ] Backend avancé capture/injection pour les appareils compatibles, à étudier séparément.
- [ ] Gestes tactiles continus et multitouch pour les sticks.
- [ ] Calibration par résolution / rotation et prise en charge des gâchettes atypiques.
- [ ] Tests instrumentés de l'overlay, du service et de l'installation APK.
- [ ] Diagnostics logs exportables, réglages de déduplication des axes.
- [ ] Signature stable et première release publique.

## Licence

MIT, voir [LICENSE](LICENSE).
