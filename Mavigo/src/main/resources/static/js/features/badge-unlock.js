/**
 * Badge Unlock UI Module
 * Handles displaying badge unlock celebrations with confetti
 */

import { confetti } from "./confetti.js";

// Badge icon mapping
const BADGE_ICONS = {
    FIRST_JOURNEY: "🚀",
    ECO_WARRIOR: "🌱",
    FREQUENT_TRAVELER: "🎯",
    CO2_SAVER: "💚",
    EXPLORER: "🗺️",
    COMMUTER: "🚇",
    GREEN_CHAMPION: "🏆",
    DISTANCE_MASTER: "📏",
};

/**
 * Show badge unlock modal with confetti animation
 * @param {Object} badge - Badge object with name, description, and badgeType
 */
export function showBadgeUnlock(badge) {
    const modal = document.getElementById("badgeUnlockModal");
    const iconElement = document.getElementById("badgeUnlockIcon");
    const nameElement = document.getElementById("badgeUnlockName");
    const descriptionElement = document.getElementById("badgeUnlockDescription");
    const closeBtn = document.getElementById("badgeUnlockCloseBtn");

    if (!modal || !iconElement || !nameElement || !descriptionElement) {
        console.error("Badge unlock modal elements not found");
        return;
    }

    // Set badge content
    const icon = badge.icon || BADGE_ICONS[badge.badgeType] || "🏆";
    iconElement.textContent = icon;
    nameElement.textContent = badge.name || "Achievement Unlocked";
    descriptionElement.textContent =
        badge.description || "You've earned a new badge!";

    // Show modal
    modal.classList.remove("hidden");

    // Trigger confetti after a short delay
    setTimeout(() => {
        confetti.start(150);
    }, 300);

    // Close handler
    const closeHandler = () => {
        modal.classList.add("hidden");
        confetti.stop();
        closeBtn.removeEventListener("click", closeHandler);
    };

    closeBtn.addEventListener("click", closeHandler);

    // Auto-close after 10 seconds
    setTimeout(() => {
        if (!modal.classList.contains("hidden")) {
            closeHandler();
        }
    }, 10000);
}

/**
 * Show multiple badge unlocks in sequence
 * @param {Array} badges - Array of badge objects
 */
export function showBadgeUnlocks(badges) {
    if (!badges || badges.length === 0) return;

    let index = 0;

    function showNext() {
        if (index >= badges.length) return;

        const badge = badges[index];
        showBadgeUnlock(badge);

        index++;

        // Show next badge after current one is dismissed or after 10 seconds
        setTimeout(showNext, 10500);
    }

    showNext();
}
