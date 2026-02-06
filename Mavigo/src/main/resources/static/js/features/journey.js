import { api } from "../core/api.js";
import { state } from "../core/state.js";
import {
  escapeHtml,
  formatDateTime,
  formatDuration,
  formatLineBadge,
  formatMode,
  getTomorrowDepartureLocalIso,
} from "../core/utils.js";
import { showToast } from "../ui/toast.js";
import { openTasksModal } from "../ui/tasks-modal.js";
import { validateComfortMode } from "./comfort-profile.js";
import { setView } from "./nav.js";
import { openDisruptionModal } from "./disruption.js";

const journeyForm = document.getElementById("journeyForm");
const resultsDiv = document.getElementById("results");
const currentJourneyPanel = document.getElementById("currentJourneyPanel");
const currentJourneyContent = document.getElementById("currentJourneyContent");
const completeJourneyBtn = document.getElementById("completeJourneyBtn");
const cancelJourneyBtn = document.getElementById("cancelJourneyBtn");
const departureInput = document.getElementById("departure");
const reportDisruptionBtn = document.getElementById("reportDisruptionBtn");
const journeyComfortSelection = document.getElementById("journeyComfortSelection");
const planJourneyPanel = document.getElementById("planJourneyPanel");
const journeyIncludeTask = document.getElementById("journeyIncludeTask");

export function setupJourneyForm() {
  journeyForm?.addEventListener("submit", handleJourneySubmit);

  const ecoToggle = document.getElementById("ecoModeToggle");
  if (ecoToggle) {
    ecoToggle.checked = state.ecoModeEnabled;
    ecoToggle.addEventListener("change", (e) => {
      state.ecoModeEnabled = e.target.checked;
    });
  }

  const wheelchairToggle = document.getElementById("wheelchairToggle");
  if (wheelchairToggle) {
    wheelchairToggle.checked = state.wheelchairAccessible;
    wheelchairToggle.addEventListener("change", (e) => {
      state.wheelchairAccessible = e.target.checked;
    });
  }

  // Home Suggestions
  const fromInput = document.getElementById("from");
  const toInput = document.getElementById("to");

  [fromInput, toInput].forEach((input) => {
    if (input) {
      input.addEventListener("focus", () => showHomeSuggestion(input));
      // Use setTimeout to allow click events on the suggestion to register before hiding
      input.addEventListener("blur", () => {
        setTimeout(hideHomeSuggestion, 200);
      });
    }
  });
}

export function setupJourneyActions() {
  completeJourneyBtn?.addEventListener("click", completeJourney);
  cancelJourneyBtn?.addEventListener("click", cancelJourney);
  reportDisruptionBtn?.addEventListener("click", () => openDisruptionModal());

  resultsDiv?.addEventListener("click", (e) => {
    const btn = e.target.closest(".start-journey-btn");
    if (!btn) return;
    const journeyId = btn.getAttribute("data-journey-id");
    if (!journeyId) return;
    startJourney(journeyId, btn);
  });
}

async function startJourney(journeyId, btnElement) {
  if (!state.currentUser) return;

  const allButtons = document.querySelectorAll(".start-journey-btn");

  if (btnElement) {
    btnElement.disabled = true;
    btnElement.textContent = "Starting...";
  }

  allButtons.forEach((btn) => {
    if (btn !== btnElement) btn.classList.add("hidden");
  });

  try {
    const journey = await api.post(`/api/journeys/${journeyId}/start`);
    const prev = (state.lastDisplayedJourneys || []).find(
      (j) => String(j?.journeyId) === String(journeyId)
    );
    const withTasks =
      prev &&
      Array.isArray(prev.includedTasks) &&
      prev.includedTasks.length > 0
        ? { ...journey, includedTasks: prev.includedTasks }
        : journey;
    updateCurrentJourney(withTasks);
    if (withTasks.newBadges && withTasks.newBadges.length > 0) {
      import("./badge-unlock.js").then((module) => {
        module.showBadgeUnlocks(withTasks.newBadges);
      });
    }
  } catch (err) {
    showToast(err.message, { variant: "warning" });
    allButtons.forEach((btn) => {
      btn.classList.remove("hidden");
      btn.disabled = false;
      if (btn === btnElement) btn.textContent = "Start Journey";
    });
  }
}

