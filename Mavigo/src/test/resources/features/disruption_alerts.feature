# language: fr
Fonctionnalité: Alertes de perturbations
  En tant qu'utilisateur de Mavigo
  Je veux être informé des perturbations
  Afin de pouvoir adapter mon trajet

  Scénario: Signaler une perturbation sur une ligne
    Etant donné un utilisateur avec un trajet actif sur la ligne "M1"
    Quand je signale une perturbation sur la ligne "M1"
    Alors la perturbation devrait être enregistrée
    Et la perturbation devrait être de type "LINE"
    Et la perturbation devrait être liée à mon trajet

  Scénario: Signaler une perturbation à une station
    Etant donné un utilisateur avec un trajet actif sur la ligne "M4"
    Quand je signale une perturbation à la station "stop:chatelet"
    Alors la perturbation devrait être enregistrée
    Et la perturbation devrait être de type "STATION"

  Scénario: Consulter les perturbations actives
    Etant donné une disruption existante sur la ligne "RER-A"
    Quand je consulte les perturbations actives
    Alors je devrais voir au moins 1 perturbation active
    Et je devrais voir la perturbation sur la ligne "RER-A"
