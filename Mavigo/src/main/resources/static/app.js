// State
let currentUser = null;
let currentView = "journey"; // 'journey' | 'tasks'

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
const departureInput = document.getElementById("departure");

// NAV
const navJourneyBtn = document.getElementById("navJourneyBtn");
const navTasksBtn = document.getElementById("navTasksBtn");
const journeyView = document.getElementById("journeyView");
const tasksView = document.getElementById("tasksView");

// TASKS UI
const loadListsBtn = document.getElementById("loadListsBtn");
const loadTasksBtn = document.getElementById("loadTasksBtn");
const taskListSelect = document.getElementById("taskListSelect");
const tasksIncludeCompleted = document.getElementById("tasksIncludeCompleted");
const tasksResults = document.getElementById("tasksResults");

const createTaskForm = document.getElementById("createTaskForm");
const taskTitle = document.getElementById("taskTitle");
const taskNotes = document.getElementById("taskNotes");
const taskDue = document.getElementById("taskDue");
const taskLocationQuery = document.getElementById("taskLocationQuery");

// Initialize
init();

function init() {
  setupAuthListeners();
  setupJourneyForm();
  setupDisruptionTester();
  setupGoogleLinkListeners();
  setupNav();
  setupTasks();
  setDefaultDepartureTime();
  restoreSession();

  ensureToastUI();
  ensureTasksModalUI();
}

// -----------------------------
// NAV
// -----------------------------
function setupNav() {
  navJourneyBtn.addEventListener("click", () => setView("journey"));
  navTasksBtn.addEventListener("click", () => setView("tasks"));
  setView("journey");
}

function setView(view) {
  currentView = view;

  if (view === "journey") {
    journeyView.classList.remove("hidden");
    tasksView.classList.add("hidden");
    navJourneyBtn.classList.add("nav-active");
    navTasksBtn.classList.remove("nav-active");
  } else {
    journeyView.classList.add("hidden");
    tasksView.classList.remove("hidden");
    navJourneyBtn.classList.remove("nav-active");
    navTasksBtn.classList.add("nav-active");
  }
}

// -----------------------------
// AUTH UI
// -----------------------------
function setupAuthListeners() {
  document
    .getElementById("showLoginBtn")
    .addEventListener("click", () => openAuthModal("login"));
  document
    .getElementById("showRegisterBtn")
    .addEventListener("click", () => openAuthModal("register"));
  document
    .getElementById("promptLoginBtn")
    .addEventListener("click", () => openAuthModal("login"));
  document
    .getElementById("closeAuthModal")
    .addEventListener("click", closeAuthModal);
  document.getElementById("switchToRegister").addEventListener("click", (e) => {
    e.preventDefault();
    showAuthForm("register");
  });
  document.getElementById("switchToLogin").addEventListener("click", (e) => {
    e.preventDefault();
    showAuthForm("login");
  });
  document.getElementById("logoutBtn").addEventListener("click", logout);

  authModal.addEventListener("click", (e) => {
    if (e.target === authModal) closeAuthModal();
  });

  loginFormEl.addEventListener("submit", handleLogin);
  registerFormEl.addEventListener("submit", handleRegister);
}

function openAuthModal(formType) {
  authModal.classList.remove("hidden");
  showAuthForm(formType);
}

function closeAuthModal() {
  authModal.classList.add("hidden");
  clearAuthErrors();
}

function showAuthForm(type) {
  clearAuthErrors();
  if (type === "login") {
    loginFormView.classList.remove("hidden");
    registerFormView.classList.add("hidden");
  } else {
    loginFormView.classList.add("hidden");
    registerFormView.classList.remove("hidden");
  }
}

function clearAuthErrors() {
  document.getElementById("loginError").classList.add("hidden");
  document.getElementById("registerError").classList.add("hidden");
}

