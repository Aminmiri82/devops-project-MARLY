// ============================================================
// CONFIGURATION
// ============================================================
const CONFIG = {
  TOAST_DURATION_MS: 4500,
  TOAST_IMPORTANT_DURATION_MS: 12000,
  POPUP_CHECK_INTERVAL_MS: 1200,
  POPUP_DIMENSIONS: "width=600,height=700",
  TOAST_HIDE_DELAY_MS: 200,
};

// ============================================================
// API CLIENT
// ============================================================
const api = {
  async get(url) {
    return this.request(url, "GET");
  },
  async post(url, body) {
    return this.request(url, "POST", body);
  },
  async put(url, body) {
    return this.request(url, "PUT", body);
  },
  async patch(url, body) {
    return this.request(url, "PATCH", body);
  },
  async delete(url) {
    return this.request(url, "DELETE");
  },
  async request(url, method, body) {
    const opts = { method, headers: {} };
    if (body !== undefined) {
      opts.headers["Content-Type"] = "application/json";
      opts.body = JSON.stringify(body);
    }
    const resp = await fetch(url, opts);
    if (!resp.ok) {
      if (resp.status === 401 || resp.status === 403 || resp.status === 409) {
        const error = new Error("Google Tasks not authorized");
        error.authError = true;
        throw error;
      }
      const text = await resp.text();
      throw new Error(text || `Request failed: ${resp.status}`);
    }
    const contentType = resp.headers.get("content-type");
    if (contentType?.includes("application/json")) {
      return resp.json();
    }
    return resp.text();
  },
};

// ============================================================
// STATE MANAGEMENT
// ============================================================
let currentUser = null;
let currentView = localStorage.getItem("mavigo_view") || "journey";
let defaultTaskList = null;
let currentJourney = null;

// ============================================================
// DOM ELEMENTS
// ============================================================
const authModal = document.getElementById("authModal");
const loginFormEl = document.getElementById("loginFormEl");
const registerFormEl = document.getElementById("registerFormEl");
const loginFormView = document.getElementById("loginForm");
const registerFormView = document.getElementById("registerForm");
const loggedOutView = document.getElementById("loggedOutView");
const loggedInView = document.getElementById("loggedInView");
const userGreeting = document.getElementById("userGreeting");
const notLoggedInPrompt = document.getElementById("notLoggedInPrompt");
const mainContent = document.getElementById("mainContent");

const journeyForm = document.getElementById("journeyForm");
const resultsDiv = document.getElementById("results");
const currentJourneyPanel = document.getElementById("currentJourneyPanel");
const currentJourneyContent = document.getElementById("currentJourneyContent");
const completeJourneyBtn = document.getElementById("completeJourneyBtn");
const cancelJourneyBtn = document.getElementById("cancelJourneyBtn");
const departureInput = document.getElementById("departure");
const reportDisruptionBtn = document.getElementById("reportDisruptionBtn");
const fetchShortDisruptionsBtn = document.getElementById(
  "fetchShortDisruptionsBtn"
);

const navJourneyBtn = document.getElementById("navJourneyBtn");
const navTasksBtn = document.getElementById("navTasksBtn");
const journeyView = document.getElementById("journeyView");
const tasksView = document.getElementById("tasksView");

const tasksIncludeCompleted = document.getElementById("tasksIncludeCompleted");
const tasksResults = document.getElementById("tasksResults");
const tasksListName = document.getElementById("tasksListName");
const refreshTasksBtn = document.getElementById("refreshTasksBtn");

const createTaskForm = document.getElementById("createTaskForm");
const taskTitle = document.getElementById("taskTitle");
const taskNotes = document.getElementById("taskNotes");
const taskDue = document.getElementById("taskDue");
const taskLocationQuery = document.getElementById("taskLocationQuery");

const comfortProfileModal = document.getElementById("comfortProfileModal");
const comfortProfileForm = document.getElementById("comfortProfileForm");
const comfortProfileSummary = document.getElementById("comfortProfileSummary");
const editComfortProfileBtn = document.getElementById("editComfortProfileBtn");
const closeComfortProfileModal = document.getElementById(
  "closeComfortProfileModal"
);
const clearComfortProfileBtn = document.getElementById(
  "clearComfortProfileBtn"
);
const comfortCheckbox = document.getElementById("comfort");

// User Dropdown Elements
const userDropdownTrigger = document.getElementById("userDropdownTrigger");
const userDropdown = document.getElementById("userDropdown");
const dropdownUserName = document.getElementById("dropdownUserName");
const dropdownUserEmail = document.getElementById("dropdownUserEmail");
const googleLinkStatusBadge = document.getElementById("googleLinkStatusBadge");

// ============================================================
// INITIALIZATION
// ============================================================
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
  setDefaultDepartureTime();
  ensureToastUI();
  restoreSession();
}

// ============================================================
// NAVIGATION
// ============================================================
function setupNav() {
  navJourneyBtn?.addEventListener("click", () => setView("journey"));
  navTasksBtn?.addEventListener("click", () => setView("tasks"));
  setView(currentView);
}

function setView(view) {
  currentView = view === "tasks" ? "tasks" : "journey";
  localStorage.setItem("mavigo_view", currentView);

  if (currentView === "journey") {
    journeyView?.classList.remove("hidden");
    tasksView?.classList.add("hidden");
    navJourneyBtn?.classList.add("nav-active");
    navTasksBtn?.classList.remove("nav-active");
  } else {
    journeyView?.classList.add("hidden");
    tasksView?.classList.remove("hidden");
    navJourneyBtn?.classList.remove("nav-active");
    navTasksBtn?.classList.add("nav-active");
  }

  updateTasksUIState();
  if (currentView === "tasks") ensureDefaultTaskListLoaded({ force: false });
}

// ============================================================
// USER DROPDOWN
// ============================================================
function setupDropdown() {
  if (!userDropdownTrigger || !userDropdown) return;

  userDropdownTrigger.addEventListener("click", (e) => {
    e.stopPropagation();
    toggleDropdown();
  });

  // Close dropdown when clicking outside
  document.addEventListener("click", (e) => {
    if (!userDropdown.classList.contains("open")) return;
    if (!userDropdown.contains(e.target) && e.target !== userDropdownTrigger) {
      closeDropdown();
    }
  });

  // Close dropdown on Escape key
  document.addEventListener("keydown", (e) => {
    if (e.key === "Escape" && userDropdown.classList.contains("open")) {
      closeDropdown();
    }
  });
}

function toggleDropdown() {
  const isOpen = userDropdown.classList.contains("open");
  if (isOpen) {
    closeDropdown();
  } else {
    openDropdown();
  }
}

function openDropdown() {
  userDropdown.classList.add("open");
  userDropdownTrigger.classList.add("open");
}

function closeDropdown() {
  userDropdown.classList.remove("open");
  userDropdownTrigger.classList.remove("open");
}

// ============================================================
// AUTHENTICATION
// ============================================================
function setupAuthListeners() {
  document
    .getElementById("showLoginBtn")
    ?.addEventListener("click", () => openAuthModal("login"));
  document
    .getElementById("showRegisterBtn")
    ?.addEventListener("click", () => openAuthModal("register"));
  document
    .getElementById("promptLoginBtn")
    ?.addEventListener("click", () => openAuthModal("login"));
  document
    .getElementById("closeAuthModal")
    ?.addEventListener("click", closeAuthModal);

  document
    .getElementById("switchToRegister")
    ?.addEventListener("click", (e) => {
      e.preventDefault();
      showAuthForm("register");
    });

  document.getElementById("switchToLogin")?.addEventListener("click", (e) => {
    e.preventDefault();
    showAuthForm("login");
  });

  document.getElementById("logoutBtn")?.addEventListener("click", logout);

  authModal?.addEventListener("click", (e) => {
    if (e.target === authModal) closeAuthModal();
  });

  loginFormEl?.addEventListener("submit", handleLogin);
  registerFormEl?.addEventListener("submit", handleRegister);
}

function openAuthModal(formType) {
  if (!authModal) return;
  authModal.classList.remove("hidden");
  showAuthForm(formType);
}

function closeAuthModal() {
  if (!authModal) return;
  authModal.classList.add("hidden");
  clearAuthErrors();
}

function showAuthForm(type) {
  clearAuthErrors();
  if (!loginFormView || !registerFormView) return;

  if (type === "login") {
    loginFormView.classList.remove("hidden");
    registerFormView.classList.add("hidden");
  } else {
    loginFormView.classList.add("hidden");
    registerFormView.classList.remove("hidden");
  }
}

function clearAuthErrors() {
  document.getElementById("loginError")?.classList.add("hidden");
  document.getElementById("registerError")?.classList.add("hidden");
}

async function handleLogin(e) {
  e.preventDefault();

  const email = (document.getElementById("loginEmail")?.value || "").trim();
  const errorEl = document.getElementById("loginError");

  if (!email) return showError(errorEl, "Please enter your email.");

  try {
    const user = await api.post("/api/users/login", { email });
    setCurrentUser(user, { preferredView: "tasks" });
    closeAuthModal();
    loginFormEl?.reset();
  } catch (err) {
    showError(errorEl, err?.message || "Login failed");
  }
}