async function completeJourney() {
  if (!state.currentJourney) return;
  const journeyWithTasks = state.currentJourney;
  try {
    const journey = await api.post(
      `/api/journeys/${state.currentJourney.journeyId}/complete`
    );
    await completeIncludedTasksIfAny(journeyWithTasks);
    updateCurrentJourney(journey);
    showToast("Journey completed!", { variant: "success" });

    // Show badge unlock celebration if new badges were earned
    if (journey.newBadges && journey.newBadges.length > 0) {
      import("./badge-unlock.js").then((module) => {
        module.showBadgeUnlocks(journey.newBadges);
      });
    }
  } catch (err) {
    showToast(err.message, { variant: "warning" });
  }
}

async function cancelJourney() {
  if (!state.currentJourney) return;
  if (!confirm("Are you sure you want to cancel this journey?")) return;

  try {
    const journey = await api.post(
      `/api/journeys/${state.currentJourney.journeyId}/cancel`
    );
    updateCurrentJourney(journey);
  } catch (err) {
    showToast(err.message, { variant: "warning" });
  }
}

/**
 * Récupère l'ID de la liste Google Tasks par défaut (avec cache dans state.defaultTaskList).
 */
async function getDefaultTaskListId() {
  if (!state.currentUser) return null;

  if (state.defaultTaskList?.id) {
    return state.defaultTaskList.id;
  }

  try {
    const list = await api.get(
      `/api/google/tasks/users/${state.currentUser.userId}/default-list`
    );
    if (list?.id) {
      state.defaultTaskList = { id: list.id, title: list.title || "Default" };
      return list.id;
    }
  } catch (err) {
    // On ne bloque pas la complétion du trajet si on ne peut pas charger la liste
    showToast(
      err?.message || "Could not load default Google Tasks list to complete task.",
      { variant: "warning" }
    );
  }
  return null;
}

/**
 * Marque comme complétées, dans Google Tasks, les tâches incluses dans le trajet.
 * On réutilise l’endpoint backend existant /complete, sans stockage local.
 */
async function completeIncludedTasksIfAny(journey) {
  if (!journey || !state.currentUser) return;

  const includedTasks = Array.isArray(journey.includedTasks)
    ? journey.includedTasks
    : [];
  if (!includedTasks.length) return;

  const listId = await getDefaultTaskListId();
  if (!listId) return;

  const userId = state.currentUser.userId;

  for (const t of includedTasks) {
    const googleTaskId =
      t.googleTaskId || t.taskId || t.id || null;
    if (!googleTaskId) continue;

    try {
      const url = `/api/google/tasks/users/${userId}/lists/${encodeURIComponent(
        listId
      )}/tasks/${encodeURIComponent(googleTaskId)}/complete`;
      await api.patch(url);
    } catch (_err) {
      // Ne pas bloquer la complétion du trajet si une tâche Google échoue
    }
  }
}

export function updateCurrentJourney(journey) {
  state.currentJourney = journey;

  if (
    journey &&
    (journey.status === "PLANNED" ||
      journey.status === "IN_PROGRESS" ||
      journey.status === "REROUTED")
  ) {
    currentJourneyPanel?.classList.remove("hidden");
    planJourneyPanel?.classList.add("hidden");
    renderCurrentJourney(journey);

    document.querySelector(".results-panel")?.classList.add("hidden");
  } else {
    currentJourneyPanel?.classList.add("hidden");
    planJourneyPanel?.classList.remove("hidden");
    document.querySelector(".results-panel")?.classList.remove("hidden");
    if (resultsDiv) {
      resultsDiv.innerHTML =
        '<p class="results-placeholder">Your journey results will appear here.</p>';
    }
    state.currentJourney = null;
  }
}

function calculateProgress(journey) {
  if (journey.status !== "IN_PROGRESS" && journey.status !== "REROUTED")
    return 0;

  const now = new Date();
  const start = new Date(journey.actualDeparture || journey.plannedDeparture);
  const end = new Date(journey.plannedArrival);

  if (isNaN(start) || isNaN(end) || end <= start) return 0;

  if (now < start) return 0;
  if (now > end) return 100;

  const progress = ((now - start) / (end - start)) * 100;
  return Math.min(Math.max(Math.round(progress), 0), 100);
}

