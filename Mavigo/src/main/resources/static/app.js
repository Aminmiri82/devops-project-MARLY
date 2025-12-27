let currentUser = null;
let currentView = localStorage.getItem("mavigo_view") || "journey";
let defaultTaskList = null;
let currentJourney = null;

// DOM Elements
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
const fetchShortDisruptionsBtn = document.getElementById("fetchShortDisruptionsBtn");

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

init();

function init() {
  setupAuthListeners();
  setupJourneyForm();
  setupJourneyActions();
  setupGoogleLinkListeners();
  setupNav();
  setupTasks();
  setDefaultDepartureTime();
  ensureToastUI();
  ensureTasksModalUI();
  restoreSession();
}

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
    const resp = await fetch("/api/users/login", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ email }),
    });

    if (!resp.ok) {
      const body = await resp.text();
      throw new Error(body || "Login failed");
    }

    const user = await resp.json();
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
    const resp = await fetch("/api/users", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(payload),
    });

    if (!resp.ok) {
      const body = await resp.text();
      throw new Error(body || "Registration failed");
    }

    const user = await resp.json();
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

function restoreSession() {
  const savedUserId = localStorage.getItem("mavigo_user_id");
  if (!savedUserId) return updateUI();

  fetch(`/api/users/${savedUserId}`)
    .then((resp) => (resp.ok ? resp.json() : Promise.reject(resp)))
    .then((user) => {
      currentUser = user;
      updateUI();
      if (currentView === "tasks")
        ensureDefaultTaskListLoaded({ force: false });
    })
    .catch(() => {
      localStorage.removeItem("mavigo_user_id");
      currentUser = null;
      defaultTaskList = null;
      updateUI();
    });
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
  document.getElementById("displayUserName") &&
    (document.getElementById("displayUserName").textContent =
      currentUser?.displayName || "—");
  document.getElementById("displayUserEmail") &&
    (document.getElementById("displayUserEmail").textContent =
      currentUser?.email || "—");
  document.getElementById("displayUserId") &&
    (document.getElementById("displayUserId").textContent =
      currentUser?.userId || "—");
}

function showError(el, message) {
  if (!el) return;
  el.textContent = message;
  el.classList.remove("hidden");
}

function setupJourneyForm() {
  journeyForm?.addEventListener("submit", handleJourneySubmit);
}

function setupJourneyActions() {
    completeJourneyBtn.addEventListener('click', completeJourney);
    cancelJourneyBtn.addEventListener('click', cancelJourney);
    if (reportDisruptionBtn) {
        reportDisruptionBtn.addEventListener('click', reportDisruption);
    }
}

async function startJourney(journeyId, btnElement) {
    if (!currentUser) return;

    // Collect all start buttons
    const allButtons = document.querySelectorAll('.start-journey-btn');

    if (btnElement) {
        btnElement.disabled = true;
        btnElement.textContent = 'Starting...';
    }

    // Hide other buttons
    allButtons.forEach(btn => {
        if (btn !== btnElement) {
            btn.classList.add('hidden');
        }
    });

    try {
        const url = `/api/journeys/${journeyId}/start`;
        console.log('Fetching:', url);
        const resp = await fetch(url, { method: 'POST' });
        if (!resp.ok) {
            const body = await resp.text();
            throw new Error(body || 'Failed to start journey');
        }
        const journey = await resp.json();
        updateCurrentJourney(journey);
    } catch (err) {
        alert(err.message);
        // Restore buttons on error
        allButtons.forEach(btn => {
            btn.classList.remove('hidden');
            btn.disabled = false;
            if (btn === btnElement) {
                btn.textContent = 'Start Journey';
            }
        });
    }
}

async function completeJourney() {
    if (!currentJourney) return;
    try {
        const url = `/api/journeys/${currentJourney.journeyId}/complete`;
        console.log('Fetching:', url);
        const resp = await fetch(url, { method: 'POST' });
        if (!resp.ok) {
            const body = await resp.text();
            throw new Error(body || 'Failed to complete journey');
        }
        const journey = await resp.json();
        updateCurrentJourney(journey);
        alert('Journey completed!');
    } catch (err) {
        alert(err.message);
    }
}