async function handleRegister(e) {
  e.preventDefault();

  const name = (document.getElementById("registerName")?.value || "").trim();
  const email = (document.getElementById("registerEmail")?.value || "").trim();
  const errorEl = document.getElementById("registerError");

  if (!name || !email) return showError(errorEl, "Please fill in all fields.");

  const payload = { displayName: name, email, externalId: generateId() };

  try {
    const user = await api.post("/api/users", payload);
    setCurrentUser(user, { preferredView: "tasks" });
    closeAuthModal();
    registerFormEl?.reset();
  } catch (err) {
    showError(errorEl, err?.message || "Registration failed");
  }
}

function logout() {
  currentUser = null;
  defaultTaskList = null;
  localStorage.removeItem("mavigo_user_id");
  clearGoogleLinkStatus();
  resetTasksUI();
  updateUI();
}

function setCurrentUser(user, opts = {}) {
  currentUser = user;
  defaultTaskList = null;
  if (user?.userId) localStorage.setItem("mavigo_user_id", user.userId);
  if (opts.preferredView) setView(opts.preferredView);
  updateUI();
}

async function restoreSession() {
  const savedUserId = localStorage.getItem("mavigo_user_id");
  if (!savedUserId) return updateUI();

  try {
    const user = await api.get(`/api/users/${savedUserId}`);
    currentUser = user;
    updateUI();
    if (currentView === "tasks") ensureDefaultTaskListLoaded({ force: false });
  } catch {
    localStorage.removeItem("mavigo_user_id");
    currentUser = null;
    defaultTaskList = null;
    updateUI();
  }
}

function updateUI() {
  if (currentUser) {
    loggedOutView?.classList.add("hidden");
    loggedInView?.classList.remove("hidden");
    if (userGreeting)
      userGreeting.textContent = `Hi, ${currentUser.displayName || "User"}`;
    notLoggedInPrompt?.classList.add("hidden");
    mainContent?.classList.remove("hidden");
    renderUserInfo();
    renderGoogleLinkStatus(currentUser);
    renderComfortProfileSummary();
    setView(currentView);
  } else {
    loggedOutView?.classList.remove("hidden");
    loggedInView?.classList.add("hidden");
    notLoggedInPrompt?.classList.remove("hidden");
    mainContent?.classList.add("hidden");
    clearGoogleLinkStatus();
    resetTasksUI();
  }
}

function renderUserInfo() {
  // Update dropdown user info
  if (dropdownUserName) {
    dropdownUserName.textContent = currentUser?.displayName || "—";
  }
  if (dropdownUserEmail) {
    dropdownUserEmail.textContent = currentUser?.email || "—";
  }
}

function showError(el, message) {
  if (!el) return;
  el.textContent = message;
  el.classList.remove("hidden");
}

// ============================================================
// JOURNEY MANAGEMENT
// ============================================================
function setupJourneyForm() {
  journeyForm?.addEventListener("submit", handleJourneySubmit);
}

function setupJourneyActions() {
  completeJourneyBtn?.addEventListener("click", completeJourney);
  cancelJourneyBtn?.addEventListener("click", cancelJourney);
  reportDisruptionBtn?.addEventListener("click", reportDisruption);
}

async function startJourney(journeyId, btnElement) {
  if (!currentUser) return;
  const allButtons = document.querySelectorAll(".start-journey-btn");
  if (btnElement) {
    btnElement.disabled = true;
    btnElement.textContent = "Démarrage…";
  }
  allButtons.forEach((btn) => { if (btn !== btnElement) btn.classList.add("hidden"); });

  try {
    const journey = await api.post(`/api/journeys/${journeyId}/start`);
    const stored = lastDisplayedJourneysById?.get(journeyId);
    const merged = stored && (stored.includedTasks?.length || stored.segments)
      ? { ...journey, includedTasks: journey.includedTasks ?? stored.includedTasks, segments: journey.segments ?? stored.segments }
      : journey;
    updateCurrentJourney(merged);
  } catch (err) {
    showToast(err?.message || "Erreur", { variant: "warning" });
    allButtons.forEach((btn) => {
      btn.classList.remove("hidden");
      btn.disabled = false;
      if (btn === btnElement) btn.textContent = "Démarrer le trajet";
    });
  }
}

async function completeJourney() {
  if (!currentJourney) return;
  const includedTasks = Array.isArray(currentJourney.includedTasks) ? currentJourney.includedTasks : [];
  const googleTaskIds = includedTasks.map((t) => t.googleTaskId).filter(Boolean);

  try {
    const journey = await api.post(`/api/journeys/${currentJourney.journeyId}/complete`);
    if (googleTaskIds.length && currentUser) {
      try {
        const list = await api.get(`/api/google/tasks/users/${currentUser.userId}/default-list`);
        const listId = list?.id;
        if (listId) {
          for (const taskId of googleTaskIds) {
            await api.patch(
              `/api/google/tasks/users/${currentUser.userId}/lists/${encodeURIComponent(listId)}/tasks/${encodeURIComponent(taskId)}/complete`
            );
          }
        }
      } catch (e) {
        console.warn("Could not mark Google task(s) complete:", e);
      }
    }
    updateCurrentJourney(journey);
    showToast("Trajet terminé.", { variant: "success" });
  } catch (err) {
    showToast(err?.message || "Erreur", { variant: "warning" });
  }
}

async function cancelJourney() {
  if (!currentJourney) return;
  if (!confirm("Annuler ce trajet ?")) return;

  try {
    const journey = await api.post(
      `/api/journeys/${currentJourney.journeyId}/cancel`
    );
    updateCurrentJourney(journey);
  } catch (err) {
    showToast(err.message, { variant: "warning" });
  }
}

