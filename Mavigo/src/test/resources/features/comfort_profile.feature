# language: fr
Fonctionnalité: Profil de confort
  En tant qu'utilisateur de Mavigo
  Je veux configurer mes préférences de confort
  Afin d'avoir des trajets adaptés à mes besoins

  Scénario: Créer un profil avec accessibilité fauteuil roulant
    Etant donné je suis un utilisateur existant avec l'email "wheelchair-user@example.com"
    Quand je mets à jour mon profil de confort avec accès fauteuil roulant
    Alors mon profil devrait indiquer l'accès fauteuil roulant
    Et mes préférences devraient être sauvegardées

  Scénario: Limiter les correspondances pour trajet plus simple
    Etant donné je suis un utilisateur existant avec l'email "simple-journey@example.com"
    Quand je mets à jour mon profil avec un maximum de 1 correspondances
    Alors mon profil devrait avoir 1 correspondances maximum

  Scénario: Configurer des préférences de marche limitée
    Etant donné je suis un utilisateur existant avec l'email "limited-walking@example.com"
    Quand je mets à jour mon profil avec un maximum de 3 correspondances
    Alors mon profil devrait avoir 3 correspondances maximum
    Et mes préférences devraient être sauvegardées
