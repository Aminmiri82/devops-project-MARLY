// State
let currentUser = null;
let currentJourney = null;

// DOM Elements
const authModal = document.getElementById('authModal');
const loginFormEl = document.getElementById('loginFormEl');
const registerFormEl = document.getElementById('registerFormEl');
const loginFormView = document.getElementById('loginForm');
const registerFormView = document.getElementById('registerForm');
const loggedOutView = document.getElementById('loggedOutView');
const loggedInView = document.getElementById('loggedInView');
const userGreeting = document.getElementById('userGreeting');
const notLoggedInPrompt = document.getElementById('notLoggedInPrompt');
const mainContent = document.getElementById('mainContent');
const journeyForm = document.getElementById('journeyForm');
const resultsDiv = document.getElementById('results');
const currentJourneyPanel = document.getElementById('currentJourneyPanel');
const currentJourneyContent = document.getElementById('currentJourneyContent');
const completeJourneyBtn = document.getElementById('completeJourneyBtn');
const cancelJourneyBtn = document.getElementById('cancelJourneyBtn');
const departureInput = document.getElementById('departure');
const reportDisruptionBtn = document.getElementById('reportDisruptionBtn');
const fetchShortDisruptionsBtn = document.getElementById('fetchShortDisruptionsBtn');

// Initialize
init();

function init() {
    setupAuthListeners();
    setupJourneyForm();
    setupJourneyActions();

    setupGoogleLinkListeners();
    setDefaultDepartureTime();
    restoreSession();
}

// Auth UI
function setupAuthListeners() {
    document.getElementById('showLoginBtn').addEventListener('click', () => openAuthModal('login'));
    document.getElementById('showRegisterBtn').addEventListener('click', () => openAuthModal('register'));
    document.getElementById('promptLoginBtn').addEventListener('click', () => openAuthModal('login'));
    document.getElementById('closeAuthModal').addEventListener('click', closeAuthModal);
    document.getElementById('switchToRegister').addEventListener('click', (e) => { e.preventDefault(); showAuthForm('register'); });
    document.getElementById('switchToLogin').addEventListener('click', (e) => { e.preventDefault(); showAuthForm('login'); });
    document.getElementById('logoutBtn').addEventListener('click', logout);

    authModal.addEventListener('click', (e) => {
        if (e.target === authModal) closeAuthModal();
    });

    loginFormEl.addEventListener('submit', handleLogin);
    registerFormEl.addEventListener('submit', handleRegister);
}

function openAuthModal(formType) {
    authModal.classList.remove('hidden');
    showAuthForm(formType);
}

function closeAuthModal() {
    authModal.classList.add('hidden');
    clearAuthErrors();
}

function showAuthForm(type) {
    clearAuthErrors();
    if (type === 'login') {
        loginFormView.classList.remove('hidden');
        registerFormView.classList.add('hidden');
    } else {
        loginFormView.classList.add('hidden');
        registerFormView.classList.remove('hidden');
    }
}

function clearAuthErrors() {
    document.getElementById('loginError').classList.add('hidden');
    document.getElementById('registerError').classList.add('hidden');
}

async function handleLogin(e) {
    e.preventDefault();
    const email = document.getElementById('loginEmail').value.trim();
    const errorEl = document.getElementById('loginError');

    if (!email) {
        showError(errorEl, 'Please enter your email.');
        return;
    }

    try {
        const resp = await fetch('/api/users/login', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ email })
        });
        if (!resp.ok) {
            const body = await resp.text();
            throw new Error(body || 'Login failed');
        }
        const user = await resp.json();
        setCurrentUser(user);
        closeAuthModal();
        loginFormEl.reset();
    } catch (err) {
        showError(errorEl, err.message);
    }
}

async function handleRegister(e) {
    e.preventDefault();
    const name = document.getElementById('registerName').value.trim();
    const email = document.getElementById('registerEmail').value.trim();
    const errorEl = document.getElementById('registerError');

    if (!name || !email) {
        showError(errorEl, 'Please fill in all fields.');
        return;
    }

    const payload = {
        displayName: name,
        email: email,
        externalId: generateId()
    };

    try {
        const resp = await fetch('/api/users', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });
        if (!resp.ok) {
            const body = await resp.text();
            throw new Error(body || 'Registration failed');
        }
        const user = await resp.json();
        setCurrentUser(user);
        closeAuthModal();
        registerFormEl.reset();
    } catch (err) {
        showError(errorEl, err.message);
    }
}

