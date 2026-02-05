# language: fr
Fonctionnalité: Optimisation de trajet avec tâches
  En tant qu'utilisateur de Mavigo
  Je veux optimiser mon trajet pour inclure des tâches
  Afin de gagner du temps dans mes déplacements quotidiens

  Contexte:
    Etant donné un utilisateur existant dans le système

  Scénario: Planifier un trajet optimisé avec une tâche
    Etant donné un utilisateur avec une tâche localisée près de "Gare de Lyon"
    Quand je planifie un trajet de "Châtelet" à "Nation" avec optimisation
    Alors le trajet optimisé devrait inclure la tâche

  Scénario: Fallback vers trajet normal si optimisation impossible
    Etant donné une tâche sans localisation
    Quand je planifie un trajet avec optimisation
    Alors le système devrait retourner un trajet normal

  Scénario: Planifier un trajet optimisé avec plusieurs tâches
    Etant donné un utilisateur avec plusieurs tâches localisées
    Quand je planifie un trajet de "Gare du Nord" à "La Défense" avec optimisation
    Alors le trajet optimisé devrait inclure les tâches sur le chemin

  Scénario: Optimisation sans tâches disponibles
    Etant donné un utilisateur sans tâches
    Quand je planifie un trajet de "Châtelet" à "Nation" avec optimisation
    Alors le système devrait retourner un trajet normal sans tâches incluses

  Scénario: Tâche trop éloignée du trajet
    Etant donné un utilisateur avec une tâche localisée loin du trajet
    Quand je planifie un trajet de "Châtelet" à "Nation" avec optimisation
    Alors la tâche ne devrait pas être incluse dans le trajet optimisé
