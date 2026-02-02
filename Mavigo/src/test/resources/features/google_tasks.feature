# language: fr
Fonctionnalité: Intégration Google Tasks
  En tant qu'utilisateur de Mavigo
  Je veux voir mes tâches Google sur mon trajet
  Afin de ne pas oublier mes courses et rendez-vous

  Scénario: Consulter mes tâches
    Etant donné un utilisateur avec des tâches Google Tasks liées
    Quand je consulte mes tâches
    Alors je devrais voir mes tâches
    Et je devrais avoir au moins 2 tâches

  Scénario: Créer une nouvelle tâche
    Etant donné un utilisateur avec des tâches Google Tasks liées
    Quand je crée une nouvelle tâche "Acheter des fleurs"
    Alors la tâche "Acheter des fleurs" devrait être créée

  Scénario: Marquer une tâche comme terminée
    Etant donné un utilisateur avec des tâches Google Tasks liées
    Et une tâche "Test Task" localisée à "Châtelet"
    Quand je marque la tâche "Test Task" comme terminée
    Alors la tâche "Test Task" devrait être marquée comme terminée

  Scénario: Tâche avec localisation
    Etant donné un utilisateur avec des tâches Google Tasks liées
    Et une tâche "Récupérer colis" localisée à "Gare de Lyon"
    Alors la tâche devrait avoir une localisation