function logout() {
    currentUser = null;
    localStorage.removeItem('mavigo_user_id');
    updateUI();
}

function setCurrentUser(user) {
    currentUser = user;
    localStorage.setItem('mavigo_user_id', user.userId);
    updateUI();
}

function restoreSession() {
    const savedUserId = localStorage.getItem('mavigo_user_id');
    if (savedUserId) {
        fetch(`/api/users/${savedUserId}`)
            .then(resp => resp.ok ? resp.json() : Promise.reject())
            .then(user => {
                currentUser = user;
                updateUI();
            })
            .catch(() => {
                localStorage.removeItem('mavigo_user_id');
                updateUI();
            });
    } else {
        updateUI();
    }
}

function updateUI() {
    if (currentUser) {
        loggedOutView.classList.add('hidden');
        loggedInView.classList.remove('hidden');
        userGreeting.textContent = `Hi, ${currentUser.displayName}`;
        notLoggedInPrompt.classList.add('hidden');
        mainContent.classList.remove('hidden');
        renderUserInfo();
        renderGoogleLinkStatus(currentUser);
    } else {
        loggedOutView.classList.remove('hidden');
        loggedInView.classList.add('hidden');
        notLoggedInPrompt.classList.remove('hidden');
        mainContent.classList.add('hidden');
    }
}

function renderUserInfo() {
    document.getElementById('displayUserName').textContent = currentUser.displayName;
    document.getElementById('displayUserEmail').textContent = currentUser.email;
    document.getElementById('displayUserId').textContent = currentUser.userId;
}

function showError(el, message) {
    el.textContent = message;
    el.classList.remove('hidden');
}

// Journey Form
function setupJourneyForm() {
    journeyForm.addEventListener('submit', handleJourneySubmit);
}

function setupJourneyActions() {
    completeJourneyBtn.addEventListener('click', completeJourney);
    cancelJourneyBtn.addEventListener('click', cancelJourney);
    if (reportDisruptionBtn) {
        reportDisruptionBtn.addEventListener('click', reportDisruption);
    }
}

async function startJourney(journeyId, btnElement) {
    if (!currentUser) return;

    // Collect all start buttons
    const allButtons = document.querySelectorAll('.start-journey-btn');

    if (btnElement) {
        btnElement.disabled = true;
        btnElement.textContent = 'Starting...';
    }

    // Hide other buttons
    allButtons.forEach(btn => {
        if (btn !== btnElement) {
            btn.classList.add('hidden');
        }
    });

    try {
        const url = `/api/journeys/${journeyId}/start`;
        console.log('Fetching:', url);
        const resp = await fetch(url, { method: 'POST' });
        if (!resp.ok) {
            const body = await resp.text();
            throw new Error(body || 'Failed to start journey');
        }
        const journey = await resp.json();
        updateCurrentJourney(journey);
    } catch (err) {
        alert(err.message);
        // Restore buttons on error
        allButtons.forEach(btn => {
            btn.classList.remove('hidden');
            btn.disabled = false;
            if (btn === btnElement) {
                btn.textContent = 'Start Journey';
            }
        });
    }
}

async function completeJourney() {
    if (!currentJourney) return;
    try {
        const url = `/api/journeys/${currentJourney.journeyId}/complete`;
        console.log('Fetching:', url);
        const resp = await fetch(url, { method: 'POST' });
        if (!resp.ok) {
            const body = await resp.text();
            throw new Error(body || 'Failed to complete journey');
        }
        const journey = await resp.json();
        updateCurrentJourney(journey);
        alert('Journey completed!');
    } catch (err) {
        alert(err.message);
    }
}

async function cancelJourney() {
    if (!currentJourney) return;
    if (!confirm('Are you sure you want to cancel this journey?')) return;

    try {
        const url = `/api/journeys/${currentJourney.journeyId}/cancel`;
        console.log('Fetching:', url);
        const resp = await fetch(url, { method: 'POST' });
        if (!resp.ok) {
            const body = await resp.text();
            throw new Error(body || 'Failed to cancel journey');
        }
        const journey = await resp.json();
        updateCurrentJourney(journey);
    } catch (err) {
        alert(err.message);
    }
}