function renderCurrentJourney(journey) {
  if (!currentJourneyContent) return;
  const statusClass =
    journey.status === "IN_PROGRESS" ? "status-active" : "status-planned";
  const progress = calculateProgress(journey);
  const hasSegments =
    Array.isArray(journey.segments) && journey.segments.length > 0;
  const hasTasks =
    Array.isArray(journey.includedTasks) && journey.includedTasks.length > 0;

  currentJourneyContent.innerHTML = `
        <div class="journey-status-card">
            <div class="status-badge ${statusClass}">${journey.status}</div>
            ${journey.status === "REROUTED" || journey.disruptionCount > 0
      ? '<div class="disruption-warning">⚠️ Disruption : New Journey Started</div>'
      : ""
    }
            <h3>${journey.originLabel} → ${journey.destinationLabel}</h3>

            ${journey.status === "IN_PROGRESS" || journey.status === "REROUTED"
      ? `
                <div class="progress-container">
                    <div class="progress-bar" style="width: ${progress}%"></div>
                </div>
                <span class="progress-text">${progress}% Completed</span>
            `
      : ""
    }

            <p><strong>Planned Departure:</strong> ${formatDateTime(
      journey.plannedDeparture
    )}</p>
            ${journey.actualDeparture
      ? `<p><strong>Started:</strong> ${formatDateTime(
        journey.actualDeparture
      )}</p>`
      : ""
    }
            <p><strong>Planned Arrival:</strong> ${formatDateTime(
      journey.plannedArrival
    )}</p>
        </div>
    `;

  if (journey.status === "IN_PROGRESS" || journey.status === "REROUTED") {
    completeJourneyBtn?.classList.remove("hidden");
    cancelJourneyBtn?.classList.remove("hidden");
    reportDisruptionBtn?.classList.remove("hidden");
  } else {
    completeJourneyBtn?.classList.add("hidden");
    cancelJourneyBtn?.classList.add("hidden");
    reportDisruptionBtn?.classList.add("hidden");
  }
}

async function handleJourneySubmit(e) {
  e.preventDefault();

  if (!state.currentUser) {
    if (resultsDiv)
      resultsDiv.innerHTML =
        '<p class="error-message">Please log in first.</p>';
    return;
  }

  const from = (document.getElementById("from")?.value || "").trim();
  const to = (document.getElementById("to")?.value || "").trim();
  const departure = departureInput?.value || "";
  const comfortSelection = journeyComfortSelection?.value || "disabled";
  const includeTask = !!journeyIncludeTask?.checked;

  if (!departure) {
    if (resultsDiv)
      resultsDiv.innerHTML =
        '<p class="error-message">Please select a departure time.</p>';
    return;
  }

  if (comfortSelection !== "disabled" && !validateComfortMode()) {
    return;
  }

  // Use green mode (eco-mode) from state
  const ecoModeEnabled = state.ecoModeEnabled;

  const payload = {
    journey: {
      userId: state.currentUser.userId,
      originQuery: from,
      destinationQuery: to,
      departureTime: departure,
      ecoModeEnabled: state.ecoModeEnabled,
      wheelchairAccessible: state.wheelchairAccessible,
    },
    preferences: {
      comfortMode: comfortSelection !== "disabled",
      namedComfortSettingId:
        comfortSelection !== "disabled" ? comfortSelection : null,
    },
  };

  // Optionnel : inclure une tâche Google sur le chemin (optimisation côté backend)
  if (includeTask) {
    const taskDetails = await loadTasksForJourney();
    if (Array.isArray(taskDetails) && taskDetails.length > 0) {
      payload.journey.taskDetails = taskDetails;
    }
  }

  const taskOptimizationRequested =
    !!payload.journey.taskDetails &&
    Array.isArray(payload.journey.taskDetails) &&
    payload.journey.taskDetails.length > 0;

  if (resultsDiv)
    resultsDiv.innerHTML = '<p class="loading">Planning your journey...</p>';

  try {
    let journeys = await api.post("/api/journeys", payload);

    if (resultsDiv) resultsDiv.innerHTML = "";
    let list = Array.isArray(journeys) ? journeys : [journeys];

    // Si l’optimisation avec tâche ne renvoie rien, on retente sans tâche
    if (list.length === 0 && taskOptimizationRequested) {
      const fallbackPayload = {
        ...payload,
        journey: { ...payload.journey },
      };
      delete fallbackPayload.journey.taskDetails;

      try {
        journeys = await api.post("/api/journeys", fallbackPayload);
        list = Array.isArray(journeys) ? journeys : [journeys];
      } catch {
        // on ignore, on tombera sur le message "No journey found"
      }
    }

    if (list.length === 0) {
      if (resultsDiv)
        resultsDiv.innerHTML = '<p class="error-message">No journey found.</p>';
      return;
    }

    displayJourneyResults(list);
  } catch (err) {
    if (resultsDiv)
      resultsDiv.innerHTML = '<p class="error-message">No journey found.</p>';
  }
}

