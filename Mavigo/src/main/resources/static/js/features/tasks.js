import { api } from "../core/api.js";
import { state } from "../core/state.js";
import { escapeHtml, formatDateTime } from "../core/utils.js";
import { showToast } from "../ui/toast.js";

const tasksIncludeCompleted = document.getElementById("tasksIncludeCompleted");
const tasksResults = document.getElementById("tasksResults");
const tasksListName = document.getElementById("tasksListName");
const refreshTasksBtn = document.getElementById("refreshTasksBtn");

const createTaskForm = document.getElementById("createTaskForm");
const taskTitle = document.getElementById("taskTitle");
const taskNotes = document.getElementById("taskNotes");
const taskDue = document.getElementById("taskDue");
const taskLocationQuery = document.getElementById("taskLocationQuery");

export function setupTasks() {
  refreshTasksBtn?.addEventListener("click", () =>
    ensureDefaultTaskListLoaded({ force: true })
  );
  tasksIncludeCompleted?.addEventListener("change", () =>
    loadTasksFromDefaultList()
  );
  createTaskForm?.addEventListener("submit", createTask);
  updateTasksUIState();
}

export function isGoogleLinked() {
  return !!(
    state.currentUser &&
    (state.currentUser.googleAccountLinkedAt ||
      state.currentUser.googleAccountSubject)
  );
}

export function updateTasksUIState() {
  const enabled = !!state.currentUser && isGoogleLinked();

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
    if (!state.currentUser) tasksListName.textContent = "Default list: —";
    else if (!isGoogleLinked())
      tasksListName.textContent = "Default list: (link Google Tasks)";
    else
      tasksListName.textContent = state.defaultTaskList?.title
        ? `Default list: ${state.defaultTaskList.title}`
        : "Default list: …";
  }

  if (tasksResults && state.currentView === "tasks") {
    if (!state.currentUser) {
      tasksResults.innerHTML =
        "<p class=\"results-placeholder\">Please log in to use Google Tasks.</p>";
    } else if (!isGoogleLinked()) {
      tasksResults.innerHTML =
        "<p class=\"results-placeholder\">Link Google Tasks to load your tasks.</p>";
    } else if (!state.defaultTaskList) {
      tasksResults.innerHTML =
        "<p class=\"results-placeholder\">Loading your default list…</p>";
    }
  }
}

export function resetTasksUI() {
  if (tasksListName) tasksListName.textContent = "Default list: —";
  if (tasksResults)
    tasksResults.innerHTML =
      '<p class="results-placeholder">Tasks will appear here.</p>';
}

export async function ensureDefaultTaskListLoaded({ force }) {
  if (!state.currentUser) return;
  if (!isGoogleLinked()) return updateTasksUIState();
  if (state.defaultTaskList && !force) {
    updateTasksUIState();
    return loadTasksFromDefaultList();
  }

  state.defaultTaskList = null;
  updateTasksUIState();

  try {
    const list = await api.get(
      `/api/google/tasks/users/${state.currentUser.userId}/default-list`
    );
    state.defaultTaskList = { id: list.id, title: list.title || "Default" };
    updateTasksUIState();
    await loadTasksFromDefaultList();
  } catch (err) {
    state.defaultTaskList = null;
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
  if (!state.currentUser) return;
  if (!isGoogleLinked()) return;
  if (!state.defaultTaskList?.id) return;

  if (tasksResults) tasksResults.innerHTML = '<p class="loading">Loading tasks...</p>';

  try {
    const includeCompleted = !!tasksIncludeCompleted?.checked;
    const url = `/api/google/tasks/users/${state.currentUser.userId}/lists/${encodeURIComponent(
      state.defaultTaskList.id
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
    tasksResults.innerHTML =
      '<p class="results-placeholder">No tasks in this list.</p>';
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
        ? '<button type="button" class="btn btn-success btn-sm" disabled>Completed</button>'
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
  if (!state.currentUser || !state.defaultTaskList?.id) return;

  try {
    const url = `/api/google/tasks/users/${state.currentUser.userId}/lists/${encodeURIComponent(
      state.defaultTaskList.id
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
  if (!state.currentUser || !state.defaultTaskList?.id) return;
  if (!confirm("Delete this task?")) return;

  try {
    const url = `/api/google/tasks/users/${state.currentUser.userId}/lists/${encodeURIComponent(
      state.defaultTaskList.id
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

  if (!state.currentUser)
    return showToast("Please log in first.", { variant: "warning" });
  if (!isGoogleLinked())
    return showToast("Link Google Tasks first.", { variant: "warning" });
  if (!state.defaultTaskList?.id)
    return showToast("Default list not loaded yet.", { variant: "warning" });

  const payload = {
    title: (taskTitle?.value || "").trim(),
    notes: (taskNotes?.value || "").trim() || null,
    due: (taskDue?.value || "").trim() || null,
    locationQuery: (taskLocationQuery?.value || "").trim() || null,
  };

  if (!payload.title)
    return showToast("Title is required.", { variant: "warning" });

  if (tasksResults) tasksResults.innerHTML = '<p class="loading">Creating task...</p>';

  try {
    const url = `/api/google/tasks/users/${state.currentUser.userId}/lists/${encodeURIComponent(
      state.defaultTaskList.id
    )}/tasks`;
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