async function cancelJourney() {
    if (!currentJourney) return;
    if (!confirm('Are you sure you want to cancel this journey?')) return;

    try {
        const url = `/api/journeys/${currentJourney.journeyId}/cancel`;
        console.log('Fetching:', url);
        const resp = await fetch(url, { method: 'POST' });
        if (!resp.ok) {
            const body = await resp.text();
            throw new Error(body || 'Failed to cancel journey');
        }
        const journey = await resp.json();
        updateCurrentJourney(journey);
    } catch (err) {
        alert(err.message);
    }
}

function updateCurrentJourney(journey) {
    currentJourney = journey;

    if (journey && (journey.status === 'PLANNED' || journey.status === 'IN_PROGRESS' || journey.status === 'REROUTED')) {
        currentJourneyPanel.classList.remove('hidden');
        renderCurrentJourney(journey);

        // Hide results if we have an active journey
        document.querySelector('.results-panel').classList.add('hidden');
    } else {
        // Journey finished or cancelled
        currentJourneyPanel.classList.add('hidden');
        document.querySelector('.results-panel').classList.remove('hidden');
        resultsDiv.innerHTML = '<p class="results-placeholder">Your journey results will appear here.</p>';
        currentJourney = null;
    }
}

function calculateProgress(journey) {
    if (journey.status !== 'IN_PROGRESS' && journey.status !== 'REROUTED') return 0;

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
    const statusClass = journey.status === 'IN_PROGRESS' ? 'status-active' : 'status-planned';
    const progress = calculateProgress(journey);

    currentJourneyContent.innerHTML = `
        <div class="journey-status-card">
            <div class="status-badge ${statusClass}">${journey.status}</div>
            ${journey.status === 'REROUTED' || journey.disruptionCount > 0 ? '<div class="disruption-warning">⚠️ Disruption : New Journey Started</div>' : ''}
            <h3>${journey.originLabel} → ${journey.destinationLabel}</h3>
            
            ${journey.status === 'IN_PROGRESS' || journey.status === 'REROUTED' ? `
                <div class="progress-container">
                    <div class="progress-bar" style="width: ${progress}%"></div>
                </div>
                <span class="progress-text">${progress}% Completed</span>
            ` : ''}

            <p><strong>Planned Departure:</strong> ${formatDateTime(journey.plannedDeparture)}</p>
            ${journey.actualDeparture ? `<p><strong>Started:</strong> ${formatDateTime(journey.actualDeparture)}</p>` : ''}
            <p><strong>Planned Arrival:</strong> ${formatDateTime(journey.plannedArrival)}</p>
        </div>
    `;

    if (journey.status === 'IN_PROGRESS' || journey.status === 'REROUTED') {
        completeJourneyBtn.classList.remove('hidden');
        cancelJourneyBtn.classList.remove('hidden');
        if (reportDisruptionBtn) reportDisruptionBtn.classList.remove('hidden');
    } else {
        completeJourneyBtn.classList.add('hidden');
        cancelJourneyBtn.classList.add('hidden');
        if (reportDisruptionBtn) reportDisruptionBtn.classList.add('hidden');
    }
}