/**
 * Charge les tâches Google pertinentes pour un trajet et les convertit
 * au format attendu par le backend (TaskDetailDto), sans duplication de logique.
 */
async function loadTasksForJourney() {
  if (!state.currentUser) return [];

  try {
    const url = `/api/google/tasks/users/${state.currentUser.userId}/for-journey?includeCompleted=false`;
    const tasks = await api.get(url);

    const list = Array.isArray(tasks) ? tasks : [];

    return list
      .map((t) => {
        const locationHint = t?.locationHint || {};
        const lat = typeof locationHint.lat === "number" ? locationHint.lat : null;
        const lng = typeof locationHint.lng === "number" ? locationHint.lng : null;

        return {
          id: String(t?.id || ""),
          title: t?.title || "",
          locationQuery: t?.locationQuery || "",
          lat,
          lng,
          completed: !!t?.completed,
        };
      })
      .filter(
        (t) =>
          t.id &&
          t.lat !== null &&
          t.lng !== null &&
          !t.completed
      );
  } catch (err) {
    showToast(err?.message || "Could not load tasks for journey.", {
      variant: "warning",
    });
    return [];
  }
}

/**
 * Displays journey results with shared info (tasks, modes) shown once at top
 */
export function displayJourneyResults(journeys) {
  if (!resultsDiv || !journeys.length) return;

  state.lastDisplayedJourneys = journeys;
  const firstJourney = journeys[0];
  const allTasks = collectUniqueTasksFromJourneys(journeys);

  const tasksBannerHtml = allTasks.length
    ? `
      <div class="tasks-on-route-banner">
        <div>
          <div class="tasks-on-route-title">${allTasks.length} task${allTasks.length > 1 ? "s" : ""
    } on your route</div>
          <div class="tasks-on-route-sub">${escapeHtml(
      allTasks[0]?.title || "Task"
    )}</div>
        </div>
        <button type="button" class="btn btn-outline btn-sm" id="viewTasksOnRouteBtn">View</button>
      </div>
    `
    : "";

  const modesHtml = `
    <div class="journey-modes">
      <span>Comfort Profile: ${firstJourney?.comfortModeEnabled ? "Active" : "None"}</span>
      ${state.ecoModeEnabled ? '<span class="mode-badge green">🌱 Green</span>' : ""}
      ${state.wheelchairAccessible ? '<span class="mode-badge accessible">♿ Accessibility</span>' : ""}
    </div>
  `;

  resultsDiv.innerHTML = `
    <div class="journey-results-header">
      ${tasksBannerHtml}
      ${modesHtml}
    </div>
  `;

  if (allTasks.length) {
    document
      .getElementById("viewTasksOnRouteBtn")
      ?.addEventListener("click", () => openTasksModal(allTasks));
  }

  journeys.forEach((journey, index) =>
    displayJourneyCard(journey, index + 1, journeys.length)
  );
}

function collectUniqueTasksFromJourneys(journeys) {
  const seen = new Set();
  const uniqueTasks = [];

  for (const journey of journeys) {
    const tasks = Array.isArray(journey?.tasksOnRoute)
      ? journey.tasksOnRoute
      : [];
    for (const task of tasks) {
      const key =
        task?.id || task?.taskId || task?.title || JSON.stringify(task);
      if (!seen.has(key)) {
        seen.add(key);
        uniqueTasks.push(task);
      }
    }
  }

  return uniqueTasks;
}