function updateCurrentJourney(journey) {
    currentJourney = journey;

    if (journey && (journey.status === 'PLANNED' || journey.status === 'IN_PROGRESS' || journey.status === 'REROUTED')) {
        currentJourneyPanel.classList.remove('hidden');
        renderCurrentJourney(journey);

        // Hide results if we have an active journey
        document.querySelector('.results-panel').classList.add('hidden');
    } else {
        // Journey finished or cancelled
        currentJourneyPanel.classList.add('hidden');
        document.querySelector('.results-panel').classList.remove('hidden');
        resultsDiv.innerHTML = '<p class="results-placeholder">Your journey results will appear here.</p>';
        currentJourney = null;
    }
}

function calculateProgress(journey) {
    if (journey.status !== 'IN_PROGRESS' && journey.status !== 'REROUTED') return 0;

    const now = new Date();
    const start = new Date(journey.actualDeparture || journey.plannedDeparture);
    const end = new Date(journey.plannedArrival);

    if (isNaN(start) || isNaN(end) || end <= start) return 0;

    if (now < start) return 0;
    if (now > end) return 100;

    const progress = ((now - start) / (end - start)) * 100;
    return Math.min(Math.max(Math.round(progress), 0), 100);
}

function renderCurrentJourney(journey) {
    const statusClass = journey.status === 'IN_PROGRESS' ? 'status-active' : 'status-planned';
    const progress = calculateProgress(journey);

    currentJourneyContent.innerHTML = `
        <div class="journey-status-card">
            <div class="status-badge ${statusClass}">${journey.status}</div>
            ${journey.status === 'REROUTED' || journey.disruptionCount > 0 ? '<div class="disruption-warning">⚠️ Disruption : New Journey Started</div>' : ''}
            <h3>${journey.originLabel} → ${journey.destinationLabel}</h3>
            
            ${journey.status === 'IN_PROGRESS' || journey.status === 'REROUTED' ? `
                <div class="progress-container">
                    <div class="progress-bar" style="width: ${progress}%"></div>
                </div>
                <span class="progress-text">${progress}% Completed</span>
            ` : ''}

            <p><strong>Planned Departure:</strong> ${formatDateTime(journey.plannedDeparture)}</p>
            ${journey.actualDeparture ? `<p><strong>Started:</strong> ${formatDateTime(journey.actualDeparture)}</p>` : ''}
            <p><strong>Planned Arrival:</strong> ${formatDateTime(journey.plannedArrival)}</p>
        </div>
    `;

    if (journey.status === 'IN_PROGRESS' || journey.status === 'REROUTED') {
        completeJourneyBtn.classList.remove('hidden');
        cancelJourneyBtn.classList.remove('hidden');
        if (reportDisruptionBtn) reportDisruptionBtn.classList.remove('hidden');
    } else {
        completeJourneyBtn.classList.add('hidden');
        cancelJourneyBtn.classList.add('hidden');
        if (reportDisruptionBtn) reportDisruptionBtn.classList.add('hidden');
    }
}

async function reportDisruption() {
    if (!currentJourney) return;

    // Ask user for rerouting method
    const choice = confirm("Report disruption: Use current GPS location? (Click 'OK' for GPS, 'Cancel' to enter a station name)");

    let lat = '';
    let lng = '';
    let manualOrigin = '';

    if (choice) {
        // Use GPS
        const getPosition = () => new Promise((resolve, reject) => {
            if (!navigator.geolocation) {
                reject(new Error('Geolocation is not supported by your browser'));
            } else {
                navigator.geolocation.getCurrentPosition(resolve, reject);
            }
        });

        try {
            const position = await getPosition();
            lat = position.coords.latitude;
            lng = position.coords.longitude;
            console.log('Got user location:', lat, lng);
        } catch (geoErr) {
            console.warn('Could not get location:', geoErr);
            if (confirm("Could not get GPS location. Enter a station manually?")) {
                manualOrigin = prompt("Enter new departure station:");
                if (!manualOrigin) return;
            } else {
                return;
            }
        }
    } else {
        // Manual entry
        manualOrigin = prompt("Enter new departure station:");
        if (!manualOrigin) return;
    }

    try {
        const creator = currentUser ? currentUser.displayName : 'Anonymous';
        let url = `/perturbations/apply?journeyId=${currentJourney.journeyId}&creator=${encodeURIComponent(creator)}`;

        if (lat && lng) {
            url += `&userLat=${lat}&userLng=${lng}`;
        } else if (manualOrigin) {
            url += `&newOrigin=${encodeURIComponent(manualOrigin)}`;
        }

        const resp = await fetch(url, { method: 'POST' });
        if (!resp.ok) {
            const body = await resp.text();
            throw new Error(body || 'Failed to report disruption');
        }

        const newJourneys = await resp.json();

        // Switch back to results view to let user choose
        currentJourneyPanel.classList.add('hidden');
        document.querySelector('.results-panel').classList.remove('hidden');

        resultsDiv.innerHTML = '';
        if (newJourneys && newJourneys.length > 0) {
            newJourneys.forEach(displayJourney);
            alert('Disruption reported. Please choose an alternative route from the list below.');
        } else {
            alert('Disruption reported, but no alternative routes found.');
        }
    } catch (err) {
        // Simple warning as requested if journey cannot be found or location is invalid
        const msg = err.message || "";
        if (msg.includes("No places") || msg.includes("No stop area") || msg.includes("No journey options") || msg.includes("Failed to calculate journey")) {
            alert("No journey found.");
        } else {
            alert("No journey found. (Technical detail: " + msg + ")");
        }
    }
}

