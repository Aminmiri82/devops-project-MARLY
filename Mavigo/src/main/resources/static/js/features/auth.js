import { api } from "../core/api.js";
import { state } from "../core/state.js";
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

const smartSuggestionsFlyout = document.getElementById(
  "smartSuggestionsFlyout",
);

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

  setupPasswordToggle("loginPassword", "loginPasswordToggle");
  setupPasswordToggle("registerPassword", "registerPasswordToggle");
  setupPasswordToggle("registerPasswordConfirm", "registerPasswordConfirmToggle");
}

function setupPasswordToggle(inputId, toggleId) {
  const input = document.getElementById(inputId);
  const toggle = document.getElementById(toggleId);
  if (!input || !toggle) return;

  const eyeSvg =
    '<svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/><circle cx="12" cy="12" r="3"/></svg>';
  const eyeOffSvg =
    '<svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24"/><line x1="1" y1="1" x2="23" y2="23"/></svg>';

  toggle.addEventListener("click", () => {
    const isPassword = input.type === "password";
    input.type = isPassword ? "text" : "password";
    toggle.innerHTML = isPassword ? eyeOffSvg : eyeSvg;
    toggle.setAttribute("aria-label", isPassword ? "Hide password" : "Show password");
  });
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

function validatePasswordStrength(password) {
  if (password.length < 8) return "Password must be at least 8 characters.";
  if (!/[A-Z]/.test(password)) return "Password must contain at least one uppercase letter.";
  if (!/[a-z]/.test(password)) return "Password must contain at least one lowercase letter.";
  if (!/[0-9]/.test(password)) return "Password must contain at least one digit.";
  return null;
}

async function handleLogin(e) {
  e.preventDefault();

  const email = (document.getElementById("loginEmail")?.value || "").trim();
  const password = document.getElementById("loginPassword")?.value ?? "";
  const errorEl = document.getElementById("loginError");

  if (!email) return showError(errorEl, "Please enter your email.");
  if (!password) return showError(errorEl, "Please enter your password.");

  try {
    const data = await api.post("/api/users/login", { email, password });
    const user = data.user;
    const token = data.token;
    if (token) localStorage.setItem("mavigo_token", token);
    setCurrentUser(user, { preferredView: "tasks" });
    closeAuthModal();
    loginFormEl?.reset();
  } catch (err) {
    showError(errorEl, err?.message || "Login failed");
  }
}

async function handleRegister(e) {
  e.preventDefault();

  const firstName = (document.getElementById("registerFirstName")?.value || "").trim();
  const lastName = (document.getElementById("registerLastName")?.value || "").trim();
  const email = (document.getElementById("registerEmail")?.value || "").trim();
  const password = document.getElementById("registerPassword")?.value ?? "";
  const passwordConfirm = document.getElementById("registerPasswordConfirm")?.value ?? "";
  const homeAddress = (document.getElementById("registerHomeAddress")?.value || "").trim();
  const errorEl = document.getElementById("registerError");

  if (!firstName || !lastName || !email) return showError(errorEl, "Please fill in first name, last name and email.");
  if (!password) return showError(errorEl, "Please enter a password.");
  const strengthError = validatePasswordStrength(password);
  if (strengthError) return showError(errorEl, strengthError);
  if (password !== passwordConfirm) return showError(errorEl, "Password and confirmation do not match.");

  try {
    const data = await api.post("/api/users", {
      firstName,
      lastName,
      email,
      password,
      passwordConfirm,
      homeAddress: homeAddress || undefined,
    });
    const user = data.user;
    const token = data.token;
    if (token) localStorage.setItem("mavigo_token", token);
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
  localStorage.removeItem("mavigo_token");
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
  if (user?.userId != null) localStorage.setItem("mavigo_user_id", user.userId);
  if (opts.preferredView) setView(opts.preferredView);
  updateUI();
}

export async function restoreSession() {
  const savedUserId = localStorage.getItem("mavigo_user_id");
  const token = localStorage.getItem("mavigo_token");
  if (!savedUserId || !token) return updateUI();

  try {
    const user = await api.get(`/api/users/${savedUserId}`);
    state.currentUser = user;
    updateUI();
    if (state.currentView === "tasks")
      ensureDefaultTaskListLoaded({ force: false });
  } catch (err) {
    if (err?.authError) {
      localStorage.removeItem("mavigo_token");
    }
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