async function reportDisruption() {
    if (!currentJourney) return;

    // Ask user for rerouting method
    const choice = confirm("Report disruption: Use current GPS location? (Click 'OK' for GPS, 'Cancel' to enter a station name)");

    let lat = '';
    let lng = '';
    let manualOrigin = '';

    if (choice) {
        // Use GPS
        const getPosition = () => new Promise((resolve, reject) => {
            if (!navigator.geolocation) {
                reject(new Error('Geolocation is not supported by your browser'));
            } else {
                navigator.geolocation.getCurrentPosition(resolve, reject);
            }
        });

        try {
            const position = await getPosition();
            lat = position.coords.latitude;
            lng = position.coords.longitude;
            console.log('Got user location:', lat, lng);
        } catch (geoErr) {
            console.warn('Could not get location:', geoErr);
            if (confirm("Could not get GPS location. Enter a station manually?")) {
                manualOrigin = prompt("Enter new departure station:");
                if (!manualOrigin) return;
            } else {
                return;
            }
        }
    } else {
        // Manual entry
        manualOrigin = prompt("Enter new departure station:");
        if (!manualOrigin) return;
    }

    try {
        const creator = currentUser ? currentUser.displayName : 'Anonymous';
        let url = `/perturbations/apply?journeyId=${currentJourney.journeyId}&creator=${encodeURIComponent(creator)}`;

        if (lat && lng) {
            url += `&userLat=${lat}&userLng=${lng}`;
        } else if (manualOrigin) {
            url += `&newOrigin=${encodeURIComponent(manualOrigin)}`;
        }

        const resp = await fetch(url, { method: 'POST' });
        if (!resp.ok) {
            const body = await resp.text();
            throw new Error(body || 'Failed to report disruption');
        }

        const newJourneys = await resp.json();

        // Switch back to results view to let user choose
        currentJourneyPanel.classList.add('hidden');
        document.querySelector('.results-panel').classList.remove('hidden');

        resultsDiv.innerHTML = '';
        if (newJourneys && newJourneys.length > 0) {
            newJourneys.forEach(displayJourney);
            alert('Disruption reported. Please choose an alternative route from the list below.');
        } else {
            alert('Disruption reported, but no alternative routes found.');
        }
    } catch (err) {
        // Simple warning as requested if journey cannot be found or location is invalid
        const msg = err.message || "";
        if (msg.includes("No places") || msg.includes("No stop area") || msg.includes("No journey options") || msg.includes("Failed to calculate journey")) {
            alert("No journey found.");
        } else {
            alert("No journey found. (Technical detail: " + msg + ")");
        }
    }
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

  const payload = {
    journey: {
      userId: currentUser.userId,
      originQuery: from,
      destinationQuery: to,
      departureTime: departure,
    },
    preferences: {
      comfortMode: comfort,
      touristicMode: touristic,
    },
  };

  if (resultsDiv)
    resultsDiv.innerHTML = '<p class="loading">Planning your journey...</p>';

  try {
    const resp = await fetch("/api/journeys", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(payload),
    });

    if (!resp.ok) {
      const body = await resp.text();
      throw new Error(body || "Failed to plan journey");
    }

    const journeys = await resp.json();

    // Clear previous results
    resultsDiv.innerHTML = '';

    // Handle both single object (legacy) and array
    const list = Array.isArray(journeys) ? journeys : [journeys];

    if (list.length === 0) {
      resultsDiv.innerHTML = '<p class="error-message">No journey found.</p>';
      return;
    }

    // Display all journeys
    list.forEach(displayJourney);

    // Notify for tasks on route for the first journey (or all if you prefer)
    if (list.length > 0) {
      notifyTasksOnRouteIfAny(list[0]);
    }
  } catch (err) {
    if (resultsDiv)
      resultsDiv.innerHTML = `<p class="error-message">No journey found.</p>`;
    console.error("Journey planning error:", err);
  }
}

