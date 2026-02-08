import { api } from "../core/api.js";
import { state } from "../core/state.js";
import { escapeHtml } from "../core/utils.js";
import { showToast } from "../ui/toast.js";

const comfortProfileModal = document.getElementById("comfortProfileModal");
const comfortProfileForm = document.getElementById("comfortProfileForm");
const comfortProfileSummary = document.getElementById("comfortProfileSummary");
const editComfortProfileBtn = document.getElementById("editComfortProfileBtn");
const closeComfortProfileModal = document.getElementById(
  "closeComfortProfileModal"
);

const comfortProfileListView = document.getElementById("comfortProfileListView");
const comfortProfileFormView = document.getElementById("comfortProfileFormView");
const namedSettingsList = document.getElementById("namedSettingsList");
const addNewComfortSettingBtn = document.getElementById(
  "addNewComfortSettingBtn"
);
const backToComfortListBtn = document.getElementById("backToComfortListBtn");
const comfortFormTitle = document.getElementById("comfortFormTitle");
const comfortFormSubtitle = document.getElementById("comfortFormSubtitle");
const editingSettingId = document.getElementById("editingSettingId");
const deleteComfortSettingBtn = document.getElementById(
  "deleteComfortSettingBtn"
);
const settingNameInput = document.getElementById("settingName");

const comfortOnboardingModal = document.getElementById("comfortOnboardingModal");
const setupComfortNowBtn = document.getElementById("setupComfortNowBtn");
const skipComfortOnboardingBtn = document.getElementById(
  "skipComfortOnboardingBtn"
);

const journeyComfortSelection = document.getElementById(
  "journeyComfortSelection"
);

export function setupComfortProfileListeners() {
  editComfortProfileBtn?.addEventListener("click", openComfortProfileModal);
  closeComfortProfileModal?.addEventListener(
    "click",
    closeComfortProfileModalFn
  );

  addNewComfortSettingBtn?.addEventListener("click", () =>
    showComfortFormView()
  );
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
  if (!comfortProfileModal || !state.currentUser) return;

  if (
    !state.currentUser.hasSeenComfortPrompt &&
    !hasComfortSettings(state.currentUser.comfortProfile)
  ) {
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
    if (comfortFormSubtitle)
      comfortFormSubtitle.textContent = `Modifying "${setting.name}"`;
    if (editingSettingId) editingSettingId.value = setting.id;
    if (deleteComfortSettingBtn)
      deleteComfortSettingBtn.classList.remove("hidden");
    loadSettingIntoForm(setting);
  } else {
    if (comfortFormTitle) comfortFormTitle.textContent = "Add Setting";
    if (comfortFormSubtitle)
      comfortFormSubtitle.textContent = "Set your travel preferences";
    if (editingSettingId) editingSettingId.value = "";
    if (deleteComfortSettingBtn)
      deleteComfortSettingBtn.classList.add("hidden");
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
  if (maxWaiting)
    maxWaiting.value = p.maxWaitingDuration
      ? Math.round(p.maxWaitingDuration / 60)
      : "";
  if (maxWalking)
    maxWalking.value = p.maxWalkingDuration
      ? Math.round(p.maxWalkingDuration / 60)
      : "";
}

function resetComfortForm() {
  comfortProfileForm?.reset();
  if (editingSettingId) editingSettingId.value = "";
}

async function saveComfortProfile(e) {
  e.preventDefault();
  if (!state.currentUser) return;

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
      `/api/users/${state.currentUser.userId}/comfort-profile`,
      payload
    );
    state.currentUser.comfortProfile = updated;
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
  if (!state.currentUser) return;
  if (!confirm("Clear all comfort profile settings?")) return;

  try {
    await api.delete(`/api/users/${state.currentUser.userId}/comfort-profile`);
    state.currentUser.comfortProfile = null;
    renderComfortProfileSummary();
    closeComfortProfileModalFn();
    showToast("Comfort profile cleared.", { variant: "success" });
  } catch (err) {
    showToast(err?.message || "Failed to clear profile", {
      variant: "warning",
    });
  }
}

