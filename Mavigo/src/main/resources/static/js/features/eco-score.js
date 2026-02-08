import { api } from "../core/api.js";
import { state } from "../core/state.js";
import { formatDateTime } from "../core/utils.js";
import { showToast } from "../ui/toast.js";

const navEcoScoreBtn = document.getElementById("navEcoScoreBtn");
const ecoScoreView = document.getElementById("ecoScoreView");
const totalCo2SavedEl = document.getElementById("totalCo2Saved");
const earnedBadgeCountEl = document.getElementById("earnedBadgeCount");
const badgeGrid = document.getElementById("badgeGrid");
const ecoHistoryList = document.getElementById("ecoHistoryList");

export function setupEcoScore() {
    navEcoScoreBtn?.addEventListener("click", () => {
        import("./nav.js").then(nav => nav.setView("eco-score"));
    });
}

export async function refreshEcoDashboard() {
    if (!state.currentUser) return;

    try {
        const data = await api.getEcoDashboard(state.currentUser.userId);
        renderDashboard(data);
    } catch (err) {
        showToast("Failed to load eco dashboard", { variant: "warning" });
    }
}

function renderDashboard(data) {
    if (totalCo2SavedEl) totalCo2SavedEl.textContent = data.totalCo2Saved.toFixed(2);
    if (earnedBadgeCountEl) earnedBadgeCountEl.textContent = data.badgeCount;

    if (badgeGrid) {
        const { earnedBadges, allBadges } = data;

        if (!allBadges || allBadges.length === 0) {
            badgeGrid.innerHTML = '<p class="results-placeholder">No badges available.</p>';
        } else {
            badgeGrid.innerHTML = allBadges.map(badge => {
                const earned = earnedBadges.find(eb => eb.name === badge.name);
                const statusClass = earned ? 'earned' : 'locked';
                const earnedDateHtml = earned
                    ? `<div class="badge-date">Earned ${new Date(earned.earnedAt).toLocaleDateString()}</div>`
                    : '<div class="badge-date">Not earned yet</div>';

                return `
                    <div class="badge-item ${statusClass}">
                        <div class="badge-icon">${badge.icon || "🏅"}</div>
                        <div class="badge-name">${badge.name}</div>
                        ${earnedDateHtml}
                        <div class="badge-desc">${badge.description}</div>
                    </div>
                `;
            }).join("");
        }
    }

    if (ecoHistoryList) {
        if (data.history.length === 0) {
            ecoHistoryList.innerHTML = '<p class="results-placeholder">No recent journey activity found.</p>';
        } else {
            ecoHistoryList.innerHTML = data.history.map(item => `
                <div class="history-item">
                    <div class="history-info">
                        <div class="history-route">${item.origin} → ${item.destination}</div>
                        <div class="history-date">${formatDateTime(item.timestamp)}</div>
                    </div>
                    <div class="history-metrics">
                        <div class="history-co2">+${item.co2Saved.toFixed(2)} kg CO2</div>
                        <div class="history-distance">${(item.distance / 1000).toFixed(1)} km</div>
                    </div>
                </div>
            `).join("");
        }
    }
}