async function handleLogin(e) {
  e.preventDefault();
  const email = document.getElementById("loginEmail").value.trim();
  const errorEl = document.getElementById("loginError");

  if (!email) {
    showError(errorEl, "Please enter your email.");
    return;
  }

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
    setCurrentUser(user);
    closeAuthModal();
    loginFormEl.reset();
  } catch (err) {
    showError(errorEl, err.message);
  }
}

async function handleRegister(e) {
  e.preventDefault();
  const name = document.getElementById("registerName").value.trim();
  const email = document.getElementById("registerEmail").value.trim();
  const errorEl = document.getElementById("registerError");

  if (!name || !email) {
    showError(errorEl, "Please fill in all fields.");
    return;
  }

  const payload = {
    displayName: name,
    email: email,
    externalId: generateId(),
  };

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
    setCurrentUser(user);
    closeAuthModal();
    registerFormEl.reset();
  } catch (err) {
    showError(errorEl, err.message);
  }
}

function logout() {
  currentUser = null;
  localStorage.removeItem("mavigo_user_id");
  updateUI();
}

function setCurrentUser(user) {
  currentUser = user;
  localStorage.setItem("mavigo_user_id", user.userId);
  updateUI();
}

function restoreSession() {
  const savedUserId = localStorage.getItem("mavigo_user_id");
  if (savedUserId) {
    fetch(`/api/users/${savedUserId}`)
      .then((resp) => (resp.ok ? resp.json() : Promise.reject()))
      .then((user) => {
        currentUser = user;
        updateUI();
      })
      .catch(() => {
        localStorage.removeItem("mavigo_user_id");
        updateUI();
      });
  } else {
    updateUI();
  }
}

function updateUI() {
  if (currentUser) {
    loggedOutView.classList.add("hidden");
    loggedInView.classList.remove("hidden");
    userGreeting.textContent = `Hi, ${currentUser.displayName}`;
    notLoggedInPrompt.classList.add("hidden");
    mainContent.classList.remove("hidden");
    renderUserInfo();
    renderGoogleLinkStatus(currentUser);
    setView(currentView || "journey");
  } else {
    loggedOutView.classList.remove("hidden");
    loggedInView.classList.add("hidden");
    notLoggedInPrompt.classList.remove("hidden");
    mainContent.classList.add("hidden");
  }
}

function renderUserInfo() {
  document.getElementById("displayUserName").textContent =
    currentUser.displayName;
  document.getElementById("displayUserEmail").textContent = currentUser.email;
  document.getElementById("displayUserId").textContent = currentUser.userId;
}

function showError(el, message) {
  el.textContent = message;
  el.classList.remove("hidden");
}

// -----------------------------
// JOURNEY FORM
// -----------------------------
function setupJourneyForm() {
  journeyForm.addEventListener("submit", handleJourneySubmit);
}

async function handleJourneySubmit(e) {
  e.preventDefault();

  if (!currentUser) {
    resultsDiv.innerHTML = '<p class="error-message">Please log in first.</p>';
    return;
  }

  const from = document.getElementById("from").value.trim();
  const to = document.getElementById("to").value.trim();
  const departure = departureInput.value;
  const comfort = document.getElementById("comfort").checked;
  const touristic = document.getElementById("touristic").checked;

  if (!departure) {
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

    const journey = await resp.json();
    displayJourney(journey);

    notifyTasksOnRouteIfAny(journey);
  } catch (err) {
    resultsDiv.innerHTML = `<p class="error-message">Error: ${err.message}</p>`;
  }
}

