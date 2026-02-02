# language: fr
Fonctionnalité: Gestion des utilisateurs
  En tant qu'utilisateur de Mavigo
  Je veux gérer mon profil et mes préférences
  Afin de personnaliser mon expérience de voyage

  Scénario: Créer un nouveau compte utilisateur
    Etant donné je suis un nouvel utilisateur
    Quand je crée mon compte
    Alors mon compte devrait être créé avec succès
    Et je devrais avoir un profil de confort par défaut

  Scénario: Mettre à jour les préférences d'accessibilité
    Etant donné je suis un utilisateur existant avec l'email "comfort-test@example.com"
    Quand je mets à jour mon profil de confort avec accès fauteuil roulant
    Alors mon profil devrait indiquer l'accès fauteuil roulant
    Et mes préférences devraient être sauvegardées

  Scénario: Limiter le nombre de correspondances
    Etant donné je suis un utilisateur existant avec l'email "transfers-test@example.com"
    Quand je mets à jour mon profil avec un maximum de 2 correspondances
    Alors mon profil devrait avoir 2 correspondances maximum
    Et mes préférences devraient être sauvegardées
