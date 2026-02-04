import { api } from "../core/api.js";
import { state } from "../core/state.js";
import { escapeHtml, formatMode } from "../core/utils.js";
import { showToast } from "../ui/toast.js";
import { displayJourneyResults } from "./journey.js";

const overlayEl = document.getElementById("disruptionModal");
const contentEl = document.getElementById("disruptionModalContent");
const titleEl = document.getElementById("disruptionModalTitle");

export function setupDisruptionModal() {
  if (!overlayEl) return;

  overlayEl.addEventListener("click", (e) => {
    if (e.target === overlayEl) closeDisruptionModal();
  });

  document
    .getElementById("disruptionModalClose")
    ?.addEventListener("click", closeDisruptionModal);
}

export function openDisruptionModal() {
  if (!overlayEl || !contentEl) return;

  titleEl.textContent = "Report Disruption";
  contentEl.innerHTML = `
    <p class="disruption-modal-subtitle">What type of disruption are you experiencing?</p>
    <div class="disruption-choice-buttons">
      <button type="button" class="btn btn-primary disruption-choice-btn" id="chooseLineDisruption">
        <span class="disruption-choice-icon">🚇</span>
        <span class="disruption-choice-label">Line Disruption</span>
        <span class="disruption-choice-desc">A metro/bus/train line is disrupted</span>
      </button>
      <button type="button" class="btn btn-primary disruption-choice-btn" id="chooseStationDisruption">
        <span class="disruption-choice-icon">🚉</span>
        <span class="disruption-choice-label">Station Disruption</span>
        <span class="disruption-choice-desc">A specific station is closed or inaccessible</span>
      </button>
    </div>
  `;

  document
    .getElementById("chooseLineDisruption")
    ?.addEventListener("click", showLineSelection);
  document
    .getElementById("chooseStationDisruption")
    ?.addEventListener("click", showStationSelection);

  overlayEl.classList.remove("hidden");
}

function closeDisruptionModal() {
  if (overlayEl) overlayEl.classList.add("hidden");
}

async function showLineSelection() {
  if (!contentEl || !state.currentJourney) return;

  titleEl.textContent = "Select Disrupted Line";
  contentEl.innerHTML = '<p class="loading">Loading lines...</p>';

  try {
    const lines = await api.get(
      `/api/journeys/${state.currentJourney.journeyId}/lines`
    );

    if (!lines || lines.length === 0) {
      contentEl.innerHTML = `
        <p class="disruption-modal-subtitle">No transit lines found in your journey.</p>
        <button type="button" class="btn btn-outline" id="backToDisruptionChoice">Back</button>
      `;
      document
        .getElementById("backToDisruptionChoice")
        ?.addEventListener("click", openDisruptionModal);
      return;
    }

    const linesHtml = lines
      .map(
        (line) => `
      <button type="button" class="disruption-line-btn" data-line-code="${escapeHtml(
        line.lineCode
      )}">
        <span class="line-color-badge" style="background-color: ${
          line.lineColor ? "#" + line.lineColor : "#666"
        }"></span>
        <span class="line-info">
          <span class="line-code">${escapeHtml(line.lineCode || "Unknown")}</span>
          <span class="line-name">${escapeHtml(line.lineName || "")}</span>
        </span>
        <span class="line-mode">${formatMode(line.mode)}</span>
      </button>
    `
      )
      .join("");

    contentEl.innerHTML = `
      <p class="disruption-modal-subtitle">Select the line that is disrupted:</p>
      <div class="disruption-lines-list">${linesHtml}</div>
      <button type="button" class="btn btn-outline btn-sm" id="backToDisruptionChoice">Back</button>
    `;

    contentEl.querySelectorAll(".disruption-line-btn").forEach((btn) => {
      btn.addEventListener("click", () => {
        const lineCode = btn.getAttribute("data-line-code");
        if (lineCode) reportLineDisruption(lineCode);
      });
    });

    document
      .getElementById("backToDisruptionChoice")
      ?.addEventListener("click", openDisruptionModal);
  } catch (err) {
    contentEl.innerHTML = `
      <p class="error-message">Failed to load lines: ${escapeHtml(
        err.message || "Unknown error"
      )}</p>
      <button type="button" class="btn btn-outline" id="backToDisruptionChoice">Back</button>
    `;
    document
      .getElementById("backToDisruptionChoice")
      ?.addEventListener("click", openDisruptionModal);
  }
}

