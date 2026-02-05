import { api } from "../core/api.js";
import { state } from "../core/state.js";
import { generateId } from "../core/utils.js";
import { setView } from "./nav.js";
import {
  clearGoogleLinkStatus,
  renderGoogleLinkStatus,
} from "./google-link.js";
import { renderHomeAddressStatus } from "./home-address.js";
import {
  loadNamedComfortSettings,
  renderComfortProfileSummary,
} from "./comfort-profile.js";
import { resetTasksUI, ensureDefaultTaskListLoaded } from "./tasks.js";
import { loadSmartSuggestions } from "./smart-suggestions.js";

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

const dropdownUserName = document.getElementById("dropdownUserName");
const dropdownUserEmail = document.getElementById("dropdownUserEmail");

const smartSuggestionsFlyout = document.getElementById("smartSuggestionsFlyout");

export function setupAuthListeners() {
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
  state.currentUser = null;
  state.defaultTaskList = null;
  localStorage.removeItem("mavigo_user_id");
  clearGoogleLinkStatus();
  renderHomeAddressStatus(null);
  resetTasksUI();
  if (smartSuggestionsFlyout) smartSuggestionsFlyout.classList.add("hidden");
  state.pendingSuggestionTask = null;
  state.smartSuggestionsDismissedFor = null;
  updateUI();
  state.lastNotifiedJourneyId = null;
  state.lastTasksSignature = null;
}

function setCurrentUser(user, opts = {}) {
  state.currentUser = user;
  state.defaultTaskList = null;
  if (user?.userId) localStorage.setItem("mavigo_user_id", user.userId);
  if (opts.preferredView) setView(opts.preferredView);
  updateUI();
}

export async function restoreSession() {
  const savedUserId = localStorage.getItem("mavigo_user_id");
  if (!savedUserId) return updateUI();

  try {
    const user = await api.get(`/api/users/${savedUserId}`);
    state.currentUser = user;
    updateUI();
    if (state.currentView === "tasks")
      ensureDefaultTaskListLoaded({ force: false });
  } catch {
    localStorage.removeItem("mavigo_user_id");
    state.currentUser = null;
    state.defaultTaskList = null;
    updateUI();
  }
}

export function updateUI() {
  if (state.currentUser) {
    loggedOutView?.classList.add("hidden");
    loggedInView?.classList.remove("hidden");
    if (userGreeting)
      userGreeting.textContent = `Hi, ${state.currentUser.displayName || "User"}`;
    notLoggedInPrompt?.classList.add("hidden");
    mainContent?.classList.remove("hidden");
    renderUserInfo();
    renderGoogleLinkStatus(state.currentUser);
    renderHomeAddressStatus(state.currentUser);
    renderComfortProfileSummary();
    loadNamedComfortSettings();
    setView(state.currentView);
    loadSmartSuggestions();
  } else {
    loggedOutView?.classList.remove("hidden");
    loggedInView?.classList.add("hidden");
    notLoggedInPrompt?.classList.remove("hidden");
    mainContent?.classList.remove("hidden");
    clearGoogleLinkStatus();
    renderHomeAddressStatus(null);
    resetTasksUI();
  }
}

function renderUserInfo() {
  if (dropdownUserName) {
    dropdownUserName.textContent = state.currentUser?.displayName || "—";
  }
  if (dropdownUserEmail) {
    dropdownUserEmail.textContent = state.currentUser?.email || "—";
  }
}

function showError(el, message) {
  if (!el) return;
  el.textContent = message;
  el.classList.remove("hidden");
}