function displayJourney(journey) {
  if (!resultsDiv) return;

  const departure = journey?.plannedDeparture
    ? formatDateTime(journey.plannedDeparture)
    : "—";
  const arrival = journey?.plannedArrival
    ? formatDateTime(journey.plannedArrival)
    : "—";
  const legs = Array.isArray(journey?.legs) ? journey.legs : [];

  // Process legs based on rules (from reroutage-task):
  // 1. If duration < 60s AND origin == destination -> Filter out
  // 2. If duration >= 60s AND origin == destination AND mode == 'OTHER' -> Change mode to 'WALK'
  const processedLegs = legs.filter(leg => {
    const duration = leg.durationSeconds || 0;
    const samePlace = leg.originLabel === leg.destinationLabel;
    if (duration < 60 && samePlace) return false;
    return true;
  }).map(leg => {
    const duration = leg.durationSeconds || 0;
    const samePlace = leg.originLabel === leg.destinationLabel;
    if (duration >= 60 && samePlace && leg.mode === 'OTHER') {
      // Return a copy with modified mode
      return { ...leg, mode: 'WALK' };
    }
    return leg;
  });

  const legsHtml = processedLegs.length
    ? processedLegs.map(leg => `
        <li class="journey-leg-item">
            <div class="leg-marker"></div>
            <div class="leg-content">
                <span class="leg-mode">${formatMode(leg.mode)} ${leg.lineCode ? `<span class="leg-line">${leg.lineCode}</span>` : ''}</span>
                <span class="leg-route">${leg.originLabel || '?'} → ${leg.destinationLabel || '?'}</span>
                <div class="leg-times">
                    ${leg.estimatedDeparture ? formatDateTime(leg.estimatedDeparture) : '?'} -
                    ${leg.estimatedArrival ? formatDateTime(leg.estimatedArrival) : '?'}
                    <span class="leg-duration">(${leg.durationSeconds ? formatDuration(leg.durationSeconds) : '?'})</span>
                </div>
            </div>
        </li>
    `).join('')
    : '<li>No route details available</li>';

  // Tasks on route banner (from fix-task-api)
  const tasks = Array.isArray(journey?.tasksOnRoute)
    ? journey.tasksOnRoute
    : [];
  const tasksBannerHtml = tasks.length
    ? `
      <div class="tasks-on-route-banner">
        <div>
          <div class="tasks-on-route-title">${tasks.length} task${
        tasks.length > 1 ? "s" : ""
      } on your route</div>
          <div class="tasks-on-route-sub">${escapeHtml(
            tasks[0]?.title || "Task"
          )}</div>
        </div>
        <button type="button" class="btn btn-outline btn-sm" id="viewTasksOnRouteBtn_${journey.journeyId}">View</button>
      </div>
    `
    : "";

  const html = `
    <div class="journey-result">
      <h3>${escapeHtml(journey?.originLabel || "—")} → ${escapeHtml(
    journey?.destinationLabel || "—"
  )}</h3>
      <p class="journey-meta">Depart: ${departure} • Arrive: ${arrival}</p>

      ${tasksBannerHtml}

      <div class="journey-modes">
        <span>Comfort: ${journey?.comfortModeEnabled ? "On" : "Off"}</span>
        <span>Touristic: ${journey?.touristicModeEnabled ? "On" : "Off"}</span>
      </div>
      <button class="btn btn-primary btn-sm start-journey-btn" onclick="startJourney('${journey.journeyId}', this)">Start Journey</button>
      <h4>Itinerary Steps:</h4>
      <ul class="journey-legs">${legsHtml}</ul>
    </div>
  `;

  resultsDiv.insertAdjacentHTML('beforeend', html);

  // Attach event listener for tasks modal if there are tasks
  if (tasks.length) {
    document
      .getElementById(`viewTasksOnRouteBtn_${journey.journeyId}`)
      ?.addEventListener("click", () => openTasksModal(tasks));
  }
}

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
    const resp = await fetch(
      `/api/google/tasks/users/${currentUser.userId}/default-list`
    );

    if (resp.status === 401 || resp.status === 403 || resp.status === 409) {
      defaultTaskList = null;
      if (tasksResults)
        tasksResults.innerHTML = `<p class="error-message">Google Tasks not authorized. Click "Link Google Tasks".</p>`;
      showToast("Link Google Tasks first.", { variant: "warning" });
      updateTasksUIState();
      return;
    }

    if (!resp.ok) {
      const body = await resp.text();
      throw new Error(body || "Failed to load default list");
    }

    const list = await resp.json();
    defaultTaskList = { id: list.id, title: list.title || "Default" };
    updateTasksUIState();
    await loadTasksFromDefaultList();
  } catch (err) {
    defaultTaskList = null;
    if (tasksResults)
      tasksResults.innerHTML = `<p class="error-message">Could not load default list.</p>`;
    showToast(err?.message || "Could not load default list.", {
      variant: "warning",
    });
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

    const resp = await fetch(url);

    if (resp.status === 401 || resp.status === 403 || resp.status === 409) {
      if (tasksResults)
        tasksResults.innerHTML = `<p class="error-message">Google Tasks not authorized. Click "Link Google Tasks".</p>`;
      showToast("Link Google Tasks first.", { variant: "warning" });
      return;
    }

    if (!resp.ok) {
      const body = await resp.text();
      throw new Error(body || "Failed to load tasks");
    }

    const tasks = await resp.json();
    renderTasks(tasks);
  } catch (err) {
    if (tasksResults)
      tasksResults.innerHTML = `<p class="error-message">Error: ${escapeHtml(
        err?.message || "Unknown error"
      )}</p>`;
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
      )}</p>
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
    const resp = await fetch(
      `/api/google/tasks/users/${currentUser.userId}/lists/${encodeURIComponent(
        defaultTaskList.id
      )}/tasks/${encodeURIComponent(taskId)}/complete`,
      { method: "PATCH" }
    );

    if (resp.status === 401 || resp.status === 403 || resp.status === 409) {
      showToast("Link Google Tasks first.", { variant: "warning" });
      return;
    }

    if (!resp.ok) {
      const body = await resp.text();
      throw new Error(body || "Failed to complete task");
    }

    showToast("Task completed!", { variant: "success" });
    await loadTasksFromDefaultList();
  } catch (err) {
    showToast(err?.message || "Failed to complete task", {
      variant: "warning",
    });
  }
}

