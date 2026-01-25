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
let lastNotifiedJourneyId = null;
let lastTasksSignature = null;

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
const closeComfortProfileModal = document.getElementById("closeComfortProfileModal");

// New Refactored Elements
const comfortProfileListView = document.getElementById("comfortProfileListView");
const comfortProfileFormView = document.getElementById("comfortProfileFormView");
const namedSettingsList = document.getElementById("namedSettingsList");
const addNewComfortSettingBtn = document.getElementById("addNewComfortSettingBtn");
const backToComfortListBtn = document.getElementById("backToComfortListBtn");
const comfortFormTitle = document.getElementById("comfortFormTitle");
const comfortFormSubtitle = document.getElementById("comfortFormSubtitle");
const editingSettingId = document.getElementById("editingSettingId");
const saveComfortSettingBtn = document.getElementById("saveComfortSettingBtn");
const deleteComfortSettingBtn = document.getElementById("deleteComfortSettingBtn");
const settingNameInput = document.getElementById("settingName");

const comfortOnboardingModal = document.getElementById("comfortOnboardingModal");
const setupComfortNowBtn = document.getElementById("setupComfortNowBtn");
const skipComfortOnboardingBtn = document.getElementById("skipComfortOnboardingBtn");

const journeyComfortSelection = document.getElementById("journeyComfortSelection");

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
  setupOnboardingListeners();
  setDefaultDepartureTime();
  ensureToastUI();
  ensureTasksModalUI();
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
  lastNotifiedJourneyId = null;
  lastTasksSignature = null;
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
    loadNamedComfortSettings();
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
  if (!currentJourney) return;
  try {
    const journey = await api.post(
      `/api/journeys/${currentJourney.journeyId}/complete`
    );
    updateCurrentJourney(journey);
    showToast("Journey completed!", { variant: "success" });
  } catch (err) {
    showToast(err.message, { variant: "warning" });
  }
}

async function cancelJourney() {
  if (!currentJourney) return;
  if (!confirm("Are you sure you want to cancel this journey?")) return;

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

    // Hide results if we have an active journey
    document.querySelector(".results-panel").classList.add("hidden");
  } else {
    // Journey finished or cancelled
    currentJourneyPanel.classList.add("hidden");
    document.querySelector(".results-panel").classList.remove("hidden");
    resultsDiv.innerHTML =
      '<p class="results-placeholder">Your journey results will appear here.</p>';
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
  const comfortSelection = journeyComfortSelection?.value || "disabled";
  const touristic = !!document.getElementById("touristic")?.checked;

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
      userId: currentUser.userId,
      originQuery: from,
      destinationQuery: to,
      departureTime: departure,
    },
    preferences: {
      comfortMode: comfortSelection !== "disabled",
      touristicMode: touristic,
      namedComfortSettingId: (comfortSelection !== "disabled") ? comfortSelection : null
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
    if (list.length > 0) notifyTasksOnRouteIfAny(list[0]);
  } catch (err) {
    if (resultsDiv)
      resultsDiv.innerHTML = '<p class="error-message">No journey found.</p>';
  }
}

/**
 * Displays journey results with shared info (tasks, modes) shown once at top
 */
