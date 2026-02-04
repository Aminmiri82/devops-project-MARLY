import { api } from "../core/api.js";
import { state } from "../core/state.js";
import { showToast } from "../ui/toast.js";
import { prefillJourneyFromSuggestion } from "./journey.js";

const editHomeAddressBtn = document.getElementById("editHomeAddressBtn");
const homeAddressStatusBadge = document.getElementById("homeAddressStatusBadge");
const homeAddressModal = document.getElementById("homeAddressModal");
const homeAddressForm = document.getElementById("homeAddressForm");
const homeAddressInput = document.getElementById("homeAddressInput");
const homeAddressCloseBtn = document.getElementById("homeAddressCloseBtn");
const homeAddressCancelBtn = document.getElementById("homeAddressCancelBtn");

export function setupHomeAddressListeners() {
  editHomeAddressBtn?.addEventListener("click", () => openHomeAddressModal());
  homeAddressCloseBtn?.addEventListener("click", closeHomeAddressModal);
  homeAddressCancelBtn?.addEventListener("click", closeHomeAddressModal);
  homeAddressForm?.addEventListener("submit", saveHomeAddress);

  homeAddressModal?.addEventListener("click", (e) => {
    if (e.target === homeAddressModal) closeHomeAddressModal();
  });
}

export function openHomeAddressModal() {
  if (!homeAddressModal) return;
  if (homeAddressInput) {
    homeAddressInput.value = state.currentUser?.homeAddress || "";
    homeAddressInput.focus();
  }
  homeAddressModal.classList.remove("hidden");
}

function closeHomeAddressModal() {
  if (!homeAddressModal) return;
  homeAddressModal.classList.add("hidden");
  document.getElementById("homeAddressError")?.classList.add("hidden");
}

async function saveHomeAddress(e) {
  e?.preventDefault();
  if (!state.currentUser?.userId) return;

  const value = (homeAddressInput?.value || "").trim();
  const payload = { homeAddress: value };
  const errorEl = document.getElementById("homeAddressError");

  try {
    const updated = await api.put(
      `/api/users/${state.currentUser.userId}/home-address`,
      payload
    );
    state.currentUser = updated;
    renderHomeAddressStatus(state.currentUser);
    closeHomeAddressModal();

    if (value) {
      showToast("Home address saved.", { variant: "success" });
    } else {
      showToast("Home address cleared.", { variant: "warning" });
    }

    if (state.pendingSuggestionTask && state.currentUser.homeAddress) {
      const task = state.pendingSuggestionTask;
      state.pendingSuggestionTask = null;
      prefillJourneyFromSuggestion(task);
    }
  } catch (err) {
    if (errorEl) {
      errorEl.textContent = err?.message || "Failed to save home address.";
      errorEl.classList.remove("hidden");
    }
  }
}

export function renderHomeAddressStatus(user) {
  if (!homeAddressStatusBadge) return;
  if (!user) {
    homeAddressStatusBadge.textContent = "";
    homeAddressStatusBadge.classList.remove("set");
    return;
  }

  const hasHome = !!(user.homeAddress && String(user.homeAddress).trim());
  if (!hasHome) {
    homeAddressStatusBadge.textContent = "Not set";
    homeAddressStatusBadge.classList.remove("set");
  } else {
    homeAddressStatusBadge.textContent = "Set";
    homeAddressStatusBadge.classList.add("set");
  }
}