function updateCurrentJourney(journey) {
  currentJourney = journey;

  if (
    journey &&
    (journey.status === "PLANNED" ||
      journey.status === "IN_PROGRESS" ||
      journey.status === "REROUTED")
  ) {
    currentJourneyPanel.classList.remove("hidden");
    renderCurrentJourney(journey);
    document.querySelector(".results-panel")?.classList.add("hidden");
  } else {
    currentJourneyPanel.classList.add("hidden");
    document.querySelector(".results-panel")?.classList.remove("hidden");
    if (resultsDiv)
      resultsDiv.innerHTML = "<p class=\"results-placeholder\">Vos résultats de trajet s'afficheront ici.</p>";
    currentJourney = null;
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
  const statusClass =
    journey.status === "IN_PROGRESS" ? "status-active" : "status-planned";
  const progress = calculateProgress(journey);
  const hasSegments = Array.isArray(journey.segments) && journey.segments.length > 0;
  const hasTasks = Array.isArray(journey.includedTasks) && journey.includedTasks.length > 0;
  const showSteps = (journey.status === "IN_PROGRESS" || journey.status === "REROUTED") && (hasSegments || hasTasks);

  // Process segments for display
  const segments = Array.isArray(journey?.segments) ? journey.segments : [];
  const includedTasks = Array.isArray(journey?.includedTasks) ? journey.includedTasks : [];
  const processedSegments = segments
    .map((seg) => {
      const points = Array.isArray(seg.points) ? seg.points : [];
      const originPoint = points[0];
      const destPoint = points.length > 1 ? points[points.length - 1] : originPoint;
      return {
        ...seg,
        originLabel: originPoint?.name || seg.lineName || "?",
        destinationLabel: destPoint?.name || seg.lineName || "?",
        mode: seg.transitMode || seg.segmentType || "OTHER"
      };
    })
    .filter((seg) => {
      const duration = seg.durationSeconds || 0;
      const samePlace = seg.originLabel === seg.destinationLabel;
      if (seg.segmentType === "WAITING") return false;
      if (duration < 30 && samePlace) return false;
      return true;
    });
  const stepsHtml = showSteps ? buildItineraryStepsHtmlFromSegments(processedSegments, includedTasks) : "";

  currentJourneyContent.innerHTML = `
        <div class="journey-status-card">
            <div class="status-badge ${statusClass}">${journey.status === "IN_PROGRESS" ? "En cours" : journey.status}</div>
            ${
              journey.status === "REROUTED" || journey.disruptionCount > 0
                ? '<div class="disruption-warning">⚠️ Perturbation : nouvel itinéraire</div>'
                : ""
            }
            <h3>${escapeHtml(journey.originLabel || "—")} → ${escapeHtml(journey.destinationLabel || "—")}</h3>
            ${
              journey.status === "IN_PROGRESS" || journey.status === "REROUTED"
                ? `
                <div class="progress-container">
                    <div class="progress-bar" style="width: ${progress}%"></div>
                </div>
                <span class="progress-text">${progress}% effectué</span>
            `
                : ""
            }
            <p><strong>Départ prévu :</strong> ${formatDateTime(journey.plannedDeparture)}</p>
            ${journey.actualDeparture ? `<p><strong>Démarré à :</strong> ${formatDateTime(journey.actualDeparture)}</p>` : ""}
            <p><strong>Arrivée prévue :</strong> ${formatDateTime(journey.plannedArrival)}</p>
        </div>
        ${showSteps ? `
        <div class="current-journey-itinerary">
            <h4 class="current-journey-itinerary-title">Étapes de l'itinéraire</h4>
            <ul class="journey-legs">${stepsHtml}</ul>
        </div>
        ` : ""}
    `;

  if (journey.status === "IN_PROGRESS" || journey.status === "REROUTED") {
    completeJourneyBtn.classList.remove("hidden");
    cancelJourneyBtn.classList.remove("hidden");
    if (reportDisruptionBtn) reportDisruptionBtn.classList.remove("hidden");
  } else {
    completeJourneyBtn.classList.add("hidden");
    cancelJourneyBtn.classList.add("hidden");
    if (reportDisruptionBtn) reportDisruptionBtn.classList.add("hidden");
  }
}

async function reportDisruption() {
  if (!currentJourney) return;
  openDisruptionModal();
}

// ============================================================
// DISRUPTION REPORTING MODAL
// ============================================================
function setupDisruptionModal() {
  const overlay = document.getElementById("disruptionModal");
  if (!overlay) return;

  overlay.addEventListener("click", (e) => {
    if (e.target === overlay) closeDisruptionModal();
  });

  document.getElementById("disruptionModalClose")?.addEventListener("click", closeDisruptionModal);
}

function openDisruptionModal() {
  const overlay = document.getElementById("disruptionModal");
  const content = document.getElementById("disruptionModalContent");
  const title = document.getElementById("disruptionModalTitle");

  if (!overlay || !content) return;

  title.textContent = "Report Disruption";
  content.innerHTML = `
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

  document.getElementById("chooseLineDisruption")?.addEventListener("click", showLineSelection);
  document.getElementById("chooseStationDisruption")?.addEventListener("click", showStationSelection);

  overlay.classList.remove("hidden");
}

function closeDisruptionModal() {
  const overlay = document.getElementById("disruptionModal");
  if (overlay) overlay.classList.add("hidden");
}

async function showLineSelection() {
  const content = document.getElementById("disruptionModalContent");
  const title = document.getElementById("disruptionModalTitle");
  if (!content || !currentJourney) return;

  title.textContent = "Select Disrupted Line";
  content.innerHTML = '<p class="loading">Loading lines...</p>';

  try {
    const lines = await api.get(`/api/journeys/${currentJourney.journeyId}/lines`);

    if (!lines || lines.length === 0) {
      content.innerHTML = `
        <p class="disruption-modal-subtitle">No transit lines found in your journey.</p>
        <button type="button" class="btn btn-outline" id="backToDisruptionChoice">Back</button>
      `;
      document.getElementById("backToDisruptionChoice")?.addEventListener("click", openDisruptionModal);
      return;
    }

    const linesHtml = lines.map(line => `
      <button type="button" class="disruption-line-btn" data-line-code="${escapeHtml(line.lineCode)}">
        <span class="line-color-badge" style="background-color: ${line.lineColor ? '#' + line.lineColor : '#666'}"></span>
        <span class="line-info">
          <span class="line-code">${escapeHtml(line.lineCode || 'Unknown')}</span>
          <span class="line-name">${escapeHtml(line.lineName || '')}</span>
        </span>
        <span class="line-mode">${formatMode(line.mode)}</span>
      </button>
    `).join('');

    content.innerHTML = `
      <p class="disruption-modal-subtitle">Select the line that is disrupted:</p>
      <div class="disruption-lines-list">${linesHtml}</div>
      <button type="button" class="btn btn-outline btn-sm" id="backToDisruptionChoice">Back</button>
    `;

    content.querySelectorAll(".disruption-line-btn").forEach(btn => {
      btn.addEventListener("click", () => {
        const lineCode = btn.getAttribute("data-line-code");
        if (lineCode) reportLineDisruption(lineCode);
      });
    });

    document.getElementById("backToDisruptionChoice")?.addEventListener("click", openDisruptionModal);

  } catch (err) {
    content.innerHTML = `
      <p class="error-message">Failed to load lines: ${escapeHtml(err.message || 'Unknown error')}</p>
      <button type="button" class="btn btn-outline" id="backToDisruptionChoice">Back</button>
    `;
    document.getElementById("backToDisruptionChoice")?.addEventListener("click", openDisruptionModal);
  }
}

async function showStationSelection() {
  const content = document.getElementById("disruptionModalContent");
  const title = document.getElementById("disruptionModalTitle");
  if (!content || !currentJourney) return;

  title.textContent = "Select Disrupted Station";
  content.innerHTML = '<p class="loading">Loading stations...</p>';

  try {
    const stops = await api.get(`/api/journeys/${currentJourney.journeyId}/stops`);

    if (!stops || stops.length === 0) {
      content.innerHTML = `
        <p class="disruption-modal-subtitle">No stations found in your journey.</p>
        <button type="button" class="btn btn-outline" id="backToDisruptionChoice">Back</button>
      `;
      document.getElementById("backToDisruptionChoice")?.addEventListener("click", openDisruptionModal);
      return;
    }

    const stopsHtml = stops.map((stop, index) => `
      <button type="button" class="disruption-station-btn" data-stop-point-id="${escapeHtml(stop.stopPointId)}">
        <span class="station-sequence">${index + 1}</span>
        <span class="station-info">
          <span class="station-name">${escapeHtml(stop.name || 'Unknown station')}</span>
          ${stop.onLineCode ? `<span class="station-line">Line ${escapeHtml(stop.onLineCode)}</span>` : ''}
        </span>
      </button>
    `).join('');

    content.innerHTML = `
      <p class="disruption-modal-subtitle">Select the station that is disrupted:</p>
      <div class="disruption-stations-list">${stopsHtml}</div>
      <button type="button" class="btn btn-outline btn-sm" id="backToDisruptionChoice">Back</button>
    `;

    content.querySelectorAll(".disruption-station-btn").forEach(btn => {
      btn.addEventListener("click", () => {
        const stopPointId = btn.getAttribute("data-stop-point-id");
        if (stopPointId) reportStationDisruption(stopPointId);
      });
    });

    document.getElementById("backToDisruptionChoice")?.addEventListener("click", openDisruptionModal);

  } catch (err) {
    content.innerHTML = `
      <p class="error-message">Failed to load stations: ${escapeHtml(err.message || 'Unknown error')}</p>
      <button type="button" class="btn btn-outline" id="backToDisruptionChoice">Back</button>
    `;
    document.getElementById("backToDisruptionChoice")?.addEventListener("click", openDisruptionModal);
  }
}

async function reportLineDisruption(lineCode) {
  const content = document.getElementById("disruptionModalContent");
  if (!content || !currentJourney) return;

  content.innerHTML = '<p class="loading">Reporting disruption and finding alternatives...</p>';

  try {
    const result = await api.post(`/api/journeys/${currentJourney.journeyId}/disruptions/line`, {
      lineCode: lineCode
    });

    closeDisruptionModal();
    handleRerouteResult(result, `Line ${lineCode}`);

  } catch (err) {
    content.innerHTML = `
      <p class="error-message">Failed to report disruption: ${escapeHtml(err.message || 'Unknown error')}</p>
      <button type="button" class="btn btn-outline" id="backToDisruptionChoice">Back</button>
    `;
    document.getElementById("backToDisruptionChoice")?.addEventListener("click", openDisruptionModal);
  }
}

async function reportStationDisruption(stopPointId) {
  const content = document.getElementById("disruptionModalContent");
  if (!content || !currentJourney) return;

  content.innerHTML = '<p class="loading">Reporting disruption and finding alternatives...</p>';

  try {
    const result = await api.post(`/api/journeys/${currentJourney.journeyId}/disruptions/station`, {
      stopPointId: stopPointId
    });

    closeDisruptionModal();
    handleRerouteResult(result, result.disruptedPoint?.name || 'the station');

  } catch (err) {
    content.innerHTML = `
      <p class="error-message">Failed to report disruption: ${escapeHtml(err.message || 'Unknown error')}</p>
      <button type="button" class="btn btn-outline" id="backToDisruptionChoice">Back</button>
    `;
    document.getElementById("backToDisruptionChoice")?.addEventListener("click", openDisruptionModal);
  }
}

function handleRerouteResult(result, disruptionDescription) {
  currentJourneyPanel.classList.add("hidden");
  document.querySelector(".results-panel").classList.remove("hidden");

  const alternatives = result.alternatives || [];

  if (alternatives.length > 0) {
    resultsDiv.innerHTML = `
      <div class="reroute-header">
        <div class="reroute-warning">
          <span class="reroute-icon">⚠️</span>
          <div class="reroute-info">
            <strong>Disruption reported on ${escapeHtml(disruptionDescription)}</strong>
            ${result.newOrigin ? `<p>Rerouting from: ${escapeHtml(result.newOrigin.name)}</p>` : ''}
          </div>
        </div>
      </div>
    `;
    displayJourneyResults(alternatives);
    showToast("Disruption reported. Choose an alternative route below.", {
      variant: "warning",
      durationMs: 6000,
    });
  } else {
    resultsDiv.innerHTML = `
      <div class="reroute-header">
        <div class="reroute-warning">
          <span class="reroute-icon">⚠️</span>
          <div class="reroute-info">
            <strong>Disruption reported on ${escapeHtml(disruptionDescription)}</strong>
            <p>No alternative routes could be found.</p>
          </div>
        </div>
      </div>
    `;
    showToast("Disruption reported, but no alternative routes found.", {
      variant: "warning",
    });
  }

  currentJourney = null;
}

async function handleJourneySubmit(e) {
  e.preventDefault();

  if (!currentUser) {
    if (resultsDiv)
      resultsDiv.innerHTML =
        '<p class="error-message">Please log in first.</p>';
    return;
  }

  const from = (document.getElementById("from")?.value || "").trim();
  const to = (document.getElementById("to")?.value || "").trim();
  const departure = departureInput?.value || "";
  const comfort = !!document.getElementById("comfort")?.checked;
  const touristic = !!document.getElementById("touristic")?.checked;

  if (!departure) {
    if (resultsDiv)
      resultsDiv.innerHTML =
        '<p class="error-message">Please select a departure time.</p>';
    return;
  }

  if (comfort && !validateComfortMode()) {
    return;
  }

  // Tâches depuis Google uniquement (/for-journey), sans stockage local
  let taskDetails = [];
  try {
    const tasks = await api.get(`/api/google/tasks/users/${currentUser.userId}/for-journey?includeCompleted=false`);
    if (Array.isArray(tasks)) {
      taskDetails = tasks
        .filter(t => t && !t.completed && t.id && t.locationQuery && t.locationHint && t.locationHint.lat != null && t.locationHint.lng != null)
        .map(t => ({
          id: t.id,
          title: t.title || "",
          locationQuery: t.locationQuery || "",
          lat: t.locationHint.lat,
          lng: t.locationHint.lng,
          completed: !!t.completed,
        }));
    }
  } catch (err) {
    console.debug("Could not load tasks for optimization:", err);
  }

  const payload = {
    journey: {
      userId: currentUser.userId,
      originQuery: from,
      destinationQuery: to,
      departureTime: departure,
      taskIds: null,
      taskDetails: taskDetails.length > 0 ? taskDetails : null,
    },
    preferences: {
      comfortMode: comfort,
      touristicMode: touristic,
    },
  };

  if (resultsDiv)
    resultsDiv.innerHTML = '<p class="loading">Planning your journey...</p>';

  try {
    const journeys = await api.post("/api/journeys", payload);

    resultsDiv.innerHTML = "";
    const list = Array.isArray(journeys) ? journeys : [journeys];

    if (list.length === 0) {
      resultsDiv.innerHTML = '<p class="error-message">No journey found.</p>';
      return;
    }

    displayJourneyResults(list);
  } catch (err) {
    if (resultsDiv)
      resultsDiv.innerHTML = '<p class="error-message">No journey found.</p>';
  }
}

let lastDisplayedJourneysById = null;

function displayJourneyResults(journeys) {
  if (!resultsDiv || !journeys.length) return;
  lastDisplayedJourneysById = new Map();
  journeys.forEach((j) => lastDisplayedJourneysById.set(j.journeyId, j));

  const firstJourney = journeys[0];
  const modesHtml = `
    <div class="journey-modes">
      <span>Confort: ${firstJourney?.comfortModeEnabled ? "Oui" : "Non"}</span>
      <span>Touristique: ${firstJourney?.touristicModeEnabled ? "Oui" : "Non"}</span>
    </div>
  `;
  resultsDiv.innerHTML = `<div class="journey-results-header">${modesHtml}</div>`;

  journeys.forEach((journey, index) =>
    displayJourneyCard(journey, index + 1, journeys.length)
  );
}

/**
 * Displays a single journey card
 */
function displayJourneyCard(journey, index, total) {
  if (!resultsDiv) return;

  const departure = journey?.plannedDeparture
    ? formatDateTime(journey.plannedDeparture)
    : "—";
  const arrival = journey?.plannedArrival
    ? formatDateTime(journey.plannedArrival)
    : "—";
  
  // Calculer la durée totale du trajet
  let totalDuration = null;
  if (journey?.plannedDeparture && journey?.plannedArrival) {
    const depTime = new Date(journey.plannedDeparture).getTime();
    const arrTime = new Date(journey.plannedArrival).getTime();
    const durationSeconds = Math.round((arrTime - depTime) / 1000);
    totalDuration = formatDuration(durationSeconds);
  } else {
    // Fallback: additionner les durées des segments
    const segments = Array.isArray(journey?.segments) ? journey.segments : [];
    const totalSeconds = segments.reduce((sum, seg) => {
      return sum + (seg.durationSeconds || 0);
    }, 0);
    if (totalSeconds > 0) {
      totalDuration = formatDuration(totalSeconds);
    }
  }

  const segments = Array.isArray(journey?.segments) ? journey.segments : [];
  const includedTasks = Array.isArray(journey?.includedTasks) ? journey.includedTasks : [];

  // Process segments: filter out noise, format for display
  const processedSegments = segments
    .map((seg) => {
      // Get origin and destination from points
      const points = Array.isArray(seg.points) ? seg.points : [];
      const originPoint = points[0];
      const destPoint = points.length > 1 ? points[points.length - 1] : originPoint;

      return {
        ...seg,
        originLabel: originPoint?.name || seg.lineName || "?",
        destinationLabel: destPoint?.name || seg.lineName || "?",
        mode: seg.transitMode || seg.segmentType || "OTHER"
      };
    })
    .filter((seg) => {
      const duration = seg.durationSeconds || 0;
      const samePlace = seg.originLabel === seg.destinationLabel;
      const isTransferOrWalk = seg.segmentType === "WALKING" ||
                               seg.segmentType === "TRANSFER" ||
                               seg.segmentType === "WAITING";

      // Filter out:
      // 1. Zero or very short duration same-place segments (noise)
      // 2. WAITING segments (should already be filtered backend, but just in case)
      // 3. Short transfer/walk segments with same origin/destination
      if (seg.segmentType === "WAITING") return false;
      if (duration < 30 && samePlace) return false;
      if (isTransferOrWalk && duration < 60 && samePlace) return false;

      // Filter out segments with unknown origin AND destination
      if (seg.originLabel === "?" && seg.destinationLabel === "?") return false;

      return true;
    });

  const transportSummary = getTransportModesSummaryFromSegments(processedSegments);
  const cardId = "journey-card-" + (journey.journeyId || index);
  const stepsId = "journey-steps-" + (journey.journeyId || index);
  const isOptimized = includedTasks.length > 0;

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
            seg.lineName ? ` <span class="leg-line-name">${escapeHtml(seg.lineName)}</span>` : ""
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

  // Add included tasks to the steps HTML
  const stepsHtml = buildItineraryStepsHtmlFromSegments(processedSegments, includedTasks);

  if (isOptimized) {
    const badgeLabel = index === 1
      ? "Meilleur trajet avec une tâche"
      : "Trajet avec cette tâche";
    const html = `
    <div class="journey-result journey-result-collapsible" id="${escapeHtml(cardId)}" data-journey-id="${escapeHtml(String(journey.journeyId || ""))}">
      <div class="journey-result-header" role="button" tabindex="0" aria-expanded="false" aria-controls="${escapeHtml(stepsId)}">
        <span class="journey-optimized-badge">${escapeHtml(badgeLabel)}</span>
        <h3 class="journey-result-title">${escapeHtml(journey?.originLabel || "—")} → ${escapeHtml(journey?.destinationLabel || "—")}</h3>
        <p class="journey-meta">Départ: ${departure} • Arrivée: ${arrival}${totalDuration ? ` • Durée: <strong>${totalDuration}</strong>` : ""}</p>
        <p class="journey-transport-summary">Transports: ${escapeHtml(transportSummary)}</p>
        <span class="journey-toggle-steps" aria-hidden="true">Afficher les étapes</span>
      </div>
      <div class="journey-result-steps" id="${escapeHtml(stepsId)}" hidden>
        <button class="btn btn-primary btn-sm start-journey-btn" onclick="startJourney('${escapeHtml(String(journey.journeyId || ""))}', this)">Démarrer le trajet</button>
        <h4 class="journey-steps-heading">Étapes de l'itinéraire</h4>
        <ul class="journey-legs">${stepsHtml}</ul>
      </div>
    </div>
    `;
    resultsDiv.insertAdjacentHTML("beforeend", html);
    const card = document.getElementById(cardId);
    const stepsEl = document.getElementById(stepsId);
    const toggleLabel = card?.querySelector(".journey-toggle-steps");
    card?.querySelector(".journey-result-header")?.addEventListener("click", () => {
      if (!stepsEl) return;
      const isOpen = !stepsEl.hidden;
      stepsEl.hidden = isOpen;
      if (toggleLabel) toggleLabel.textContent = isOpen ? "Afficher les étapes" : "Masquer les étapes";
      card?.querySelector(".journey-result-header")?.setAttribute("aria-expanded", String(!isOpen));
      card?.classList.toggle("journey-result-open", !isOpen);
    });
    return;
  }

  const optionLabel =
    total > 1 ? `<span class="route-option-label">Option ${index}</span> ` : "";

  const totalDurationHtml = totalDuration
    ? ` • Durée: <strong>${totalDuration}</strong>`
    : "";

  const html = `
    <div class="journey-result">
      <h3>${optionLabel}${escapeHtml(journey?.originLabel || "—")} → ${escapeHtml(journey?.destinationLabel || "—")}</h3>
      <p class="journey-meta">Départ: ${departure} • Arrivée: ${arrival}${totalDurationHtml}</p>
      <button class="btn btn-primary btn-sm start-journey-btn" onclick="startJourney('${journey.journeyId}', this)">Démarrer le trajet</button>
      <h4 class="journey-steps-heading">Étapes de l'itinéraire</h4>
      <ul class="journey-legs">${legsHtml}</ul>
    </div>
  `;
  resultsDiv.insertAdjacentHTML("beforeend", html);
}

// ============================================================
// GOOGLE TASKS
// ============================================================
function setupTasks() {
  refreshTasksBtn?.addEventListener("click", () =>
    ensureDefaultTaskListLoaded({ force: true })
  );
  tasksIncludeCompleted?.addEventListener("change", () =>
    loadTasksFromDefaultList()
  );
  updateTasksUIState();
}

function isGoogleLinked() {
  return !!(
    currentUser &&
    (currentUser.googleAccountLinkedAt || currentUser.googleAccountSubject)
  );
}

function updateTasksUIState() {
  const enabled = !!currentUser && isGoogleLinked();

  if (refreshTasksBtn) refreshTasksBtn.disabled = !enabled;

  if (createTaskForm) {
    const submitBtn = createTaskForm.querySelector('button[type="submit"]');
    if (submitBtn) submitBtn.disabled = !enabled;
    if (taskTitle) taskTitle.disabled = !enabled;
    if (taskNotes) taskNotes.disabled = !enabled;
    if (taskDue) taskDue.disabled = !enabled;
    if (taskLocationQuery) taskLocationQuery.disabled = !enabled;
  }

  if (tasksListName) {
    if (!currentUser) tasksListName.textContent = "Default list: —";
    else if (!isGoogleLinked())
      tasksListName.textContent = "Default list: (link Google Tasks)";
    else
      tasksListName.textContent = defaultTaskList?.title
        ? `Default list: ${defaultTaskList.title}`
        : "Default list: …";
  }

  if (tasksResults && currentView === "tasks") {
    if (!currentUser) {
      tasksResults.innerHTML = `<p class="results-placeholder">Please log in to use Google Tasks.</p>`;
    } else if (!isGoogleLinked()) {
      tasksResults.innerHTML = `<p class="results-placeholder">Link Google Tasks to load your tasks.</p>`;
    } else if (!defaultTaskList) {
      tasksResults.innerHTML = `<p class="results-placeholder">Loading your default list…</p>`;
    }
  }
}

function resetTasksUI() {
  if (tasksListName) tasksListName.textContent = "Default list: —";
  if (tasksResults)
    tasksResults.innerHTML = `<p class="results-placeholder">Tasks will appear here.</p>`;
}

async function ensureDefaultTaskListLoaded({ force }) {
  if (!currentUser) return;
  if (!isGoogleLinked()) return updateTasksUIState();
  if (defaultTaskList && !force) {
    updateTasksUIState();
    return loadTasksFromDefaultList();
  }

  defaultTaskList = null;
  updateTasksUIState();

  try {
    const list = await api.get(
      `/api/google/tasks/users/${currentUser.userId}/default-list`
    );
    defaultTaskList = { id: list.id, title: list.title || "Default" };
    updateTasksUIState();
    await loadTasksFromDefaultList();
  } catch (err) {
    defaultTaskList = null;
    if (err.authError) {
      if (tasksResults)
        tasksResults.innerHTML =
          '<p class="error-message">Google Tasks not authorized. Click "Link Google Tasks".</p>';
      showToast("Link Google Tasks first.", { variant: "warning" });
    } else {
      if (tasksResults)
        tasksResults.innerHTML =
          '<p class="error-message">Could not load default list.</p>';
      showToast(err?.message || "Could not load default list.", {
        variant: "warning",
      });
    }
    updateTasksUIState();
  }
}

async function loadTasksFromDefaultList() {
  if (!currentUser) return;
  if (!isGoogleLinked()) return;
  if (!defaultTaskList?.id) return;

  if (tasksResults)
    tasksResults.innerHTML = '<p class="loading">Loading tasks...</p>';

  try {
    const includeCompleted = !!tasksIncludeCompleted?.checked;
    const url = `/api/google/tasks/users/${
      currentUser.userId
    }/lists/${encodeURIComponent(
      defaultTaskList.id
    )}/tasks?includeCompleted=${includeCompleted}`;
    const tasks = await api.get(url);
    renderTasks(tasks);
  } catch (err) {
    if (err.authError) {
      if (tasksResults)
        tasksResults.innerHTML =
          '<p class="error-message">Google Tasks not authorized. Click "Link Google Tasks".</p>';
      showToast("Link Google Tasks first.", { variant: "warning" });
    } else {
      if (tasksResults)
        tasksResults.innerHTML = `<p class="error-message">Error: ${escapeHtml(
          err?.message || "Unknown error"
        )}</p>`;
    }
  }
}

function renderTasks(tasks) {
  if (!tasksResults) return;

  if (!Array.isArray(tasks) || !tasks.length) {
    tasksResults.innerHTML = `<p class="results-placeholder">No tasks in this list.</p>`;
    return;
  }

  const html = tasks
    .map((t) => {
      const id = String(t?.id || "");
      const title = escapeHtml(t?.title || "Untitled");
      const due = t?.due ? formatDateTime(t.due) : "—";
      const statusRaw = (t?.status || "needsAction").toLowerCase();
      const completed = statusRaw === "completed";
      const location = t?.locationQuery ? escapeHtml(t.locationQuery) : null;

      const completeBtn = completed
        ? `<button type="button" class="btn btn-success btn-sm" disabled>Completed</button>`
        : `<button type="button" class="btn btn-success btn-sm" data-action="complete" data-task-id="${escapeHtml(
            id
          )}">Complete</button>`;

      return `
        <div class="task-card ${completed ? "completed" : ""}">
          <h3 class="task-title">${title}</h3>
          <p class="task-meta">Due: ${escapeHtml(due)} • Status: ${escapeHtml(
        statusRaw
      )}${location ? ` • Location: ${location}` : ""}</p>
          <div class="task-actions">
            ${completeBtn}
            <button type="button" class="btn btn-danger btn-sm" data-action="delete" data-task-id="${escapeHtml(
              id
            )}">Delete</button>
          </div>
        </div>
      `;
    })
    .join("");

  tasksResults.innerHTML = `<div class="tasks-results">${html}</div>`;

  tasksResults.querySelectorAll("[data-action='complete']").forEach((btn) => {
    btn.addEventListener("click", async () => {
      const taskId = btn.getAttribute("data-task-id");
      if (!taskId) return;
      await completeTask(taskId);
    });
  });

  tasksResults.querySelectorAll("[data-action='delete']").forEach((btn) => {
    btn.addEventListener("click", async () => {
      const taskId = btn.getAttribute("data-task-id");
      if (!taskId) return;
      await deleteTask(taskId);
    });
  });
}

