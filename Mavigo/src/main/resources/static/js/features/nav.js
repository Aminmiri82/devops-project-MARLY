import { state } from "../core/state.js";
import { updateTasksUIState, ensureDefaultTaskListLoaded } from "./tasks.js";

const navJourneyBtn = document.getElementById("navJourneyBtn");
const navTasksBtn = document.getElementById("navTasksBtn");
const navEcoScoreBtn = document.getElementById("navEcoScoreBtn");
const journeyView = document.getElementById("journeyView");
const tasksView = document.getElementById("tasksView");
const ecoScoreView = document.getElementById("ecoScoreView");

export function setupNav() {
  navJourneyBtn?.addEventListener("click", () => setView("journey"));
  navTasksBtn?.addEventListener("click", () => setView("tasks"));
  navEcoScoreBtn?.addEventListener("click", () => setView("eco-score"));

  const path = window.location.pathname || "";
  if (path.startsWith("/tasks")) {
    setView("tasks");
  } else if (path.startsWith("/eco")) {
    setView("eco-score");
  } else if (path.startsWith("/search") || path.startsWith("/results")) {
    setView("journey");
  } else {
    setView(state.currentView);
  }
}

export function setView(view) {
  state.currentView = view === "tasks" ? "tasks" : (view === "eco-score" ? "eco-score" : "journey");
  localStorage.setItem("mavigo_view", state.currentView);

  // Hide all views first
  journeyView?.classList.add("hidden");
  tasksView?.classList.add("hidden");
  ecoScoreView?.classList.add("hidden");

  // Remove active class from all nav buttons
  navJourneyBtn?.classList.remove("nav-active");
  navTasksBtn?.classList.remove("nav-active");
  navEcoScoreBtn?.classList.remove("nav-active");

  if (state.currentView === "journey") {
    journeyView?.classList.remove("hidden");
    navJourneyBtn?.classList.add("nav-active");
  } else if (state.currentView === "tasks") {
    tasksView?.classList.remove("hidden");
    navTasksBtn?.classList.add("nav-active");
    ensureDefaultTaskListLoaded({ force: false });
  } else if (state.currentView === "eco-score") {
    ecoScoreView?.classList.remove("hidden");
    navEcoScoreBtn?.classList.add("nav-active");
    import("./eco-score.js").then(module => module.refreshEcoDashboard());
  }

  updateTasksUIState();
}
