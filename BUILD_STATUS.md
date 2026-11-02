# Rapport de vérification — Mappix 0.2.0

Date : 2026-11-02.

- **Projet source** : généré et archivé.
- **Vérification XML** : 7 fichiers XML analysés avec succès.
- **Internationalisation** : 118 clés de traduction françaises et anglaises, aucune manquante.
- **Références strings.xml** : aucun identifiant `R.string` non défini dans les sources Kotlin.
- **Moteur de mapping Kotlin pur** : compilé avec Kotlin 1.9.0 et testé via un petit programme JVM ; assertions validées pour l'anti-répétition des taps, les changements d'état des gâchettes, le relâchement et les directions du stick.
- **Tentative de compilation APK** : non réalisable ici. `gradle` n'est pas installé ; l'environnement ne dispose pas du SDK Android, et l'accès réseau nécessaire pour les installer est indisponible.
- **Compilation APK Android Studio / GitHub Actions** : **NON VÉRIFIÉE**. Le workflow `.github/workflows/android.yml` peut être lancé une fois le code poussé sur GitHub ; un build réussi publie un artifact APK debug.
- **Signature et mises à jour** : le workflow `release.yml` prépare une release signée sous réserve des secrets de signature. Aucune release n'a été produite pendant cette session.

La validation sur téléphone est nécessaire : reconnexion USB/Bluetooth, revocations de permissions, overlay, téléchargement, installation et signature.