async function showStationSelection() {
  if (!contentEl || !state.currentJourney) return;

  titleEl.textContent = "Select Disrupted Station";
  contentEl.innerHTML = '<p class="loading">Loading stations...</p>';

  try {
    const stops = await api.get(
      `/api/journeys/${state.currentJourney.journeyId}/stops`
    );

    if (!stops || stops.length === 0) {
      contentEl.innerHTML = `
        <p class="disruption-modal-subtitle">No stations found in your journey.</p>
        <button type="button" class="btn btn-outline" id="backToDisruptionChoice">Back</button>
      `;
      document
        .getElementById("backToDisruptionChoice")
        ?.addEventListener("click", openDisruptionModal);
      return;
    }

    const stopsHtml = stops
      .map(
        (stop, index) => `
      <button type="button" class="disruption-station-btn" data-stop-point-id="${escapeHtml(
        stop.stopPointId
      )}">
        <span class="station-sequence">${index + 1}</span>
        <span class="station-info">
          <span class="station-name">${escapeHtml(
            stop.name || "Unknown station"
          )}</span>
          ${
            stop.onLineCode
              ? `<span class="station-line">Line ${escapeHtml(
                  stop.onLineCode
                )}</span>`
              : ""
          }
        </span>
      </button>
    `
      )
      .join("");

    contentEl.innerHTML = `
      <p class="disruption-modal-subtitle">Select the station that is disrupted:</p>
      <div class="disruption-stations-list">${stopsHtml}</div>
      <button type="button" class="btn btn-outline btn-sm" id="backToDisruptionChoice">Back</button>
    `;

    contentEl.querySelectorAll(".disruption-station-btn").forEach((btn) => {
      btn.addEventListener("click", () => {
        const stopPointId = btn.getAttribute("data-stop-point-id");
        if (stopPointId) reportStationDisruption(stopPointId);
      });
    });

    document
      .getElementById("backToDisruptionChoice")
      ?.addEventListener("click", openDisruptionModal);
  } catch (err) {
    contentEl.innerHTML = `
      <p class="error-message">Failed to load stations: ${escapeHtml(
        err.message || "Unknown error"
      )}</p>
      <button type="button" class="btn btn-outline" id="backToDisruptionChoice">Back</button>
    `;
    document
      .getElementById("backToDisruptionChoice")
      ?.addEventListener("click", openDisruptionModal);
  }
}

async function reportLineDisruption(lineCode) {
  if (!contentEl || !state.currentJourney) return;

  contentEl.innerHTML =
    '<p class="loading">Reporting disruption and finding alternatives...</p>';

  try {
    const result = await api.post(
      `/api/journeys/${state.currentJourney.journeyId}/disruptions/line`,
      {
        lineCode: lineCode,
      }
    );

    closeDisruptionModal();
    handleRerouteResult(result, `Line ${lineCode}`);
  } catch (err) {
    contentEl.innerHTML = `
      <p class="error-message">Failed to report disruption: ${escapeHtml(
        err.message || "Unknown error"
      )}</p>
      <button type="button" class="btn btn-outline" id="backToDisruptionChoice">Back</button>
    `;
    document
      .getElementById("backToDisruptionChoice")
      ?.addEventListener("click", openDisruptionModal);
  }
}

async function reportStationDisruption(stopPointId) {
  if (!contentEl || !state.currentJourney) return;

  contentEl.innerHTML =
    '<p class="loading">Reporting disruption and finding alternatives...</p>';

  try {
    const result = await api.post(
      `/api/journeys/${state.currentJourney.journeyId}/disruptions/station`,
      {
        stopPointId: stopPointId,
      }
    );

    closeDisruptionModal();
    handleRerouteResult(result, result.disruptedPoint?.name || "the station");
  } catch (err) {
    contentEl.innerHTML = `
      <p class="error-message">Failed to report disruption: ${escapeHtml(
        err.message || "Unknown error"
      )}</p>
      <button type="button" class="btn btn-outline" id="backToDisruptionChoice">Back</button>
    `;
    document
      .getElementById("backToDisruptionChoice")
      ?.addEventListener("click", openDisruptionModal);
  }
}

function handleRerouteResult(result, disruptionDescription) {
  document.getElementById("currentJourneyPanel")?.classList.add("hidden");
  document.querySelector(".results-panel")?.classList.remove("hidden");

  const resultsDiv = document.getElementById("results");
  const alternatives = result.alternatives || [];

  if (alternatives.length > 0) {
    if (resultsDiv) {
      resultsDiv.innerHTML = `
      <div class="reroute-header">
        <div class="reroute-warning">
          <span class="reroute-icon">⚠️</span>
          <div class="reroute-info">
            <strong>Disruption reported on ${escapeHtml(
              disruptionDescription
            )}</strong>
            ${
              result.newOrigin
                ? `<p>Rerouting from: ${escapeHtml(result.newOrigin.name)}</p>`
                : ""
            }
          </div>
        </div>
      </div>
    `;
    }
    displayJourneyResults(alternatives);
    showToast("Disruption reported. Choose an alternative route below.", {
      variant: "warning",
      durationMs: 6000,
    });
  } else {
    if (resultsDiv) {
      resultsDiv.innerHTML = `
      <div class="reroute-header">
        <div class="reroute-warning">
          <span class="reroute-icon">⚠️</span>
          <div class="reroute-info">
            <strong>Disruption reported on ${escapeHtml(
              disruptionDescription
            )}</strong>
            <p>No alternative routes could be found.</p>
          </div>
        </div>
      </div>
    `;
    }
    showToast("Disruption reported, but no alternative routes found.", {
      variant: "warning",
    });
  }

  state.currentJourney = null;
}