function displayJourney(journey) {
  const departure = journey.plannedDeparture
    ? formatDateTime(journey.plannedDeparture)
    : "—";
  const arrival = journey.plannedArrival
    ? formatDateTime(journey.plannedArrival)
    : "—";
  const legs = journey.legs || [];

  const legsHtml = legs.length
    ? legs
        .map(
          (leg) => `
        <li>
          <span class="leg-mode">${leg.mode || "Unknown"}</span>
          ${leg.originLabel || "?"} → ${leg.destinationLabel || "?"}
          <div class="leg-times">
            ${
              leg.estimatedDeparture
                ? formatDateTime(leg.estimatedDeparture)
                : "?"
            } -
            ${leg.estimatedArrival ? formatDateTime(leg.estimatedArrival) : "?"}
            (${leg.durationSeconds ? formatDuration(leg.durationSeconds) : "?"})
          </div>
        </li>
      `
        )
        .join("")
    : "<li>No route details available</li>";

  resultsDiv.innerHTML = `
    <div class="journey-result">
      <h3>${journey.originLabel} → ${journey.destinationLabel}</h3>
      <p class="journey-meta">Depart: ${departure} • Arrive: ${arrival}</p>
      <div class="journey-modes">
        <span>Comfort: ${journey.comfortModeEnabled ? "On" : "Off"}</span>
        <span>Touristic: ${journey.touristicModeEnabled ? "On" : "Off"}</span>
      </div>
      <ul class="journey-legs">${legsHtml}</ul>
    </div>
  `;
}

// -----------------------------
// TASKS (NEW PAGE)
// -----------------------------
function setupTasks() {
  if (loadListsBtn) loadListsBtn.addEventListener("click", loadTaskLists);
  if (loadTasksBtn)
    loadTasksBtn.addEventListener("click", loadTasksForSelectedList);
  if (createTaskForm) createTaskForm.addEventListener("submit", createTask);
}

async function loadTaskLists() {
  if (!currentUser) {
    showToast("Please log in first.", { variant: "warning" });
    return;
  }

  tasksResults.innerHTML = '<p class="loading">Loading lists...</p>';

  try {
    const resp = await fetch(
      `/api/google/tasks/users/${currentUser.userId}/lists`
    );

    // si ton backend renvoie 401 (meilleur), on gère proprement
    if (resp.status === 401 || resp.status === 403) {
      tasksResults.innerHTML = `<p class="error-message">Google Tasks not authorized. Click "Link Google Tasks".</p>`;
      showToast("Link Google Tasks first.", { variant: "warning" });
      return;
    }

    if (!resp.ok) {
      const body = await resp.text();
      throw new Error(body || "Failed to load lists");
    }

    const lists = await resp.json();
    renderTaskLists(lists);
    tasksResults.innerHTML = `<p class="results-placeholder">Lists loaded. Select one and click "Load Tasks".</p>`;
  } catch (err) {
    // ton cas CORS/302 vers Google => ça finit souvent en TypeError
    tasksResults.innerHTML = `<p class="error-message">Could not load lists. Make sure Google Tasks is linked.</p>`;
    showToast("Could not load lists (likely not linked).", {
      variant: "warning",
    });
  }
}

function renderTaskLists(lists) {
  taskListSelect.innerHTML = "";

  if (!Array.isArray(lists) || !lists.length) {
    const opt = document.createElement("option");
    opt.value = "";
    opt.textContent = "— No lists —";
    taskListSelect.appendChild(opt);
    return;
  }

  const first = document.createElement("option");
  first.value = "";
  first.textContent = "— Select a list —";
  taskListSelect.appendChild(first);

  for (const l of lists) {
    const opt = document.createElement("option");
    opt.value = l.id;
    opt.textContent = l.title || l.id;
    taskListSelect.appendChild(opt);
  }
}