/**
 * Displays a single journey card (without shared info)
 */
function displayJourneyCard(journey, index, total) {
  if (!resultsDiv) return;

  const departure = journey?.plannedDeparture
    ? formatDateTime(journey.plannedDeparture)
    : "—";
  const arrival = journey?.plannedArrival
    ? formatDateTime(journey.plannedArrival)
    : "—";

  let totalDuration = null;
  if (journey?.plannedDeparture && journey?.plannedArrival) {
    const depTime = new Date(journey.plannedDeparture).getTime();
    const arrTime = new Date(journey.plannedArrival).getTime();
    const durationSeconds = Math.round((arrTime - depTime) / 1000);
    totalDuration = formatDuration(durationSeconds);
  } else {
    const segments = Array.isArray(journey?.segments) ? journey.segments : [];
    const totalSeconds = segments.reduce((sum, seg) => {
      return sum + (seg.durationSeconds || 0);
    }, 0);
    if (totalSeconds > 0) {
      totalDuration = formatDuration(totalSeconds);
    }
  }

  const segments = Array.isArray(journey?.segments) ? journey.segments : [];

  const processedSegments = segments
    .map((seg) => {
      const points = Array.isArray(seg.points) ? seg.points : [];
      const originPoint = points[0];
      const destPoint = points.length > 1 ? points[points.length - 1] : originPoint;

      return {
        ...seg,
        originLabel: originPoint?.name || seg.lineName || "?",
        destinationLabel: destPoint?.name || seg.lineName || "?",
        mode: seg.transitMode || seg.segmentType || "OTHER",
      };
    })
    .filter((seg) => {
      const duration = seg.durationSeconds || 0;
      const samePlace = seg.originLabel === seg.destinationLabel;
      const isTransferOrWalk =
        seg.segmentType === "WALKING" ||
        seg.segmentType === "TRANSFER" ||
        seg.segmentType === "WAITING";

      if (seg.segmentType === "WAITING") return false;
      if (duration < 30 && samePlace) return false;
      if (isTransferOrWalk && duration < 60 && samePlace) return false;
      if (seg.originLabel === "?" && seg.destinationLabel === "?") return false;

      return true;
    });

  const includedTasks = Array.isArray(journey?.includedTasks)
    ? journey.includedTasks
    : [];
  const legItems = processedSegments.map(
    (seg) => `
        <li class="journey-leg-item">
            <div class="leg-marker-container">
                ${formatLineBadge(seg.mode, seg.lineCode, seg.lineColor)}
            </div>
            <div class="leg-content">
                <span class="leg-mode">${formatMode(seg.mode)}${seg.lineName
            ? ` <span class="leg-line-name">${escapeHtml(seg.lineName)}</span>`
            : ""
          }</span>
                <span class="leg-route">${seg.originLabel || "?"} → ${seg.destinationLabel || "?"
          }</span>
                <div class="leg-times">
                    ${seg.scheduledDeparture
            ? formatDateTime(seg.scheduledDeparture)
            : "?"
          } -
                    ${seg.scheduledArrival
            ? formatDateTime(seg.scheduledArrival)
            : "?"
          }
                    <span class="leg-duration">(${seg.durationSeconds
            ? formatDuration(seg.durationSeconds)
            : "?"
          })</span>
                </div>
            </div>
        </li>
    `
  );

  // Insérer la step de tâche au bon endroit dans la chronologie si une tâche est incluse
  if (includedTasks.length > 0) {
    const task = includedTasks[0];
    const taskTitle = task.title || "Task";
    const taskLocation = (task.locationQuery || "").toLowerCase();

    const taskStepHtml = `
      <li class="journey-leg-item task-leg-item">
        <div class="leg-marker-container">
          <span class="task-badge">TASK</span>
        </div>
        <div class="leg-content">
          <span class="leg-mode">Task stop</span>
          <span class="leg-route">${escapeHtml(taskTitle)}${
            taskLocation
              ? ` – ${escapeHtml(task.locationQuery)}`
              : ""
          }</span>
        </div>
      </li>
    `;

    let insertIndex = -1;
    if (taskLocation) {
      for (let i = 0; i < processedSegments.length; i++) {
        const seg = processedSegments[i];
        const o = (seg.originLabel || "").toLowerCase();
        const d = (seg.destinationLabel || "").toLowerCase();
        if (o.includes(taskLocation) || d.includes(taskLocation)) {
          insertIndex = i;
          break;
        }
      }
    }

    if (insertIndex < 0) {
      insertIndex = Math.floor(legItems.length / 2);
    }

    const safeIndex = Math.min(Math.max(insertIndex + 1, 0), legItems.length);
    legItems.splice(safeIndex, 0, taskStepHtml);
  }

  const legsHtml =
    legItems.length > 0
      ? legItems.join("")
      : "<li>No route details available</li>";

  const optionLabel =
    total > 1 ? `<span class="route-option-label">Option ${index}</span> ` : "";

  const totalDurationHtml = totalDuration
    ? ` • Durée: <strong>${totalDuration}</strong>`
    : "";

  let ecoHtml = "";
  if (state.ecoModeEnabled) {
    const totalMeters = (journey.segments || []).reduce(
      (acc, s) => acc + (s.distanceMeters || 0),
      0
    );
    const co2Kg = ((totalMeters / 1000) * 0.2).toFixed(1);
    ecoHtml = `
      <div class="eco-savings-badge">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3">
          <path d="M12 2L4.5 9/5C4.5 12.5 8 16 12 21c4-5 7.5-8.5 7.5-11.5S16 2 12 2z"></path>
        </svg>
        Estimated CO2 Savings: ${co2Kg} kg
      </div>
    `;
  }

  const html = `
    <div class="journey-result">
      <h3>${optionLabel}${escapeHtml(
    journey?.originLabel || "—"
  )} → ${escapeHtml(journey?.destinationLabel || "—")}</h3>
      <p class="journey-meta">Départ: ${departure} • Arrivée: ${arrival}${totalDurationHtml}</p>
      ${ecoHtml}
      <button class="btn btn-primary btn-sm start-journey-btn" data-journey-id="${escapeHtml(
    journey.journeyId
  )}">Start Journey</button>
      <h4>Itinerary Steps:</h4>
      <ul class="journey-legs">
        ${legsHtml}
      </ul>
    </div>
  `;

  resultsDiv.insertAdjacentHTML("beforeend", html);
}

