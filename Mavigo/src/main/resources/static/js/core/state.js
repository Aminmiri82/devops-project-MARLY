export const state = {
  currentUser: null,
  currentView: localStorage.getItem("mavigo_view") || "journey",
  defaultTaskList: null,
  currentJourney: null,
  lastNotifiedJourneyId: null,
  lastTasksSignature: null,
  pendingSuggestionTask: null,
  smartSuggestionsDismissedFor: null,
};
