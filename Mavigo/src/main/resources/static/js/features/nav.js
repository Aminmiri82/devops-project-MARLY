import { state } from "../core/state.js";
import { updateTasksUIState, ensureDefaultTaskListLoaded } from "./tasks.js";

const navJourneyBtn = document.getElementById("navJourneyBtn");
const navTasksBtn = document.getElementById("navTasksBtn");
const journeyView = document.getElementById("journeyView");
const tasksView = document.getElementById("tasksView");

export function setupNav() {
  navJourneyBtn?.addEventListener("click", () => setView("journey"));
  navTasksBtn?.addEventListener("click", () => setView("tasks"));
  const path = window.location.pathname || "";
  if (path.startsWith("/tasks")) {
    setView("tasks");
  } else if (path.startsWith("/search") || path.startsWith("/results")) {
    setView("journey");
  } else {
    setView(state.currentView);
  }
}

export function setView(view) {
  state.currentView = view === "tasks" ? "tasks" : "journey";
  localStorage.setItem("mavigo_view", state.currentView);

  if (state.currentView === "journey") {
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
  if (state.currentView === "tasks") ensureDefaultTaskListLoaded({ force: false });
}