export function prefillJourneyFromSuggestion(task) {
  if (!task || !state.currentUser) return;
  const fromInput = document.getElementById("from");
  const toInput = document.getElementById("to");

  if (fromInput) fromInput.value = state.currentUser.homeAddress || "";
  if (toInput) toInput.value = task.locationQuery || "";
  if (departureInput) departureInput.value = getTomorrowDepartureLocalIso();

  setView("journey");
  journeyForm?.scrollIntoView({ behavior: "smooth", block: "start" });
  showToast("Journey form prefilled.", { variant: "info" });
}

export function setDefaultDepartureTime() {
  if (!departureInput) return;
  const d = new Date(Date.now() + 60 * 60 * 1000);
  d.setMinutes(d.getMinutes() - d.getTimezoneOffset());
  departureInput.value = d.toISOString().slice(0, 16);
}

// --- Home Suggestions ---

let currentHomePopover = null;

function showHomeSuggestion(inputEl) {
  if (!state.currentUser?.homeAddress) return;

  hideHomeSuggestion();

  const homeAddress = state.currentUser.homeAddress;
  const popover = document.createElement("div");
  popover.className = "home-suggestion-popover";
  popover.innerHTML = `
      <div class="home-suggestion-item">
          <span class="home-suggestion-icon">🏠</span>
          <div class="home-suggestion-details">
              <span class="home-suggestion-title">Use Home</span>
              <span class="home-suggestion-address">${escapeHtml(
    homeAddress
  )}</span>
          </div>
      </div>
  `;

  popover.querySelector(".home-suggestion-item").addEventListener("click", () => {
    inputEl.value = homeAddress;
    hideHomeSuggestion();
  });

  inputEl.parentNode.style.position = "relative";
  inputEl.parentNode.appendChild(popover);
  currentHomePopover = popover;
}

function hideHomeSuggestion() {
  if (currentHomePopover) {
    currentHomePopover.remove();
    currentHomePopover = null;
  }
}