async function loadTasksForSelectedList() {
  if (!currentUser) {
    showToast("Please log in first.", { variant: "warning" });
    return;
  }

  const listId = taskListSelect.value;
  if (!listId) {
    tasksResults.innerHTML = `<p class="error-message">Select a list first.</p>`;
    return;
  }

  tasksResults.innerHTML = '<p class="loading">Loading tasks...</p>';

  try {
    const includeCompleted = !!tasksIncludeCompleted.checked;
    const url = `/api/google/tasks/users/${
      currentUser.userId
    }/lists/${encodeURIComponent(
      listId
    )}/tasks?includeCompleted=${includeCompleted}`;

    const resp = await fetch(url);
    if (!resp.ok) {
      const body = await resp.text();
      throw new Error(body || "Failed to load tasks");
    }

    const tasks = await resp.json();
    renderTasks(tasks);
  } catch (err) {
    tasksResults.innerHTML = `<p class="error-message">Error: ${err.message}</p>`;
  }
}

function renderTasks(tasks) {
  if (!Array.isArray(tasks) || !tasks.length) {
    tasksResults.innerHTML = `<p class="results-placeholder">No tasks in this list.</p>`;
    return;
  }

  const items = tasks
    .map((t) => {
      const title = escapeHtml(t.title || "Untitled");
      const due = t.due ? formatDateTime(t.due) : "—";
      const status = t.status || "needsAction";

      return `
      <div class="journey-result" style="margin-bottom: 12px;">
        <h3 style="margin-bottom:6px;">${title}</h3>
        <p class="journey-meta">Due: ${due} • Status: ${escapeHtml(status)}</p>
      </div>
    `;
    })
    .join("");

  tasksResults.innerHTML = items;
}

async function createTask(e) {
  e.preventDefault();

  if (!currentUser) {
    showToast("Please log in first.", { variant: "warning" });
    return;
  }

  const listId = taskListSelect.value;
  if (!listId) {
    showToast("Select a list first.", { variant: "warning" });
    return;
  }

  const payload = {
    title: (taskTitle.value || "").trim(),
    notes: (taskNotes.value || "").trim() || null,
    due: (taskDue.value || "").trim() || null,
    locationQuery: (taskLocationQuery.value || "").trim() || null,
  };

  if (!payload.title) {
    showToast("Title is required.", { variant: "warning" });
    return;
  }

  tasksResults.innerHTML = '<p class="loading">Creating task...</p>';

  try {
    const resp = await fetch(
      `/api/google/tasks/users/${currentUser.userId}/lists/${encodeURIComponent(
        listId
      )}/tasks`,
      {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(payload),
      }
    );

    if (!resp.ok) {
      const body = await resp.text();
      throw new Error(body || "Failed to create task");
    }

    const created = await resp.json();

    if (created.locationWarning) {
      showToast(
        `Task created, but location failed: ${created.locationWarning}`,
        { variant: "warning", durationMs: 6000 }
      );
    } else {
      showToast("Task created!", { variant: "warning" });
    }

    createTaskForm.reset();
    await loadTasksForSelectedList();
  } catch (err) {
    tasksResults.innerHTML = `<p class="error-message">Error: ${err.message}</p>`;
  }
}

// -----------------------------
// GOOGLE LINK (your code)
// -----------------------------
function setupGoogleLinkListeners() {
  document
    .getElementById("linkGoogleTasksBtn")
    .addEventListener("click", startGoogleLinkFlow);
  document
    .getElementById("refreshGoogleLinkBtn")
    .addEventListener("click", refreshGoogleLink);
  window.addEventListener("message", handleGoogleLinkMessage);
}

function startGoogleLinkFlow() {
  const statusEl = document.getElementById("googleLinkStatus");

  if (!currentUser) {
    statusEl.textContent = "Please log in first.";
    return;
  }

  const linkUrl = `/api/google/tasks/link?userId=${encodeURIComponent(
    currentUser.userId
  )}`;
  const popup = window.open(linkUrl, "googleTasksLink", "width=600,height=700");

  if (!popup) {
    statusEl.textContent = "Popup blocked. Please allow popups for this site.";
    return;
  }

  statusEl.textContent = "Complete sign-in in the popup...";

  const watcher = setInterval(() => {
    if (popup.closed) {
      clearInterval(watcher);
      refreshGoogleLink();
    }
  }, 1500);
}