async function completeTask(taskId) {
  if (!currentUser || !defaultTaskList?.id) return;

  try {
    const url = `/api/google/tasks/users/${
      currentUser.userId
    }/lists/${encodeURIComponent(
      defaultTaskList.id
    )}/tasks/${encodeURIComponent(taskId)}/complete`;
    await api.patch(url);
    showToast("Task completed!", { variant: "success" });
    await loadTasksFromDefaultList();
  } catch (err) {
    if (err.authError) {
      showToast("Link Google Tasks first.", { variant: "warning" });
    } else {
      showToast(err?.message || "Failed to complete task", {
        variant: "warning",
      });
    }
  }
}

async function deleteTask(taskId) {
  if (!currentUser || !defaultTaskList?.id) return;
  if (!confirm("Delete this task?")) return;

  try {
    const url = `/api/google/tasks/users/${
      currentUser.userId
    }/lists/${encodeURIComponent(
      defaultTaskList.id
    )}/tasks/${encodeURIComponent(taskId)}`;
    await api.delete(url);
    showToast("Task deleted.", { variant: "success" });
    await loadTasksFromDefaultList();
  } catch (err) {
    if (err.authError) {
      showToast("Link Google Tasks first.", { variant: "warning" });
    } else {
      showToast(err?.message || "Failed to delete task", {
        variant: "warning",
      });
    }
  }
}

