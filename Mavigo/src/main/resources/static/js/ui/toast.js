import { CONFIG } from "../core/config.js";

export function ensureToastUI() {
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

export function showToast(message, opts = {}) {
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

export function removeToast(toastEl) {
  if (!toastEl) return;
  toastEl.classList.add("toast-hide");
  setTimeout(() => toastEl.remove(), CONFIG.TOAST_HIDE_DELAY_MS);
}
