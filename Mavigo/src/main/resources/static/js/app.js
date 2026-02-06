import { initTheme } from "./ui/theme.js";
import { ensureToastUI } from "./ui/toast.js";
import { ensureTasksModalUI } from "./ui/tasks-modal.js";

import { setupAuthListeners, restoreSession } from "./features/auth.js";
import { setupJourneyForm, setupJourneyActions, setDefaultDepartureTime } from "./features/journey.js";
import { setupGoogleLinkListeners } from "./features/google-link.js";
import { setupComfortProfileListeners, setupOnboardingListeners } from "./features/comfort-profile.js";
import { setupDisruptionModal } from "./features/disruption.js";
import { setupNav } from "./features/nav.js";
import { setupTasks } from "./features/tasks.js";
import { setupDropdown } from "./features/user-dropdown.js";
import { setupHomeAddressListeners } from "./features/home-address.js";
import { setupSmartSuggestionsListeners } from "./features/smart-suggestions.js";
import { setupEcoScore } from "./features/eco-score.js";

init();

function init() {
  initTheme();
  setupAuthListeners();
  setupJourneyForm();
  setupJourneyActions();
  setupGoogleLinkListeners();
  setupComfortProfileListeners();
  setupDisruptionModal();
  setupNav();
  setupTasks();
  setupDropdown();
  setupHomeAddressListeners();
  setupSmartSuggestionsListeners();
  setupOnboardingListeners();
  setupEcoScore();
  setDefaultDepartureTime();
  ensureToastUI();
  ensureTasksModalUI();
  restoreSession();
}