async function deleteTask(taskId) {
  if (!currentUser || !defaultTaskList?.id) return;

  if (!confirm("Delete this task?")) return;

  try {
    const resp = await fetch(
      `/api/google/tasks/users/${currentUser.userId}/lists/${encodeURIComponent(
        defaultTaskList.id
      )}/tasks/${encodeURIComponent(taskId)}`,
      { method: "DELETE" }
    );

    if (resp.status === 401 || resp.status === 403 || resp.status === 409) {
      showToast("Link Google Tasks first.", { variant: "warning" });
      return;
    }

    if (!resp.ok) {
      const body = await resp.text();
      throw new Error(body || "Failed to delete task");
    }

    showToast("Task deleted.", { variant: "success" });
    await loadTasksFromDefaultList();
  } catch (err) {
    showToast(err?.message || "Failed to delete task", { variant: "warning" });
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
    const resp = await fetch(
      `/api/google/tasks/users/${currentUser.userId}/lists/${encodeURIComponent(
        defaultTaskList.id
      )}/tasks`,
      {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(payload),
      }
    );

    if (resp.status === 401 || resp.status === 403 || resp.status === 409) {
      if (tasksResults)
        tasksResults.innerHTML = `<p class="error-message">Google Tasks not authorized. Click "Link Google Tasks".</p>`;
      showToast("Link Google Tasks first.", { variant: "warning" });
      return;
    }

    if (!resp.ok) {
      const body = await resp.text();
      throw new Error(body || "Failed to create task");
    }

    const created = await resp.json();

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
    if (tasksResults)
      tasksResults.innerHTML = `<p class="error-message">Error: ${escapeHtml(
        err?.message || "Unknown error"
      )}</p>`;
  }
}

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
  const popup = window.open(linkUrl, "googleTasksLink", "width=600,height=700");

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
  }, 1200);
}

async function refreshGoogleLink() {
  if (!currentUser?.userId) return;

  try {
    const resp = await fetch(`/api/users/${currentUser.userId}`);
    if (!resp.ok) return;

    const user = await resp.json();
    currentUser = user;
    renderGoogleLinkStatus(user);
    updateTasksUIState();
    if (currentView === "tasks") ensureDefaultTaskListLoaded({ force: true });
  } catch (_) {}
}

function clearGoogleLinkStatus() {
  const statusEl = document.getElementById("googleLinkStatus");
  if (!statusEl) return;
  statusEl.textContent = "";
  statusEl.classList.remove("linked");
}

function renderGoogleLinkStatus(user) {
  const statusEl = document.getElementById("googleLinkStatus");
  if (!statusEl) return;
  if (!user) return clearGoogleLinkStatus();

  const linked = !!(user.googleAccountLinkedAt || user.googleAccountSubject);

  if (!linked) {
    statusEl.textContent = "Compte Google non lié.";
    statusEl.classList.remove("linked");
  } else {
    statusEl.textContent = "Compte Google lié.";
    statusEl.classList.add("linked");
  }
}

function handleGoogleLinkMessage(event) {
  if (event.origin !== window.location.origin) return;
  if (event.data && event.data.type === "GOOGLE_TASKS_LINKED")
    refreshGoogleLink();
}

let lastNotifiedJourneyId = null;
let lastTasksSignature = null;

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
      ? 12000
      : 4500;
  const timer = setTimeout(() => removeToast(toast), ttl);

  toast.addEventListener("mouseenter", () => clearTimeout(timer));
}

function removeToast(toastEl) {
  if (!toastEl) return;
  toastEl.classList.add("toast-hide");
  setTimeout(() => toastEl.remove(), 200);
}

function ensureTasksModalUI() {
  if (document.getElementById("tasksModal")) return;

  const overlay = document.createElement("div");
  overlay.id = "tasksModal";
  overlay.className = "tasks-modal-overlay hidden";

  overlay.innerHTML = `
    <div class="tasks-modal">
      <button type="button" class="tasks-modal-close" id="tasksModalClose">&times;</button>
      <h3 class="tasks-modal-title">Tasks on your route</h3>
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
    if (!mode) return 'Unknown';
    if (mode === 'OTHER') return 'Connection';
    if (mode === 'WALK') return 'Walk';
    // Capitalize first letter, lowercase rest for others
    return mode.charAt(0).toUpperCase() + mode.slice(1).toLowerCase();
}

function generateId() {
  if (window.crypto && typeof window.crypto.randomUUID === "function")
    return window.crypto.randomUUID();
  return `user-${Date.now()}`;
}