async function handleJourneySubmit(e) {
    e.preventDefault();

    if (!currentUser) {
        resultsDiv.innerHTML = '<p class="error-message">Please log in first.</p>';
        return;
    }

    const from = document.getElementById('from').value.trim();
    const to = document.getElementById('to').value.trim();
    const departure = departureInput.value;
    const comfort = document.getElementById('comfort').checked;
    const touristic = document.getElementById('touristic').checked;

    if (!departure) {
        resultsDiv.innerHTML = '<p class="error-message">Please select a departure time.</p>';
        return;
    }

    const payload = {
        journey: {
            userId: currentUser.userId,
            originQuery: from,
            destinationQuery: to,
            departureTime: departure
        },
        preferences: {
            comfortMode: comfort,
            touristicMode: touristic
        }
    };

    resultsDiv.innerHTML = '<p class="loading">Planning your journey...</p>';

    try {
        const resp = await fetch('/api/journeys', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });

        if (!resp.ok) {
            const body = await resp.text();
            throw new Error(body || 'Failed to plan journey');
        }

        const journeys = await resp.json();

        // Clear previous results
        resultsDiv.innerHTML = '';

        // Handle both single object (legacy) and array
        const list = Array.isArray(journeys) ? journeys : [journeys];

        if (list.length === 0) {
            resultsDiv.innerHTML = '<p class="error-message">No journey found.</p>';
            return;
        }

        list.forEach(displayJourney);
    } catch (err) {
        resultsDiv.innerHTML = `<p class="error-message">No journey found.</p>`;
        console.error("Journey planning error:", err);
    }
}

function displayJourney(journey) {
    const departure = journey.plannedDeparture ? formatDateTime(journey.plannedDeparture) : '—';
    const arrival = journey.plannedArrival ? formatDateTime(journey.plannedArrival) : '—';
    const legs = journey.legs || [];

    // Process legs based on rules:
    // 1. If duration < 60s AND origin == destination -> Filter out
    // 2. If duration >= 60s AND origin == destination AND mode == 'OTHER' -> Change mode to 'WALK'
    const processedLegs = legs.filter(leg => {
        const duration = leg.durationSeconds || 0;
        const samePlace = leg.originLabel === leg.destinationLabel;
        if (duration < 60 && samePlace) return false;
        return true;
    }).map(leg => {
        const duration = leg.durationSeconds || 0;
        const samePlace = leg.originLabel === leg.destinationLabel;
        if (duration >= 60 && samePlace && leg.mode === 'OTHER') {
            // Return a copy with modified mode
            return { ...leg, mode: 'WALK' };
        }
        return leg;
    });

    const legsHtml = processedLegs.length
        ? processedLegs.map(leg => `
            <li class="journey-leg-item">
                <div class="leg-marker"></div>
                <div class="leg-content">
                    <span class="leg-mode">${formatMode(leg.mode)} ${leg.lineCode ? `<span class="leg-line">${leg.lineCode}</span>` : ''}</span>
                    <span class="leg-route">${leg.originLabel || '?'} → ${leg.destinationLabel || '?'}</span>
                    <div class="leg-times">
                        ${leg.estimatedDeparture ? formatDateTime(leg.estimatedDeparture) : '?'} - 
                        ${leg.estimatedArrival ? formatDateTime(leg.estimatedArrival) : '?'}
                        <span class="leg-duration">(${leg.durationSeconds ? formatDuration(leg.durationSeconds) : '?'})</span>
                    </div>
                </div>
            </li>
        `).join('')
        : '<li>No route details available</li>';

    const html = `
        <div class="journey-result">
            <h3>${journey.originLabel} → ${journey.destinationLabel}</h3>
            <p class="journey-meta">Depart: ${departure} • Arrive: ${arrival}</p>
            <div class="journey-modes">
                <span>Comfort: ${journey.comfortModeEnabled ? 'On' : 'Off'}</span>
                <span>Touristic: ${journey.touristicModeEnabled ? 'On' : 'Off'}</span>
            </div>
            <button class="btn btn-primary btn-sm start-journey-btn" onclick="startJourney('${journey.journeyId}', this)">Start Journey</button>
            <h4>Itinerary Steps:</h4>
            <ul class="journey-legs">${legsHtml}</ul>
        </div>
    `;
    resultsDiv.insertAdjacentHTML('beforeend', html);
}