export async function loadNamedComfortSettings() {
  if (!state.currentUser) return;

  try {
    const settings = await api.get(
      `/api/users/${state.currentUser.userId}/comfort-settings`
    );
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

  namedSettingsList.innerHTML = settings
    .map((s) => {
      const p = s.comfortProfile;
      const details = [];
      if (p.directPath && p.directPath !== "indifferent")
        details.push(p.directPath);
      if (p.requireAirConditioning) details.push("AC");
      if (p.maxNbTransfers !== null) details.push(`${p.maxNbTransfers} transfers`);

      return `
      <div class="named-setting-card" data-id="${s.id}">
        <div class="named-setting-info">
          <span class="named-setting-name">${escapeHtml(s.name)}</span>
          <span class="named-setting-details">${escapeHtml(
        details.join(", ") || "No specific constraints"
      )}</span>
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
    })
    .join("");

  namedSettingsList
    .querySelectorAll(".named-setting-card")
    .forEach((card) => {
      card.addEventListener("click", (e) => {
        if (e.target.closest(".apply-setting-btn")) return;

        const id = card.getAttribute("data-id");
        const setting = settings.find((s) => s.id === id);
        if (setting) showComfortFormView(setting);
      });
    });

  namedSettingsList
    .querySelectorAll(".apply-setting-btn")
    .forEach((btn) => {
      btn.addEventListener("click", () => {
        const id = btn.getAttribute("data-id");
        const setting = settings.find((s) => s.id === id);
        if (setting) {
          showToast(`Applied preset: ${setting.name}`, { variant: "success" });
        }
      });
    });
}

function applyNamedSetting(setting) {
  const p = setting.comfortProfile;

  const directPath = document.getElementById("directPath");
  const requireAC = document.getElementById("requireAirConditioning");
  const wheelchair = document.getElementById("wheelchairAccessible");
  const maxTransfers = document.getElementById("maxNbTransfers");
  const maxWaiting = document.getElementById("maxWaitingDuration");
  const maxWalking = document.getElementById("maxWalkingDuration");

  if (directPath) directPath.value = p.directPath || "";
  if (requireAC) requireAC.checked = !!p.requireAirConditioning;
  if (wheelchair) wheelchair.checked = !!p.wheelchairAccessible;
  if (maxTransfers) maxTransfers.value = p.maxNbTransfers ?? "";
  if (maxWaiting)
    maxWaiting.value = p.maxWaitingDuration
      ? Math.round(p.maxWaitingDuration / 60)
      : "";
  if (maxWalking)
    maxWalking.value = p.maxWalkingDuration
      ? Math.round(p.maxWalkingDuration / 60)
      : "";

  showToast(`Applied setting: ${setting.name}`, { variant: "success" });
}

async function saveComfortSetting(e) {
  e.preventDefault();
  if (!state.currentUser) return;

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
      requireAirConditioning: !!document.getElementById(
        "requireAirConditioning"
      )?.checked,
      maxNbTransfers: document.getElementById("maxNbTransfers")?.value
        ? parseInt(document.getElementById("maxNbTransfers").value, 10)
        : null,
      maxWaitingDuration: document.getElementById("maxWaitingDuration")?.value
        ? parseInt(document.getElementById("maxWaitingDuration").value, 10) *
        60
        : null,
      maxWalkingDuration: document.getElementById("maxWalkingDuration")?.value
        ? parseInt(document.getElementById("maxWalkingDuration").value, 10) *
        60
        : null,
    },
  };

  try {
    if (id) {
      await api.put(
        `/api/users/${state.currentUser.userId}/comfort-settings/${id}`,
        payload
      );
      showToast(`Updated setting: ${name}`, { variant: "success" });
    } else {
      await api.post(
        `/api/users/${state.currentUser.userId}/comfort-settings`,
        payload
      );
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
    <option value="disabled">No Profile</option>
  `;

  html += settings
    .map((s) => `<option value="${s.id}">${escapeHtml(s.name)}</option>`)
    .join("");

  journeyComfortSelection.innerHTML = html;

  if (
    Array.from(journeyComfortSelection.options).some(
      (o) => o.value === currentValue
    )
  ) {
    journeyComfortSelection.value = currentValue;
  }
}

async function deleteNamedSetting(settingId) {
  if (!state.currentUser || !confirm("Delete this saved setting?")) return;

  try {
    await api.delete(
      `/api/users/${state.currentUser.userId}/comfort-settings/${settingId}`
    );
    showToast("Setting deleted", { variant: "success" });
    showComfortListView();
  } catch (err) {
    showToast(err.message || "Failed to delete setting", {
      variant: "warning",
    });
  }
}

export function renderComfortProfileSummary() {
  if (!comfortProfileSummary) return;

  const profile = state.currentUser?.comfortProfile;

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
      `<li>Direct Path: ${labels[profile.directPath] || profile.directPath}</li>`
    );
  }

  if (profile.requireAirConditioning) {
    items.push("<li>Require Air Conditioning: Yes</li>");
  }

  if (profile.wheelchairAccessible) {
    items.push("<li>Wheelchair Accessible: Yes</li>");
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
    profile.wheelchairAccessible === true ||
    profile.maxNbTransfers != null ||
    profile.maxWaitingDuration != null ||
    profile.maxWalkingDuration != null
  );
}

export function validateComfortMode() {
  const comfortSelection = journeyComfortSelection?.value || "disabled";
  if (comfortSelection === "disabled") return true;

  if (
    comfortSelection !== "default" &&
    comfortSelection !== "" &&
    comfortSelection !== "disabled"
  ) {
    return true;
  }

  const profile = state.currentUser?.comfortProfile;
  if (!hasComfortSettings(profile)) {
    showToast(
      "Please configure your comfort profile first or select a saved setting.",
      {
        variant: "warning",
        durationMs: 5000,
      }
    );
    openComfortProfileModal();
    return false;
  }

  return true;
}

export function setupOnboardingListeners() {
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
  if (!state.currentUser || state.currentUser.hasSeenComfortPrompt) return;

  const hasSettings =
    settings.length > 0 || hasComfortSettings(state.currentUser.comfortProfile);

  if (!hasSettings) {
    comfortOnboardingModal?.classList.remove("hidden");
  } else {
    markComfortPromptSeen();
  }
}

async function markComfortPromptSeen() {
  if (!state.currentUser || state.currentUser.hasSeenComfortPrompt) return;

  try {
    await api.post(`/api/users/${state.currentUser.userId}/comfort-prompt-seen`);
    state.currentUser.hasSeenComfortPrompt = true;
  } catch (err) {
    console.error("Failed to mark comfort prompt as seen", err);
  }
}
