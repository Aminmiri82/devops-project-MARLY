# language: fr
Fonctionnalité: Planification de trajets
  En tant qu'utilisateur de Mavigo
  Je veux planifier des trajets en transport en commun
  Afin d'optimiser mes déplacements en Île-de-France

  Contexte:
    Etant donné un utilisateur existant dans le système

  Scénario: Planifier un trajet simple
    Quand je planifie un trajet de "Gare de Lyon" à "Châtelet"
    Alors je devrais recevoir au moins 1 option de trajet
    Et le trajet devrait avoir le statut "PLANNED"
    Et le trajet devrait être sauvegardé en base de données

  Scénario: Planifier un trajet avec le mode confort
    Quand je planifie un trajet de "Gare de Lyon" à "Châtelet" avec le mode confort activé
    Alors je devrais recevoir au moins 1 option de trajet
    Et le trajet devrait avoir le mode confort activé

  Scénario: Planifier un trajet entre deux stations de métro
    Quand je planifie un trajet de "Nation" à "La Défense"
    Alors je devrais recevoir au moins 1 option de trajet
    Et le trajet devrait avoir le statut "PLANNED"

  Scénario: Planifier un trajet depuis une gare
    Quand je planifie un trajet de "Gare du Nord" à "Aéroport CDG"
    Alors je devrais recevoir au moins 1 option de trajet