// ============================================================
// GOOGLE ACCOUNT LINKING
// ============================================================
function setupGoogleLinkListeners() {
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

  if (!currentUser) {
    if (statusEl) statusEl.textContent = "Please log in first.";
    return;
  }

  const linkUrl = `/api/google/tasks/link?userId=${encodeURIComponent(
    currentUser.userId
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

async function refreshGoogleLink() {
  if (!currentUser?.userId) return;

  try {
    const user = await api.get(`/api/users/${currentUser.userId}`);
    currentUser = user;
    renderGoogleLinkStatus(user);
    updateTasksUIState();
    if (currentView === "tasks") ensureDefaultTaskListLoaded({ force: true });
  } catch {
    // Silently fail on refresh
  }
}

function clearGoogleLinkStatus() {
  if (!googleLinkStatusBadge) return;
  googleLinkStatusBadge.textContent = "";
  googleLinkStatusBadge.classList.remove("linked");
}

function renderGoogleLinkStatus(user) {
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

// ============================================================
// COMFORT PROFILE
// ============================================================
function setupComfortProfileListeners() {
  editComfortProfileBtn?.addEventListener("click", openComfortProfileModal);
  closeComfortProfileModal?.addEventListener(
    "click",
    closeComfortProfileModalFn
  );
  clearComfortProfileBtn?.addEventListener("click", clearComfortProfile);
  comfortProfileForm?.addEventListener("submit", saveComfortProfile);

  comfortProfileModal?.addEventListener("click", (e) => {
    if (e.target === comfortProfileModal) closeComfortProfileModalFn();
  });
}

function openComfortProfileModal() {
  if (!comfortProfileModal) return;
  comfortProfileModal.classList.remove("hidden");
  loadComfortProfileIntoForm();
}

function closeComfortProfileModalFn() {
  if (!comfortProfileModal) return;
  comfortProfileModal.classList.add("hidden");
  document.getElementById("comfortProfileError")?.classList.add("hidden");
}

function loadComfortProfileIntoForm() {
  if (!currentUser?.comfortProfile) return;
  const profile = currentUser.comfortProfile;

  const directPath = document.getElementById("directPath");
  const requireAC = document.getElementById("requireAirConditioning");
  const maxTransfers = document.getElementById("maxNbTransfers");
  const maxWaiting = document.getElementById("maxWaitingDuration");
  const maxWalking = document.getElementById("maxWalkingDuration");

  if (directPath) directPath.value = profile.directPath || "";
  if (requireAC) requireAC.checked = !!profile.requireAirConditioning;
  if (maxTransfers) maxTransfers.value = profile.maxNbTransfers ?? "";
  if (maxWaiting)
    maxWaiting.value = profile.maxWaitingDuration
      ? Math.round(profile.maxWaitingDuration / 60)
      : "";
  if (maxWalking)
    maxWalking.value = profile.maxWalkingDuration
      ? Math.round(profile.maxWalkingDuration / 60)
      : "";
}

async function saveComfortProfile(e) {
  e.preventDefault();
  if (!currentUser) return;

  const directPath = document.getElementById("directPath")?.value || null;
  const requireAC = !!document.getElementById("requireAirConditioning")
    ?.checked;
  const maxTransfersVal = document.getElementById("maxNbTransfers")?.value;
  const maxWaitingVal = document.getElementById("maxWaitingDuration")?.value;
  const maxWalkingVal = document.getElementById("maxWalkingDuration")?.value;

  const payload = {
    directPath: directPath || null,
    requireAirConditioning: requireAC,
    maxNbTransfers: maxTransfersVal ? parseInt(maxTransfersVal, 10) : null,
    maxWaitingDuration: maxWaitingVal ? parseInt(maxWaitingVal, 10) * 60 : null,
    maxWalkingDuration: maxWalkingVal ? parseInt(maxWalkingVal, 10) * 60 : null,
  };

  const errorEl = document.getElementById("comfortProfileError");

  try {
    const updated = await api.put(
      `/api/users/${currentUser.userId}/comfort-profile`,
      payload
    );
    currentUser.comfortProfile = updated;
    renderComfortProfileSummary();
    closeComfortProfileModalFn();
    showToast("Comfort profile saved!", { variant: "success" });
  } catch (err) {
    if (errorEl) {
      errorEl.textContent = err?.message || "Failed to save";
      errorEl.classList.remove("hidden");
    }
  }
}

async function clearComfortProfile() {
  if (!currentUser) return;
  if (!confirm("Clear all comfort profile settings?")) return;

  try {
    await api.delete(`/api/users/${currentUser.userId}/comfort-profile`);
    currentUser.comfortProfile = null;
    renderComfortProfileSummary();
    closeComfortProfileModalFn();
    showToast("Comfort profile cleared.", { variant: "success" });
  } catch (err) {
    showToast(err?.message || "Failed to clear profile", {
      variant: "warning",
    });
  }
}

function renderComfortProfileSummary() {
  if (!comfortProfileSummary) return;

  const profile = currentUser?.comfortProfile;

  if (!profile || !hasComfortSettings(profile)) {
    comfortProfileSummary.innerHTML = "<p>No comfort profile configured.</p>";
    return;
  }

  const items = [];

  if (profile.directPath) {
    const labels = {
      indifferent: "Indifferent",
      none: "No direct path",
      only: "Direct path only",
      only_with_alternatives: "Direct with alternatives",
    };
    items.push(
      `<li>Direct Path: ${
        labels[profile.directPath] || profile.directPath
      }</li>`
    );
  }

  if (profile.requireAirConditioning) {
    items.push("<li>Require Air Conditioning: Yes</li>");
  }

  if (profile.maxNbTransfers != null) {
    items.push(`<li>Max Transfers: ${profile.maxNbTransfers}</li>`);
  }

  if (profile.maxWaitingDuration != null) {
    const mins = Math.round(profile.maxWaitingDuration / 60);
    items.push(`<li>Max Waiting: ${mins} min</li>`);
  }

  if (profile.maxWalkingDuration != null) {
    const mins = Math.round(profile.maxWalkingDuration / 60);
    items.push(`<li>Max Walking: ${mins} min</li>`);
  }

  comfortProfileSummary.innerHTML = `<ul class="comfort-summary-list">${items.join(
    ""
  )}</ul>`;
}

function hasComfortSettings(profile) {
  if (!profile) return false;
  return (
    profile.directPath != null ||
    profile.requireAirConditioning === true ||
    profile.maxNbTransfers != null ||
    profile.maxWaitingDuration != null ||
    profile.maxWalkingDuration != null
  );
}

function validateComfortMode() {
  if (!comfortCheckbox?.checked) return true;

  const profile = currentUser?.comfortProfile;
  if (!hasComfortSettings(profile)) {
    showToast("Please configure your comfort profile first.", {
      variant: "warning",
      durationMs: 5000,
    });
    openComfortProfileModal();
    return false;
  }

  return true;
}

// ============================================================
// UI UTILITIES
// ============================================================
function ensureToastUI() {
  if (!document.getElementById("toastContainer")) {
    const container = document.createElement("div");
    container.id = "toastContainer";
    container.className = "toast-container";
    document.body.appendChild(container);
  }

  if (!document.getElementById("toastContainerImportant")) {
    const container = document.createElement("div");
    container.id = "toastContainerImportant";
    container.className = "toast-container toast-container-important";
    document.body.appendChild(container);
  }
}

function showToast(message, opts = {}) {
  const containerId = opts.important
    ? "toastContainerImportant"
    : "toastContainer";
  const container = document.getElementById(containerId);
  if (!container) return;

  const toast = document.createElement("div");
  toast.className = `toast ${opts.variant ? `toast-${opts.variant}` : ""} ${
    opts.important ? "toast-important" : ""
  }`.trim();

  const text = document.createElement("div");
  text.className = "toast-text";
  text.textContent = message;

  const actions = document.createElement("div");
  actions.className = "toast-actions";

  if (opts.actionText && typeof opts.onAction === "function") {
    const btn = document.createElement("button");
    btn.type = "button";
    btn.className = "toast-btn";
    btn.textContent = opts.actionText;
    btn.addEventListener("click", () => {
      opts.onAction();
      removeToast(toast);
    });
    actions.appendChild(btn);
  }

  const closeBtn = document.createElement("button");
  closeBtn.type = "button";
  closeBtn.className = "toast-close";
  closeBtn.innerHTML = "&times;";
  closeBtn.addEventListener("click", () => removeToast(toast));

  toast.appendChild(text);
  toast.appendChild(actions);
  toast.appendChild(closeBtn);

  container.appendChild(toast);

  const ttl =
    typeof opts.durationMs === "number"
      ? opts.durationMs
      : opts.important
      ? CONFIG.TOAST_IMPORTANT_DURATION_MS
      : CONFIG.TOAST_DURATION_MS;
  const timer = setTimeout(() => removeToast(toast), ttl);

  toast.addEventListener("mouseenter", () => clearTimeout(timer));
}

function removeToast(toastEl) {
  if (!toastEl) return;
  toastEl.classList.add("toast-hide");
  setTimeout(() => toastEl.remove(), CONFIG.TOAST_HIDE_DELAY_MS);
}

function escapeHtml(str) {
  return String(str)
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#039;");
}

function setDefaultDepartureTime() {
  if (!departureInput) return;
  const d = new Date(Date.now() + 60 * 60 * 1000);
  d.setMinutes(d.getMinutes() - d.getTimezoneOffset());
  departureInput.value = d.toISOString().slice(0, 16);
}

function formatDuration(seconds) {
  if (seconds === null || seconds === undefined) return "?";
  const s = Number(seconds);
  if (Number.isNaN(s)) return "?";
  const h = Math.floor(s / 3600);
  const m = Math.floor((s % 3600) / 60);
  return h > 0 ? `${h}h ${m}m` : `${m}m`;
}

function formatDateTime(dt) {
  return new Date(dt).toLocaleString();
}

function formatMode(mode) {
  if (!mode) return "Unknown";
  if (mode === "OTHER") return "Connection";
  if (mode === "WALK") return "Marche";
  const s = String(mode);
  if (s === "METRO") return "Métro";
  if (s === "RER" || s === "BUS" || s === "TRAM") return s.charAt(0) + s.slice(1).toLowerCase();
  return s.charAt(0).toUpperCase() + s.slice(1).toLowerCase();
}

/**
 * Résume les modes de transport d'un trajet (ex: "Bus 268, RER D, Métro 4, Marche").
 */
function getTransportModesSummary(legs) {
  if (!Array.isArray(legs) || !legs.length) return "—";
  const seen = new Set();
  const parts = [];
  for (const leg of legs) {
    const mode = (leg.mode || "").toUpperCase();
    if (mode === "OTHER" || mode === "CONNECTION") continue;
    const label = mode === "WALK" ? "Marche" : formatMode(leg.mode) + (leg.lineCode ? " " + leg.lineCode : "");
    const key = label.trim();
    if (!seen.has(key)) {
      seen.add(key);
      parts.push(key);
    }
  }
  return parts.length ? parts.join(", ") : "—";
}

/**
 * Résume les modes de transport d'un trajet à partir des segments.
 */
function getTransportModesSummaryFromSegments(segments) {
  if (!Array.isArray(segments) || !segments.length) return "—";
  const seen = new Set();
  const parts = [];
  for (const seg of segments) {
    const mode = (seg.mode || seg.transitMode || "").toUpperCase();
    if (mode === "OTHER" || mode === "CONNECTION" || mode === "WALKING" || mode === "TRANSFER") continue;
    const label = mode === "WALK" ? "Marche" : formatMode(mode) + (seg.lineCode ? " " + seg.lineCode : "");
    const key = label.trim();
    if (!seen.has(key)) {
      seen.add(key);
      parts.push(key);
    }
  }
  return parts.length ? parts.join(", ") : "—";
}

/**
 * Builds itinerary steps HTML from segments with included tasks.
 */
function buildItineraryStepsHtmlFromSegments(segments, includedTasks) {
  if (!Array.isArray(segments) || !segments.length) {
    return "<li>Aucune étape disponible</li>";
  }

  const taskInsertionPoints = new Map();
  if (Array.isArray(includedTasks) && includedTasks.length > 0) {
    includedTasks.forEach((task, taskIdx) => {
      const idx = findTaskInsertionIndexForSegments(task, segments, taskIdx, includedTasks.length);
      if (idx >= 0 && idx <= segments.length) {
        if (!taskInsertionPoints.has(idx)) taskInsertionPoints.set(idx, []);
        taskInsertionPoints.get(idx).push({ task, originalIndex: taskIdx });
      }
    });
  }

  const mixedSteps = [];
  for (let i = 0; i <= segments.length; i++) {
    if (taskInsertionPoints.has(i)) {
      const tasks = taskInsertionPoints.get(i).sort((a, b) => a.originalIndex - b.originalIndex);
      tasks.forEach(({ task }) => mixedSteps.push({ type: "task", data: task }));
    }
    if (i < segments.length) mixedSteps.push({ type: "segment", data: segments[i] });
  }

  return mixedSteps
    .map((step) => {
      if (step.type === "task") {
        const task = step.data;
        return `
        <li class="journey-section-label journey-task-section-start">Arrivée sur place pour la tâche</li>
        <li class="journey-leg-item journey-task-stop">
            <div class="leg-marker task-marker">✓</div>
            <div class="leg-content">
                <span class="leg-mode task-mode">📋 Tâche à réaliser</span>
                <span class="leg-route task-title"><strong>${escapeHtml(task.title || "Tâche sans titre")}</strong></span>
                <div class="leg-times task-location">📍 ${escapeHtml(task.locationQuery || "Localisation inconnue")}</div>
                <p class="task-access-hint">Vous y accédez par les étapes de transport ci-dessus. Une fois sur place, effectuez la tâche puis reprenez le trajet ci-dessous.</p>
            </div>
        </li>
        <li class="journey-section-label journey-task-section-end">Suite du trajet</li>
    `;
      }
      const seg = step.data;
      return `
        <li class="journey-leg-item">
            <div class="leg-marker-container">
                ${formatLineBadge(seg.mode, seg.lineCode, seg.lineColor)}
            </div>
            <div class="leg-content">
                <span class="leg-mode">${formatMode(seg.mode)}${
            seg.lineName ? ` <span class="leg-line-name">${escapeHtml(seg.lineName)}</span>` : ""
          }</span>
                <span class="leg-route">${seg.originLabel || "?"} → ${seg.destinationLabel || "?"}</span>
                <div class="leg-times">
                    ${seg.scheduledDeparture ? formatDateTime(seg.scheduledDeparture) : "?"} -
                    ${seg.scheduledArrival ? formatDateTime(seg.scheduledArrival) : "?"}
                    <span class="leg-duration">(${seg.durationSeconds ? formatDuration(seg.durationSeconds) : "?"})</span>
                </div>
            </div>
        </li>
    `;
    })
    .join("");
}

function findTaskInsertionIndexForSegments(task, segments, taskIndex, totalTasks) {
  if (!task || segments.length === 0) return -1;
  const taskLocation = (task.locationQuery || "").toLowerCase().trim();
  if (!taskLocation) return Math.floor(segments.length / 2);

  let bestMatch = -1;
  let bestMatchScore = 0;
  for (let i = 0; i < segments.length; i++) {
    const seg = segments[i];
    const destination = (seg.destinationLabel || "").toLowerCase().trim();
    if (!destination) continue;
    if (destination === taskLocation) return i + 1;
    const taskWords = taskLocation.split(/[\s,]+/).filter((w) => w.length > 2);
    const destWords = destination.split(/[\s,]+/).filter((w) => w.length > 2);
    let matchCount = 0;
    taskWords.forEach((tw) => {
      const twClean = tw.replace(/[^\w]/g, "").toLowerCase();
      destWords.forEach((dw) => {
        const dwClean = dw.replace(/[^\w]/g, "").toLowerCase();
        if (dwClean.includes(twClean) || twClean.includes(dwClean)) matchCount++;
      });
    });
    if (matchCount >= 1 && matchCount > bestMatchScore) {
      bestMatch = i + 1;
      bestMatchScore = matchCount;
    }
    if (destination.includes(taskLocation) || taskLocation.includes(destination)) {
      if (matchCount + 1 > bestMatchScore) {
        bestMatch = i + 1;
        bestMatchScore = matchCount + 1;
      }
    }
  }
  if (bestMatch >= 0 && bestMatchScore >= 1) return bestMatch;
  if (totalTasks === 1) return Math.floor(segments.length / 2);
  if (totalTasks > 1) {
    const ratio = (taskIndex + 1) / (totalTasks + 1);
    return Math.max(1, Math.min(Math.floor(ratio * segments.length), segments.length));
  }
  return -1;
}

function findTaskInsertionIndex(task, legs, taskIndex, totalTasks) {
  if (!task || legs.length === 0) return -1;
  const taskLocation = (task.locationQuery || "").toLowerCase().trim();
  if (!taskLocation) return Math.floor(legs.length / 2);

  let bestMatch = -1;
  let bestMatchScore = 0;
  for (let i = 0; i < legs.length; i++) {
    const leg = legs[i];
    const destination = (leg.destinationLabel || "").toLowerCase().trim();
    if (!destination) continue;
    if (destination === taskLocation) return i + 1;
    const taskWords = taskLocation.split(/[\s,]+/).filter((w) => w.length > 2);
    const destWords = destination.split(/[\s,]+/).filter((w) => w.length > 2);
    let matchCount = 0;
    taskWords.forEach((tw) => {
      const twClean = tw.replace(/[^\w]/g, "").toLowerCase();
      destWords.forEach((dw) => {
        const dwClean = dw.replace(/[^\w]/g, "").toLowerCase();
        if (dwClean.includes(twClean) || twClean.includes(dwClean)) matchCount++;
      });
    });
    if (matchCount >= 1 && matchCount > bestMatchScore) {
      bestMatch = i + 1;
      bestMatchScore = matchCount;
    }
    if (destination.includes(taskLocation) || taskLocation.includes(destination)) {
      if (matchCount + 1 > bestMatchScore) {
        bestMatch = i + 1;
        bestMatchScore = matchCount + 1;
      }
    }
  }
  if (bestMatch >= 0 && bestMatchScore >= 1) return bestMatch;
  if (totalTasks === 1) return Math.floor(legs.length / 2);
  if (totalTasks > 1) {
    const ratio = (taskIndex + 1) / (totalTasks + 1);
    return Math.max(1, Math.min(Math.floor(ratio * legs.length), legs.length));
  }
  return -1;
}

function formatLegRoute(leg) {
  const mode = (leg.mode || "").toUpperCase();
  const isConnection = mode === "OTHER" || mode === "CONNECTION";
  const orig = (leg.originLabel || "").trim();
  const dest = (leg.destinationLabel || "").trim();
  const unknownOrig = !orig || orig === "Unknown origin" || orig === "?";
  const unknownDest = !dest || dest === "Unknown destination" || dest === "?";
  if (isConnection && (unknownOrig || unknownDest))
    return "Correspondance · attente entre deux transports";
  return `${unknownOrig ? "—" : orig} → ${unknownDest ? "—" : dest}`;
}

function buildItineraryStepsHtml(journey) {
  const legs = Array.isArray(journey?.legs) ? journey.legs : [];
  const includedTasks = Array.isArray(journey?.includedTasks) ? journey.includedTasks : [];
  const processedLegs = legs
    .filter((leg) => {
      const d = leg.durationSeconds || 0;
      const same = leg.originLabel === leg.destinationLabel;
      return !(d < 60 && same);
    })
    .map((leg) => {
      const d = leg.durationSeconds || 0;
      const same = leg.originLabel === leg.destinationLabel;
      if (d >= 60 && same && (leg.mode || "").toUpperCase() === "OTHER")
        return { ...leg, mode: "WALK" };
      return leg;
    });

  const taskInsertionPoints = new Map();
  includedTasks.forEach((task, taskIdx) => {
    const idx = findTaskInsertionIndex(task, processedLegs, taskIdx, includedTasks.length);
    if (idx >= 0 && idx <= processedLegs.length) {
      if (!taskInsertionPoints.has(idx)) taskInsertionPoints.set(idx, []);
      taskInsertionPoints.get(idx).push({ task, originalIndex: taskIdx });
    }
  });

  const mixedSteps = [];
  for (let i = 0; i <= processedLegs.length; i++) {
    if (taskInsertionPoints.has(i)) {
      const tasks = taskInsertionPoints.get(i).sort((a, b) => a.originalIndex - b.originalIndex);
      tasks.forEach(({ task }) => mixedSteps.push({ type: "task", data: task }));
    }
    if (i < processedLegs.length) mixedSteps.push({ type: "leg", data: processedLegs[i] });
  }

  if (mixedSteps.length === 0) return "<li>Aucune étape disponible</li>";
  return mixedSteps
    .map((step) => {
      if (step.type === "task") {
        const task = step.data;
        return `
        <li class="journey-section-label journey-task-section-start">Arrivée sur place pour la tâche</li>
        <li class="journey-leg-item journey-task-stop">
            <div class="leg-marker task-marker">✓</div>
            <div class="leg-content">
                <span class="leg-mode task-mode">📋 Tâche à réaliser</span>
                <span class="leg-route task-title"><strong>${escapeHtml(task.title || "Tâche sans titre")}</strong></span>
                <div class="leg-times task-location">📍 ${escapeHtml(task.locationQuery || "Localisation inconnue")}</div>
                <p class="task-access-hint">Vous y accédez par les étapes de transport ci-dessus. Une fois sur place, effectuez la tâche puis reprenez le trajet ci-dessous.</p>
            </div>
        </li>
        <li class="journey-section-label journey-task-section-end">Suite du trajet</li>
    `;
      }
      const leg = step.data;
      const routeText = formatLegRoute(leg);
      const modeLabel = (leg.mode && String(leg.mode).toUpperCase() === "OTHER") ? "Correspondance" : formatMode(leg.mode);
      return `
        <li class="journey-leg-item">
            <div class="leg-marker"></div>
            <div class="leg-content">
                <span class="leg-mode">${modeLabel}${leg.lineCode ? ` <span class="leg-line">${leg.lineCode}</span>` : ""}</span>
                <span class="leg-route">${escapeHtml(routeText)}</span>
                <div class="leg-times">
                    ${leg.estimatedDeparture ? formatDateTime(leg.estimatedDeparture) : "?"} - ${leg.estimatedArrival ? formatDateTime(leg.estimatedArrival) : "?"}
                    <span class="leg-duration">(${leg.durationSeconds ? formatDuration(leg.durationSeconds) : "?"})</span>
                </div>
            </div>
        </li>
    `;
    })
    .join("");
}

/**
 * Generates HTML for a transport line badge based on mode and line info.
 * - Metro/RER: Circle with line code (number or letter)
 * - Tram: Circle with T + number
 * - Bus: Rounded pill with line number
 * - Walk/Other: Icon-based representation
 */
function formatLineBadge(mode, lineCode, lineColor) {
  if (!lineCode) {
    // For walk/transfer/other modes without a line
    if (mode === "WALK" || mode === "WALKING") {
      return '<span class="line-badge line-badge-walk"></span>';
    }
    if (mode === "OTHER" || mode === "TRANSFER") {
      return '<span class="line-badge line-badge-transfer"></span>';
    }
    return '';
  }

  const bgColor = lineColor ? `#${lineColor}` : '#666';
  // Determine text color based on background brightness
  const textColor = getContrastColor(bgColor);

  // Determine badge type based on mode
  let badgeClass = 'line-badge';
  let displayCode = escapeHtml(lineCode);

  switch (mode) {
    case 'METRO':
      badgeClass += ' line-badge-metro';
      break;
    case 'RER':
      badgeClass += ' line-badge-rer';
      break;
    case 'TRAM':
      badgeClass += ' line-badge-tram';
      // If lineCode doesn't start with T, add it
      if (!lineCode.toUpperCase().startsWith('T')) {
        displayCode = 'T' + displayCode;
      }
      break;
    case 'BUS':
      badgeClass += ' line-badge-bus';
      break;
    case 'TRANSILIEN':
      badgeClass += ' line-badge-transilien';
      break;
    default:
      badgeClass += ' line-badge-other';
  }

  return `<span class="${badgeClass}" style="background-color: ${bgColor}; color: ${textColor};">${displayCode}</span>`;
}

