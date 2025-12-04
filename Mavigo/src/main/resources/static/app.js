const form = document.getElementById('journeyForm');
const resultsDiv = document.getElementById('results');
const userIdInput = document.getElementById('userId');
const departureInput = document.getElementById('departure');

initUserCreationSection();
setDefaultDepartureTime();

form.addEventListener('submit', async (e) => {
    e.preventDefault();
    resultsDiv.innerHTML = "Planning journey...";

    const payload = collectPayload();
    if (!payload) {
        return;
    }

    try {
        const response = await fetch('/api/journeys', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });

        if (!response.ok) {
            const errorBody = await response.text();
            throw new Error(`HTTP ${response.status} - ${errorBody}`);
        }

        const journey = await response.json();
        displayJourney(journey);
    } catch (err) {
        resultsDiv.innerHTML = `<p style="color:red">Error planning journey: ${err.message}</p>`;
        console.error(err);
    }
});

function collectPayload() {
    const userId = userIdInput.value.trim();
    const from = document.getElementById('from').value.trim();
    const to = document.getElementById('to').value.trim();
    const departure = departureInput.value;

    if (!userId) {
        resultsDiv.innerHTML = `<p style="color:red">User ID is required.</p>`;
        return null;
    }
    if (!departure) {
        resultsDiv.innerHTML = `<p style="color:red">Departure date/time is required.</p>`;
        return null;
    }

    return {
        journey: {
            userId,
            originQuery: from,
            destinationQuery: to,
            departureTime: departure
        },
        preferences: {
            comfortMode: document.getElementById('comfort').checked,
            touristicMode: document.getElementById('touristic').checked
        }
    };
}

function displayJourney(journey) {
    const departure = journey.plannedDeparture ? formatDateTime(journey.plannedDeparture) : 'Unknown';
    const arrival = journey.plannedArrival ? formatDateTime(journey.plannedArrival) : 'Unknown';
    const legs = journey.legs || [];

    const legsHtml = legs.length
        ? legs.map(formatLeg).join('')
        : '<li>No legs returned from Prim</li>';

    resultsDiv.innerHTML = `
        <div class="journey">
            <h3>${journey.originLabel} → ${journey.destinationLabel}</h3>
            <p>Departure: ${departure} | Arrival: ${arrival}</p>
            <p>Comfort: ${journey.comfortModeEnabled ? 'On' : 'Off'} | Touristic: ${journey.touristicModeEnabled ? 'On' : 'Off'}</p>
            <p>Prim Itinerary: ${journey.primItineraryId || 'N/A'}</p>
            <ul class="sections">${legsHtml}</ul>
        </div>
    `;
}

function formatLeg(leg) {
    const mode = leg.mode || 'Unknown';
    const from = leg.originLabel || 'Unknown';
    const to = leg.destinationLabel || 'Unknown';
    const departure = leg.estimatedDeparture ? formatDateTime(leg.estimatedDeparture) : 'Unknown';
    const arrival = leg.estimatedArrival ? formatDateTime(leg.estimatedArrival) : 'Unknown';
    const duration = leg.durationSeconds ? formatDuration(leg.durationSeconds) : '?';

    return `<li>
        <strong>${mode}</strong> ${from} → ${to}<br>
        ${departure} - ${arrival} (${duration})
    </li>`;
}

function formatDuration(seconds) {
    if (!seconds && seconds !== 0) {
        return '?';
    }
    const h = Math.floor(seconds / 3600);
    const m = Math.floor((seconds % 3600) / 60);
    return `${h}h ${m}m`;
}

function formatDateTime(dt) {
    const date = new Date(dt);
    return date.toLocaleString();
}

function setDefaultDepartureTime() {
    if (!departureInput) {
        return;
    }
    const oneHourLater = new Date(Date.now() + 60 * 60 * 1000);
    const iso = oneHourLater.toISOString().slice(0, 16);
    departureInput.value = iso;
}

function initUserCreationSection() {
    if (!form || !userIdInput) {
        return;
    }

    const container = document.createElement('section');
    container.className = 'user-section';
    container.innerHTML = `
        <h2>Create User</h2>
        <p style="max-width:600px;margin:0 0 10px;">
            Use this helper to create a user and automatically copy the generated ID into the journey planner.
        </p>
        <form id="userCreateForm" style="display:flex;flex-wrap:wrap;gap:10px;align-items:flex-end;">
            <label style="display:flex;flex-direction:column;">
                Display Name
                <input type="text" id="userDisplayName" required>
            </label>
            <label style="display:flex;flex-direction:column;">
                Email
                <input type="email" id="userEmail" required>
            </label>
            <label style="display:flex;flex-direction:column;">
                External ID
                <input type="text" id="userExternalId" required>
            </label>
            <button type="submit">Create User</button>
        </form>
        <div id="userCreationStatus" style="margin-top:8px;"></div>
        <hr>
    `;

    form.parentNode.insertBefore(container, form);

    const userForm = container.querySelector('#userCreateForm');
    const displayNameInput = container.querySelector('#userDisplayName');
    const emailInput = container.querySelector('#userEmail');
    const externalIdInput = container.querySelector('#userExternalId');
    const statusDiv = container.querySelector('#userCreationStatus');

    externalIdInput.value = generateExternalIdSeed();

    userForm.addEventListener('submit', async (event) => {
        event.preventDefault();

        const payload = {
            displayName: displayNameInput.value.trim(),
            email: emailInput.value.trim(),
            externalId: externalIdInput.value.trim()
        };

        if (!payload.displayName || !payload.email || !payload.externalId) {
            statusDiv.innerHTML = `<p style="color:red">All fields are required.</p>`;
            return;
        }

        statusDiv.textContent = 'Creating user...';

        try {
            const response = await fetch('/api/users', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });

            if (!response.ok) {
                const errorBody = await response.text();
                throw new Error(errorBody || `Request failed with status ${response.status}`);
            }

            const user = await response.json();
            statusDiv.innerHTML = `<p style="color:green">Created user <strong>${user.displayName}</strong>. ID copied below.</p>`;

            if (user && user.userId) {
                userIdInput.value = user.userId;
            }

            displayNameInput.value = '';
            emailInput.value = '';
            externalIdInput.value = generateExternalIdSeed();
        } catch (err) {
            statusDiv.innerHTML = `<p style="color:red">Failed to create user: ${err.message}</p>`;
            console.error(err);
        }
    });
}

function generateExternalIdSeed() {
    if (window.crypto && typeof window.crypto.randomUUID === 'function') {
        return window.crypto.randomUUID();
    }
    return `user-${Date.now()}`;
}
