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
import { notifyTasksOnRouteIfAny } from "./task-notifications.js";
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

export function setupJourneyForm() {
  journeyForm?.addEventListener("submit", handleJourneySubmit);
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
    updateCurrentJourney(journey);
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
  try {
    const journey = await api.post(
      `/api/journeys/${state.currentJourney.journeyId}/complete`
    );
    updateCurrentJourney(journey);
    showToast("Journey completed!", { variant: "success" });
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

export function updateCurrentJourney(journey) {
  state.currentJourney = journey;

  if (
    journey &&
    (journey.status === "PLANNED" ||
      journey.status === "IN_PROGRESS" ||
      journey.status === "REROUTED")
  ) {
    currentJourneyPanel?.classList.remove("hidden");
    renderCurrentJourney(journey);

    document.querySelector(".results-panel")?.classList.add("hidden");
  } else {
    currentJourneyPanel?.classList.add("hidden");
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
  const showSteps =
    (journey.status === "IN_PROGRESS" || journey.status === "REROUTED") &&
    (hasSegments || hasTasks);

  const segments = Array.isArray(journey?.segments) ? journey.segments : [];
  const includedTasks = Array.isArray(journey?.includedTasks)
    ? journey.includedTasks
    : [];
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
      if (seg.segmentType === "WAITING") return false;
      if (duration < 30 && samePlace) return false;
      return true;
    });

  currentJourneyContent.innerHTML = `
        <div class="journey-status-card">
            <div class="status-badge ${statusClass}">${journey.status}</div>
            ${
              journey.status === "REROUTED" || journey.disruptionCount > 0
                ? '<div class="disruption-warning">⚠️ Disruption : New Journey Started</div>'
                : ""
            }
            <h3>${journey.originLabel} → ${journey.destinationLabel}</h3>
            
            ${
              journey.status === "IN_PROGRESS" || journey.status === "REROUTED"
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
            ${
              journey.actualDeparture
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

  if (!departure) {
    if (resultsDiv)
      resultsDiv.innerHTML =
        '<p class="error-message">Please select a departure time.</p>';
    return;
  }

  if (comfortSelection !== "disabled" && !validateComfortMode()) {
    return;
  }

  const payload = {
    journey: {
      userId: state.currentUser.userId,
      originQuery: from,
      destinationQuery: to,
      departureTime: departure,
    },
    preferences: {
      comfortMode: comfortSelection !== "disabled",
      namedComfortSettingId:
        comfortSelection !== "disabled" ? comfortSelection : null,
    },
  };

  if (resultsDiv)
    resultsDiv.innerHTML = '<p class="loading">Planning your journey...</p>';

  try {
    const journeys = await api.post("/api/journeys", payload);

    if (resultsDiv) resultsDiv.innerHTML = "";
    const list = Array.isArray(journeys) ? journeys : [journeys];

    if (list.length === 0) {
      if (resultsDiv)
        resultsDiv.innerHTML = '<p class="error-message">No journey found.</p>';
      return;
    }

    displayJourneyResults(list);
    if (list.length > 0) notifyTasksOnRouteIfAny(list[0]);
  } catch (err) {
    if (resultsDiv)
      resultsDiv.innerHTML = '<p class="error-message">No journey found.</p>';
  }
}

/**
 * Displays journey results with shared info (tasks, modes) shown once at top
 */
export function displayJourneyResults(journeys) {
  if (!resultsDiv || !journeys.length) return;

  const firstJourney = journeys[0];
  const allTasks = collectUniqueTasksFromJourneys(journeys);

  const tasksBannerHtml = allTasks.length
    ? `
      <div class="tasks-on-route-banner">
        <div>
          <div class="tasks-on-route-title">${allTasks.length} task${
        allTasks.length > 1 ? "s" : ""
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
      <span>Comfort Profile: ${
        firstJourney?.comfortModeEnabled ? "Active" : "None"
      }</span>
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

/**
 * Collects unique tasks from all journeys (deduped by ID or title)
 */
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

  const legsHtml = processedSegments.length
    ? processedSegments
        .map(
          (seg) => `
        <li class="journey-leg-item">
            <div class="leg-marker-container">
                ${formatLineBadge(seg.mode, seg.lineCode, seg.lineColor)}
            </div>
            <div class="leg-content">
                <span class="leg-mode">${formatMode(seg.mode)}${
            seg.lineName
              ? ` <span class="leg-line-name">${escapeHtml(seg.lineName)}</span>`
              : ""
          }</span>
                <span class="leg-route">${seg.originLabel || "?"} → ${
            seg.destinationLabel || "?"
          }</span>
                <div class="leg-times">
                    ${
                      seg.scheduledDeparture
                        ? formatDateTime(seg.scheduledDeparture)
                        : "?"
                    } -
                    ${
                      seg.scheduledArrival
                        ? formatDateTime(seg.scheduledArrival)
                        : "?"
                    }
                    <span class="leg-duration">(${
                      seg.durationSeconds
                        ? formatDuration(seg.durationSeconds)
                        : "?"
                    })</span>
                </div>
            </div>
        </li>
    `
        )
        .join("")
    : "<li>No route details available</li>";

  const optionLabel =
    total > 1 ? `<span class="route-option-label">Option ${index}</span> ` : "";

  const totalDurationHtml = totalDuration
    ? ` • Durée: <strong>${totalDuration}</strong>`
    : "";

  const html = `
    <div class="journey-result">
      <h3>${optionLabel}${escapeHtml(
    journey?.originLabel || "—"
  )} → ${escapeHtml(journey?.destinationLabel || "—")}</h3>
      <p class="journey-meta">Départ: ${departure} • Arrivée: ${arrival}${totalDurationHtml}</p>
      <button class="btn btn-primary btn-sm start-journey-btn" data-journey-id="${escapeHtml(
        journey.journeyId
      )}">Start Journey</button>
      <h4>Itinerary Steps:</h4>
      <ul class="journey-legs">${legsHtml}</ul>
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
