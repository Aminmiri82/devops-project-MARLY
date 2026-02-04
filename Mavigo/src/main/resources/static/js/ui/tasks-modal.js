import { escapeHtml } from "../core/utils.js";

export function ensureTasksModalUI() {
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

export function openTasksModal(tasks) {
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

  list.innerHTML = items || '<div class="tasks-modal-empty">No tasks found.</div>';
  overlay.classList.remove("hidden");
}
