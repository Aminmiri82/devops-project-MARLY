import { api } from "../core/api.js";
import { CONFIG } from "../core/config.js";
import { state } from "../core/state.js";
import {
  ensureDefaultTaskListLoaded,
  updateTasksUIState,
} from "./tasks.js";
import { loadSmartSuggestions } from "./smart-suggestions.js";

const googleLinkStatusBadge = document.getElementById("googleLinkStatusBadge");

export function setupGoogleLinkListeners() {
  document
    .getElementById("linkGoogleTasksBtn")
    ?.addEventListener("click", startGoogleLinkFlow);
  document
    .getElementById("refreshGoogleLinkBtn")
    ?.addEventListener("click", refreshGoogleLink);
  window.addEventListener("message", handleGoogleLinkMessage);
}

function startGoogleLinkFlow() {
  const statusEl = document.getElementById("googleLinkStatus");

  if (!state.currentUser) {
    if (statusEl) statusEl.textContent = "Please log in first.";
    return;
  }

  const linkUrl = `/api/google/tasks/link?userId=${encodeURIComponent(
    state.currentUser.userId
  )}`;
  const popup = window.open(
    linkUrl,
    "googleTasksLink",
    CONFIG.POPUP_DIMENSIONS
  );

  if (!popup) {
    if (statusEl)
      statusEl.textContent =
        "Popup blocked. Please allow popups for this site.";
    return;
  }

  if (statusEl) statusEl.textContent = "Complete sign-in in the popup...";

  const watcher = setInterval(() => {
    if (popup.closed) {
      clearInterval(watcher);
      refreshGoogleLink();
    }
  }, CONFIG.POPUP_CHECK_INTERVAL_MS);
}

export async function refreshGoogleLink() {
  if (!state.currentUser?.userId) return;

  try {
    const user = await api.get(`/api/users/${state.currentUser.userId}`);
    state.currentUser = user;
    renderGoogleLinkStatus(user);
    updateTasksUIState();
    if (state.currentView === "tasks")
      ensureDefaultTaskListLoaded({ force: true });
    loadSmartSuggestions();
  } catch {
    // Silently fail on refresh
  }
}

export function clearGoogleLinkStatus() {
  if (!googleLinkStatusBadge) return;
  googleLinkStatusBadge.textContent = "";
  googleLinkStatusBadge.classList.remove("linked");
}

export function renderGoogleLinkStatus(user) {
  if (!googleLinkStatusBadge) return;
  if (!user) return clearGoogleLinkStatus();

  const linked = !!(user.googleAccountLinkedAt || user.googleAccountSubject);

  if (!linked) {
    googleLinkStatusBadge.textContent = "";
    googleLinkStatusBadge.classList.remove("linked");
  } else {
    googleLinkStatusBadge.textContent = "Linked";
    googleLinkStatusBadge.classList.add("linked");
  }
}

function handleGoogleLinkMessage(event) {
  if (event.origin !== window.location.origin) return;
  if (event.data && event.data.type === "GOOGLE_TASKS_LINKED")
    refreshGoogleLink();
}