/**
 * Returns black or white text color based on background color brightness.
 */
function getContrastColor(hexColor) {
  // Remove # if present
  const hex = hexColor.replace('#', '');
  const r = parseInt(hex.substr(0, 2), 16);
  const g = parseInt(hex.substr(2, 2), 16);
  const b = parseInt(hex.substr(4, 2), 16);
  // Calculate luminance
  const luminance = (0.299 * r + 0.587 * g + 0.114 * b) / 255;
  return luminance > 0.5 ? '#000000' : '#ffffff';
}

function generateId() {
  if (window.crypto && typeof window.crypto.randomUUID === "function")
    return window.crypto.randomUUID();
  return `user-${Date.now()}`;
}

// ============================================
// GESTION DU THÈME (MODE CLAIR/SOMBRE)
// ============================================

function initTheme() {
  const themeToggle = document.getElementById("themeToggle");
  const themeIcon = document.getElementById("themeIcon");

  // Récupérer le thème sauvegardé ou utiliser le thème système
  const savedTheme = localStorage.getItem("mavigo_theme");
  const prefersDark = window.matchMedia("(prefers-color-scheme: dark)").matches;
  const initialTheme = savedTheme || (prefersDark ? "dark" : "light");

  // Appliquer le thème initial
  setTheme(initialTheme);

  // Afficher le bouton de thème
  if (themeToggle) {
    themeToggle.classList.remove("hidden");
  }

  // Écouter les changements de préférence système
  window
    .matchMedia("(prefers-color-scheme: dark)")
    .addEventListener("change", (e) => {
      if (!localStorage.getItem("mavigo_theme")) {
        setTheme(e.matches ? "dark" : "light");
      }
    });

  // Écouter le clic sur le bouton toggle
  themeToggle?.addEventListener("click", () => {
    const currentTheme =
      document.documentElement.getAttribute("data-theme") || "light";
    const newTheme = currentTheme === "dark" ? "light" : "dark";
    setTheme(newTheme);
    localStorage.setItem("mavigo_theme", newTheme);
  });
}

function setTheme(theme) {
  const themeIcon = document.getElementById("themeIcon");
  document.documentElement.setAttribute("data-theme", theme);

  if (themeIcon) {
    themeIcon.textContent = theme === "dark" ? "☀️" : "🌙";
  }
}
