import { api } from "../core/api.js";
import { state } from "../core/state.js";
import { escapeHtml, getTomorrowDateString } from "../core/utils.js";
import { showToast } from "../ui/toast.js";
import { isGoogleLinked } from "./tasks.js";
import { openHomeAddressModal } from "./home-address.js";
import { prefillJourneyFromSuggestion } from "./journey.js";

const smartSuggestionsFlyout = document.getElementById("smartSuggestionsFlyout");
const smartSuggestionsContent = document.getElementById("smartSuggestionsContent");
const smartSuggestionsCloseBtn = document.getElementById("smartSuggestionsCloseBtn");

export function setupSmartSuggestionsListeners() {
  smartSuggestionsCloseBtn?.addEventListener("click", dismissSmartSuggestions);
}

export function dismissSmartSuggestions() {
  if (!smartSuggestionsFlyout) return;
  smartSuggestionsFlyout.classList.add("hidden");
  state.smartSuggestionsDismissedFor = getTomorrowDateString();
}

export async function loadSmartSuggestions() {
  if (!smartSuggestionsFlyout || !smartSuggestionsContent) return;

  if (!state.currentUser || !isGoogleLinked()) {
    smartSuggestionsFlyout.classList.add("hidden");
    smartSuggestionsContent.innerHTML = "";
    return;
  }

  const date = getTomorrowDateString();
  if (state.smartSuggestionsDismissedFor === date) {
    smartSuggestionsFlyout.classList.add("hidden");
    return;
  }

  smartSuggestionsContent.innerHTML =
    '<p class="loading">Loading smart suggestions...</p>';

  try {
    const url = `/api/google/tasks/users/${state.currentUser.userId}/suggestions?date=${encodeURIComponent(
      date
    )}`;
    const tasks = await api.get(url);
    const suggestions = (Array.isArray(tasks) ? tasks : []).filter(
      (t) => t?.locationQuery
    );

    if (!suggestions.length) {
      smartSuggestionsFlyout.classList.add("hidden");
      smartSuggestionsContent.innerHTML = "";
      return;
    }

    smartSuggestionsFlyout.classList.remove("hidden");
    renderSmartSuggestions(suggestions);
  } catch {
    smartSuggestionsFlyout.classList.add("hidden");
    smartSuggestionsContent.innerHTML = "";
  }
}

function renderSmartSuggestions(tasks) {
  if (!smartSuggestionsContent) return;

  const intro = `
    <div class="smart-suggestions-intro">
      You’ve got tasks coming up tomorrow. Want to plan the journey for them?
    </div>
  `;

  const listHtml = tasks
    .map((t, idx) => {
      const title = escapeHtml(t?.title || "Untitled");
      const location = escapeHtml(t?.locationQuery || "Unknown location");
      return `
        <div class="smart-suggestion-item">
          <div class="smart-suggestion-details">
            <div class="smart-suggestion-title">${title}</div>
            <div class="smart-suggestion-location">Location: <strong>${location}</strong></div>
          </div>
          <div class="smart-suggestion-actions">
            <button type="button" class="btn btn-primary btn-sm smart-suggestion-cta" data-action="prefill" data-index="${idx}">
              Show me the journey
            </button>
          </div>
        </div>
      `;
    })
    .join("");

  smartSuggestionsContent.innerHTML = `
    ${intro}
    <div class="smart-suggestions-list">
      ${listHtml}
    </div>
  `;

  smartSuggestionsContent
    .querySelectorAll("[data-action='prefill']")
    .forEach((btn) => {
      btn.addEventListener("click", () => {
        const idx = Number(btn.getAttribute("data-index"));
        const task = tasks[idx];
        handleSuggestionClick(task);
      });
    });
}

function handleSuggestionClick(task) {
  if (!task) return;

  dismissSmartSuggestions();

  if (!state.currentUser?.homeAddress) {
    state.pendingSuggestionTask = task;
    openHomeAddressModal();
    showToast("Add your home address to plan this journey.", {
      variant: "warning",
      durationMs: 5000,
    });
    return;
  }

  prefillJourneyFromSuggestion(task);
}
