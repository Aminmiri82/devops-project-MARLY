export const state = {
  currentUser: null,
  currentView: localStorage.getItem("mavigo_view") || "journey",
  defaultTaskList: null,
  currentJourney: null,
  /** Derniers itinéraires affichés (avec includedTasks), pour les conserver au start. */
  lastDisplayedJourneys: null,
  lastNotifiedJourneyId: null,
  lastTasksSignature: null,
  pendingSuggestionTask: null,
  smartSuggestionsDismissedFor: null,
  ecoModeEnabled: false,
  wheelchairAccessible: false,
};