function displayJourneyResults(journeys) {
  if (!resultsDiv || !journeys.length) return;

  const firstJourney = journeys[0];
  const allTasks = collectUniqueTasksFromJourneys(journeys);

  // Build header with shared info (tasks banner + modes)
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
      <span>Comfort: ${firstJourney?.comfortModeEnabled ? "On" : "Off"}</span>
      <span>Touristic: ${firstJourney?.touristicModeEnabled ? "On" : "Off"
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

  const legsHtml = processedSegments.length
    ? processedSegments
      .map(
        (seg) => `
        <li class="journey-leg-item">
            <div class="leg-marker-container">
                ${formatLineBadge(seg.mode, seg.lineCode, seg.lineColor)}
            </div>
            <div class="leg-content">
                <span class="leg-mode">${formatMode(seg.mode)}${seg.lineName ? ` <span class="leg-line-name">${escapeHtml(seg.lineName)}</span>` : ""
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
      <button class="btn btn-primary btn-sm start-journey-btn" onclick="startJourney('${journey.journeyId
    }', this)">Start Journey</button>
      <h4>Itinerary Steps:</h4>
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
  createTaskForm?.addEventListener("submit", createTask);
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
    const url = `/api/google/tasks/users/${currentUser.userId
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
    const url = `/api/google/tasks/users/${currentUser.userId
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
    const url = `/api/google/tasks/users/${currentUser.userId
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

async function createTask(e) {
  e.preventDefault();

  if (!currentUser)
    return showToast("Please log in first.", { variant: "warning" });
  if (!isGoogleLinked())
    return showToast("Link Google Tasks first.", { variant: "warning" });
  if (!defaultTaskList?.id)
    return showToast("Default list not loaded yet.", { variant: "warning" });

  const payload = {
    title: (taskTitle?.value || "").trim(),
    notes: (taskNotes?.value || "").trim() || null,
    due: (taskDue?.value || "").trim() || null,
    locationQuery: (taskLocationQuery?.value || "").trim() || null,
  };

  if (!payload.title)
    return showToast("Title is required.", { variant: "warning" });

  if (tasksResults)
    tasksResults.innerHTML = '<p class="loading">Creating task...</p>';

  try {
    const url = `/api/google/tasks/users/${currentUser.userId
      }/lists/${encodeURIComponent(defaultTaskList.id)}/tasks`;
    const created = await api.post(url, payload);

    if (created?.locationWarning) {
      showToast(
        `Task created, but location failed: ${created.locationWarning}`,
        { variant: "warning", durationMs: 6000 }
      );
    } else {
      showToast("Task created!", { variant: "success" });
    }

    createTaskForm?.reset();
    await loadTasksFromDefaultList();
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
  closeComfortProfileModal?.addEventListener("click", closeComfortProfileModalFn);

  addNewComfortSettingBtn?.addEventListener("click", () => showComfortFormView());
  backToComfortListBtn?.addEventListener("click", () => showComfortListView());

  deleteComfortSettingBtn?.addEventListener("click", () => {
    const id = editingSettingId?.value;
    if (id) deleteNamedSetting(id);
  });

  comfortProfileForm?.addEventListener("submit", saveComfortSetting);

  comfortProfileModal?.addEventListener("click", (e) => {
    if (e.target === comfortProfileModal) closeComfortProfileModalFn();
  });
}

function openComfortProfileModal() {
  if (!comfortProfileModal || !currentUser) return;

  if (!currentUser.hasSeenComfortPrompt && !hasComfortSettings(currentUser.comfortProfile)) {
    comfortOnboardingModal?.classList.remove("hidden");
  } else {
    comfortProfileModal.classList.remove("hidden");
    showComfortListView();
  }
}

function closeComfortProfileModalFn() {
  if (!comfortProfileModal) return;
  comfortProfileModal.classList.add("hidden");
  document.getElementById("comfortProfileError")?.classList.add("hidden");
  resetComfortForm();
}

function showComfortListView() {
  comfortProfileListView?.classList.remove("hidden");
  comfortProfileFormView?.classList.add("hidden");
  loadNamedComfortSettings();
}

function showComfortFormView(setting = null) {
  comfortProfileListView?.classList.add("hidden");
  comfortProfileFormView?.classList.remove("hidden");

  if (setting) {
    if (comfortFormTitle) comfortFormTitle.textContent = "Edit Setting";
    if (comfortFormSubtitle) comfortFormSubtitle.textContent = `Modifying "${setting.name}"`;
    if (editingSettingId) editingSettingId.value = setting.id;
    if (deleteComfortSettingBtn) deleteComfortSettingBtn.classList.remove("hidden");
    loadSettingIntoForm(setting);
  } else {
    if (comfortFormTitle) comfortFormTitle.textContent = "Add Setting";
    if (comfortFormSubtitle) comfortFormSubtitle.textContent = "Set your travel preferences";
    if (editingSettingId) editingSettingId.value = "";
    if (deleteComfortSettingBtn) deleteComfortSettingBtn.classList.add("hidden");
    resetComfortForm();
  }
}

function loadSettingIntoForm(setting) {
  const p = setting.comfortProfile;
  if (settingNameInput) settingNameInput.value = setting.name || "";

  const directPath = document.getElementById("directPath");
  const requireAC = document.getElementById("requireAirConditioning");
  const maxTransfers = document.getElementById("maxNbTransfers");
  const maxWaiting = document.getElementById("maxWaitingDuration");
  const maxWalking = document.getElementById("maxWalkingDuration");

  if (directPath) directPath.value = p.directPath || "";
  if (requireAC) requireAC.checked = !!p.requireAirConditioning;
  if (maxTransfers) maxTransfers.value = p.maxNbTransfers ?? "";
  if (maxWaiting) maxWaiting.value = p.maxWaitingDuration ? Math.round(p.maxWaitingDuration / 60) : "";
  if (maxWalking) maxWalking.value = p.maxWalkingDuration ? Math.round(p.maxWalkingDuration / 60) : "";
}

function resetComfortForm() {
  comfortProfileForm?.reset();
  if (editingSettingId) editingSettingId.value = "";
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

async function loadNamedComfortSettings() {
  if (!currentUser) return;

  try {
    const settings = await api.get(`/api/users/${currentUser.userId}/comfort-settings`);
    renderNamedSettings(settings);
    populateJourneyComfortDropdown(settings);
    checkComfortOnboarding(settings);
  } catch (err) {
    console.error("Failed to load named settings", err);
  }
}

function renderNamedSettings(settings) {
  if (!namedSettingsList) return;

  if (!settings || settings.length === 0) {
    namedSettingsList.innerHTML = `
      <div class="empty-state text-center py-8">
        <p class="text-muted">No saved comfort settings yet.</p>
      </div>
    `;
    return;
  }

  namedSettingsList.innerHTML = settings.map(s => {
    const p = s.comfortProfile;
    const details = [];
    if (p.directPath && p.directPath !== 'indifferent') details.push(p.directPath);
    if (p.requireAirConditioning) details.push("AC");
    if (p.maxNbTransfers !== null) details.push(`${p.maxNbTransfers} transfers`);

    return `
      <div class="named-setting-card" data-id="${s.id}">
        <div class="named-setting-info">
          <span class="named-setting-name">${escapeHtml(s.name)}</span>
          <span class="named-setting-details">${escapeHtml(details.join(", ") || "No specific constraints")}</span>
        </div>
        <div class="named-setting-actions">
          <button type="button" class="btn btn-ghost btn-sm apply-setting-btn" data-id="${s.id}" title="Preview/Apply">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M5 12h14M12 5l7 7-7 7"></path>
            </svg>
          </button>
        </div>
      </div>
    `;
  }).join("");

  namedSettingsList.querySelectorAll(".named-setting-card").forEach(card => {
    card.addEventListener("click", (e) => {
      // Don't trigger edit if the apply button was clicked specifically
      if (e.target.closest('.apply-setting-btn')) return;

      const id = card.getAttribute("data-id");
      const setting = settings.find(s => s.id === id);
      if (setting) showComfortFormView(setting);
    });
  });

  namedSettingsList.querySelectorAll(".apply-setting-btn").forEach(btn => {
    btn.addEventListener("click", () => {
      const id = btn.getAttribute("data-id");
      const setting = settings.find(s => s.id === id);
      if (setting) {
        showToast(`Applied preset: ${setting.name}`, { variant: "success" });
        // Optional: auto-fill journey setup? The request said "modify or delete" in the modal.
      }
    });
  });
}

function applyNamedSetting(setting) {
  const p = setting.comfortProfile;

  const directPath = document.getElementById("directPath");
  const requireAC = document.getElementById("requireAirConditioning");
  const maxTransfers = document.getElementById("maxNbTransfers");
  const maxWaiting = document.getElementById("maxWaitingDuration");
  const maxWalking = document.getElementById("maxWalkingDuration");

  if (directPath) directPath.value = p.directPath || "";
  if (requireAC) requireAC.checked = !!p.requireAirConditioning;
  if (maxTransfers) maxTransfers.value = p.maxNbTransfers ?? "";
  if (maxWaiting) maxWaiting.value = p.maxWaitingDuration ? Math.round(p.maxWaitingDuration / 60) : "";
  if (maxWalking) maxWalking.value = p.maxWalkingDuration ? Math.round(p.maxWalkingDuration / 60) : "";

  showToast(`Applied setting: ${setting.name}`, { variant: "success" });
}

async function saveComfortSetting(e) {
  e.preventDefault();
  if (!currentUser) return;

  const id = editingSettingId?.value;
  const name = settingNameInput?.value?.trim();
  if (!name) {
    showToast("Please provide a name for the setting", { variant: "warning" });
    settingNameInput?.focus();
    return;
  }

  const payload = {
    name: name,
    comfortProfile: {
      directPath: document.getElementById("directPath")?.value || null,
      requireAirConditioning: !!document.getElementById("requireAirConditioning")?.checked,
      maxNbTransfers: document.getElementById("maxNbTransfers")?.value ? parseInt(document.getElementById("maxNbTransfers").value, 10) : null,
      maxWaitingDuration: document.getElementById("maxWaitingDuration")?.value ? parseInt(document.getElementById("maxWaitingDuration").value, 10) * 60 : null,
      maxWalkingDuration: document.getElementById("maxWalkingDuration")?.value ? parseInt(document.getElementById("maxWalkingDuration").value, 10) * 60 : null,
    }
  };

  try {
    if (id) {
      await api.put(`/api/users/${currentUser.userId}/comfort-settings/${id}`, payload);
      showToast(`Updated setting: ${name}`, { variant: "success" });
    } else {
      await api.post(`/api/users/${currentUser.userId}/comfort-settings`, payload);
      showToast(`Saved setting: ${name}`, { variant: "success" });
    }
    showComfortListView();
  } catch (err) {
    const errorEl = document.getElementById("comfortProfileError");
    if (errorEl) {
      errorEl.textContent = err?.message || "Failed to save";
      errorEl.classList.remove("hidden");
    }
  }
}

function populateJourneyComfortDropdown(settings) {
  if (!journeyComfortSelection) return;

  const currentValue = journeyComfortSelection.value;
  let html = `
    <option value="disabled">Comfort mode Disabled</option>
  `;

  html += settings.map(s => `<option value="${s.id}">${escapeHtml(s.name)}</option>`).join("");

  journeyComfortSelection.innerHTML = html;

  if (Array.from(journeyComfortSelection.options).some(o => o.value === currentValue)) {
    journeyComfortSelection.value = currentValue;
  }
}

async function deleteNamedSetting(settingId) {
  if (!currentUser || !confirm("Delete this saved setting?")) return;

  try {
    await api.delete(`/api/users/${currentUser.userId}/comfort-settings/${settingId}`);
    showToast("Setting deleted", { variant: "success" });
    showComfortListView();
  } catch (err) {
    showToast(err.message || "Failed to delete setting", { variant: "warning" });
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
      `<li>Direct Path: ${labels[profile.directPath] || profile.directPath
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
  const comfortSelection = journeyComfortSelection?.value || "disabled";
  if (comfortSelection === "disabled") return true;

  // If a named setting is selected, it's already persisted and valid
  if (comfortSelection !== "default" && comfortSelection !== "" && comfortSelection !== "disabled") {
    return true;
  }

  const profile = currentUser?.comfortProfile;
  if (!hasComfortSettings(profile)) {
    showToast("Please configure your comfort profile first or select a saved setting.", {
      variant: "warning",
      durationMs: 5000,
    });
    openComfortProfileModal();
    return false;
  }

  return true;
}

function setupOnboardingListeners() {
  setupComfortNowBtn?.addEventListener("click", () => {
    comfortOnboardingModal?.classList.add("hidden");
    markComfortPromptSeen();
    openComfortProfileModal();
    showComfortFormView();
  });

  skipComfortOnboardingBtn?.addEventListener("click", () => {
    comfortOnboardingModal?.classList.add("hidden");
    markComfortPromptSeen();
  });
}

function checkComfortOnboarding(settings = []) {
  if (!currentUser || currentUser.hasSeenComfortPrompt) return;

  // Show onboarding if no named settings and primary profile is empty
  const hasSettings = settings.length > 0 || hasComfortSettings(currentUser.comfortProfile);

  if (!hasSettings) {
    comfortOnboardingModal?.classList.remove("hidden");
  } else {
    // If they already have settings but haven't "seen" the prompt, mark it anyway to avoid future popups
    markComfortPromptSeen();
  }
}

async function markComfortPromptSeen() {
  if (!currentUser || currentUser.hasSeenComfortPrompt) return;

  try {
    const updated = await api.post(`/api/users/${currentUser.userId}/comfort-prompt-seen`);
    currentUser.hasSeenComfortPrompt = true;
  } catch (err) {
    console.error("Failed to mark comfort prompt as seen", err);
  }
}

// ============================================================
// TASK NOTIFICATIONS
// ============================================================
function notifyTasksOnRouteIfAny(journey) {
  const tasks =
    journey && Array.isArray(journey.tasksOnRoute) ? journey.tasksOnRoute : [];
  if (!tasks.length) return;

  const journeyId = journey?.id || journey?.journeyId || null;

  // 1 popup max par trajet
  if (journeyId && lastNotifiedJourneyId === journeyId) return;

  const sig = tasks
    .map((t) =>
      String(
        t?.taskId ||
        t?.id ||
        t?.googleTaskId ||
        t?.sourceTaskId ||
        t?.title ||
        ""
      )
    )
    .filter(Boolean)
    .sort()
    .join("|");

  // éviter plusieurs toasts pour le *même trajet* (ex: double-render)
  if (journeyId && sig && lastTasksSignature === `${journeyId}:${sig}`) return;

  lastNotifiedJourneyId = journeyId;
  lastTasksSignature = journeyId ? `${journeyId}:${sig}` : sig;

  const count = tasks.length;
  const firstTitle = tasks[0]?.title || "a task";

  const msg =
    count === 1
      ? `Task on your route: ${firstTitle}`
      : `${count} tasks on your route`;

  showToast(msg, {
    variant: "warning",
    important: true,
    durationMs: 15000,
    actionText: "View",
    onAction: () => openTasksModal(tasks),
  });
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
  toast.className = `toast ${opts.variant ? `toast-${opts.variant}` : ""} ${opts.important ? "toast-important" : ""
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

function ensureTasksModalUI() {
  if (document.getElementById("tasksModal")) return;

  const overlay = document.createElement("div");
  overlay.id = "tasksModal";
  overlay.className = "tasks-modal-overlay hidden";
  overlay.setAttribute("role", "dialog");
  overlay.setAttribute("aria-modal", "true");
  overlay.setAttribute("aria-labelledby", "tasksModalTitle");

  overlay.innerHTML = `
    <div class="tasks-modal">
      <button type="button" class="tasks-modal-close" id="tasksModalClose" aria-label="Close dialog">&times;</button>
      <h3 class="tasks-modal-title" id="tasksModalTitle">Tasks on your route</h3>
      <p class="tasks-modal-subtitle">These tasks are close to your planned journey.</p>
      <div id="tasksModalList" class="tasks-modal-list"></div>
      <div class="tasks-modal-footer">
        <button type="button" class="btn btn-outline btn-sm" id="tasksModalOk">OK</button>
      </div>
    </div>
  `;

  document.body.appendChild(overlay);

  const close = () => overlay.classList.add("hidden");

  overlay.addEventListener("click", (e) => {
    if (e.target === overlay) close();
  });

  overlay.querySelector("#tasksModalClose")?.addEventListener("click", close);
  overlay.querySelector("#tasksModalOk")?.addEventListener("click", close);
}

function openTasksModal(tasks) {
  const overlay = document.getElementById("tasksModal");
  const list = document.getElementById("tasksModalList");
  if (!overlay || !list) return;

  const items = (Array.isArray(tasks) ? tasks : [])
    .map((t) => {
      const title = escapeHtml(t?.title || "Untitled");
      const dist =
        typeof t?.distanceMeters === "number"
          ? `${Math.round(t.distanceMeters)} m`
          : "—";
      return `
        <div class="tasks-modal-item">
          <div class="tasks-modal-item-title">${title}</div>
          <div class="tasks-modal-item-meta">Distance: ${dist}</div>
        </div>
      `;
    })
    .join("");

  list.innerHTML =
    items || '<div class="tasks-modal-empty">No tasks found.</div>';
  overlay.classList.remove("hidden");
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
  if (mode === "WALK") return "Walk";
  // Capitalize first letter, lowercase rest for others
  return mode.charAt(0).toUpperCase() + mode.slice(1).toLowerCase();
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