async function refreshGoogleLink() {
  if (!currentUser) return;

  try {
    const resp = await fetch(`/api/users/${currentUser.userId}`);
    if (resp.ok) {
      const user = await resp.json();
      currentUser = user;
      renderGoogleLinkStatus(user);
    }
  } catch (err) {
    console.error("Failed to refresh link status", err);
  }
}

function renderGoogleLinkStatus(user) {
  const statusEl = document.getElementById("googleLinkStatus");

  if (!user || !user.googleAccountLinked) {
    statusEl.textContent = "Not linked";
    statusEl.classList.remove("linked");
  } else {
    const email = user.googleAccountEmail || "your Google account";
    const linkedAt = user.googleAccountLinkedAt
      ? formatDateTime(user.googleAccountLinkedAt)
      : "";
    statusEl.textContent = `Linked to ${email}${
      linkedAt ? ` (${linkedAt})` : ""
    }`;
    statusEl.classList.add("linked");
  }
}

function handleGoogleLinkMessage(event) {
  if (event.origin !== window.location.origin) return;
  if (event.data && event.data.type === "GOOGLE_TASKS_LINKED") {
    refreshGoogleLink();
  }
}

// -----------------------------
// NOTIFICATIONS (your code unchanged)
// -----------------------------
let lastTasksSignature = null;

function notifyTasksOnRouteIfAny(journey) {
  const tasks =
    journey && Array.isArray(journey.tasksOnRoute) ? journey.tasksOnRoute : [];
  if (!tasks.length) return;

  const sig = tasks
    .map((t) => t.taskId)
    .sort()
    .join("|");
  if (sig && sig === lastTasksSignature) return;
  lastTasksSignature = sig;

  const count = tasks.length;
  const firstTitle = tasks[0]?.title || "a task";

  const msg =
    count === 1
      ? `Task on your route: ${firstTitle}`
      : `${count} tasks on your route`;

  showToast(msg, {
    variant: "warning",
    actionText: "View",
    onAction: () => openTasksModal(tasks),
  });
}

function ensureToastUI() {
  if (document.getElementById("toastContainer")) return;
  const container = document.createElement("div");
  container.id = "toastContainer";
  container.className = "toast-container";
  document.body.appendChild(container);
}

function showToast(message, opts = {}) {
  const container = document.getElementById("toastContainer");
  if (!container) return;

  const toast = document.createElement("div");
  toast.className = `toast ${
    opts.variant ? `toast-${opts.variant}` : ""
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

  const ttl = typeof opts.durationMs === "number" ? opts.durationMs : 4500;
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
  overlay.querySelector("#tasksModalClose").addEventListener("click", close);
  overlay.querySelector("#tasksModalOk").addEventListener("click", close);
}

function openTasksModal(tasks) {
  const overlay = document.getElementById("tasksModal");
  const list = document.getElementById("tasksModalList");
  if (!overlay || !list) return;

  const items = tasks
    .map((t) => {
      const title = escapeHtml(t.title || "Untitled");
      const dist =
        typeof t.distanceMeters === "number"
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

// -----------------------------
// Utilities
// -----------------------------
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
  const oneHourLater = new Date(Date.now() + 60 * 60 * 1000);
  departureInput.value = oneHourLater.toISOString().slice(0, 16);
}

function formatDuration(seconds) {
  if (!seconds && seconds !== 0) return "?";
  const h = Math.floor(seconds / 3600);
  const m = Math.floor((seconds % 3600) / 60);
  return h > 0 ? `${h}h ${m}m` : `${m}m`;
}

function formatDateTime(dt) {
  return new Date(dt).toLocaleString();
}

function generateId() {
  if (window.crypto && typeof window.crypto.randomUUID === "function") {
    return window.crypto.randomUUID();
  }
  return `user-${Date.now()}`;
}

function setupDisruptionTester() {
  // No-op in provided snippet
}