// Google Link
function setupGoogleLinkListeners() {
    document.getElementById('linkGoogleTasksBtn').addEventListener('click', startGoogleLinkFlow);
    document.getElementById('refreshGoogleLinkBtn').addEventListener('click', refreshGoogleLink);
    window.addEventListener('message', handleGoogleLinkMessage);
}

function startGoogleLinkFlow() {
    const statusEl = document.getElementById('googleLinkStatus');

    if (!currentUser) {
        statusEl.textContent = 'Please log in first.';
        return;
    }

    const linkUrl = `/api/google/tasks/link?userId=${encodeURIComponent(currentUser.userId)}`;
    const popup = window.open(linkUrl, 'googleTasksLink', 'width=600,height=700');

    if (!popup) {
        statusEl.textContent = 'Popup blocked. Please allow popups for this site.';
        return;
    }

    statusEl.textContent = 'Complete sign-in in the popup...';

    const watcher = setInterval(() => {
        if (popup.closed) {
            clearInterval(watcher);
            refreshGoogleLink();
        }
    }, 1500);
}

async function refreshGoogleLink() {
    if (!currentUser) return;

    try {
        const resp = await fetch(`/api/users/${currentUser.userId}`);
        if (resp.ok) {
            const user = await resp.json();
            currentUser = user;
            renderGoogleLinkStatus(user);
        }
    } catch (err) {
        console.error('Failed to refresh link status', err);
    }
}

function renderGoogleLinkStatus(user) {
    const statusEl = document.getElementById('googleLinkStatus');

    if (!user || !user.googleAccountLinked) {
        statusEl.textContent = 'Not linked';
        statusEl.classList.remove('linked');
    } else {
        const email = user.googleAccountEmail || 'your Google account';
        const linkedAt = user.googleAccountLinkedAt ? formatDateTime(user.googleAccountLinkedAt) : '';
        statusEl.textContent = `Linked to ${email}${linkedAt ? ` (${linkedAt})` : ''}`;
        statusEl.classList.add('linked');
    }
}

function handleGoogleLinkMessage(event) {
    if (event.origin !== window.location.origin) return;
    if (event.data && event.data.type === 'GOOGLE_TASKS_LINKED') {
        refreshGoogleLink();
    }
}

// Utilities
function setDefaultDepartureTime() {
    if (!departureInput) return;
    const oneHourLater = new Date(Date.now() + 60 * 60 * 1000);
    departureInput.value = oneHourLater.toISOString().slice(0, 16);
}

function formatDuration(seconds) {
    if (!seconds && seconds !== 0) return '?';
    const h = Math.floor(seconds / 3600);
    const m = Math.floor((seconds % 3600) / 60);
    return h > 0 ? `${h}h ${m}m` : `${m}m`;
}

function formatDateTime(dt) {
    return new Date(dt).toLocaleString();
}

function formatMode(mode) {
    if (!mode) return 'Unknown';
    if (mode === 'OTHER') return 'Connection';
    if (mode === 'WALK') return 'Walk';
    // Capitalize first letter, lowercase rest for others
    return mode.charAt(0).toUpperCase() + mode.slice(1).toLowerCase();
}

function generateId() {
    if (window.crypto && typeof window.crypto.randomUUID === 'function') {
        return window.crypto.randomUUID();
    }
    return `user-${Date.now()}`;
}
