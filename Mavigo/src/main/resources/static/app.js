// State
let currentUser = null;

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
const departureInput = document.getElementById('departure');
const disruptionLineInput = document.getElementById('disruptionLineInput');
const disruptionResultsDiv = document.getElementById('disruptionResults');
const fetchDisruptionsBtn = document.getElementById('fetchDisruptionsBtn');
const fetchShortDisruptionsBtn = document.getElementById('fetchShortDisruptionsBtn');

// Initialize
init();

function init() {
    setupAuthListeners();
    setupJourneyForm();
    setupDisruptionTester();
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

// Disruptions tester
function setupDisruptionTester() {
    if (!disruptionLineInput || !disruptionResultsDiv) return;
    if (fetchDisruptionsBtn) {
        fetchDisruptionsBtn.addEventListener('click', () => fetchDisruptions(false));
    }
    if (fetchShortDisruptionsBtn) {
        fetchShortDisruptionsBtn.addEventListener('click', () => fetchDisruptions(true));
    }
}

async function fetchDisruptions(shortTerm) {
    const line = (disruptionLineInput.value || '').trim();
    if (!line) {
        disruptionResultsDiv.innerHTML = '<p class="error-message">Please enter a line id or code.</p>';
        return;
    }

    disruptionResultsDiv.innerHTML = '<p class="loading">Loading disruptions...</p>';

    const url = shortTerm
        ? `/disruptions/line/${encodeURIComponent(line)}/short-term`
        : `/disruptions/line/${encodeURIComponent(line)}`;

    try {
        const resp = await fetch(url);
        if (!resp.ok) {
            const body = await resp.text();
            throw new Error(body || 'Request failed');
        }
        const data = await resp.json();
        renderDisruptions(data, line, shortTerm);
    } catch (err) {
        disruptionResultsDiv.innerHTML = `<p class="error-message">Error: ${err.message}</p>`;
    }
}

function renderDisruptions(disruptions, line, shortTerm) {
    if (!Array.isArray(disruptions) || disruptions.length === 0) {
        disruptionResultsDiv.innerHTML = `<p class="results-placeholder">No ${shortTerm ? 'short-term ' : ''}disruptions found for "${line}".</p>`;
        return;
    }

    const cards = disruptions.map(d => {
        const updated = d.updatedAt ? formatDateTime(d.updatedAt) : '—';
        const tags = Array.isArray(d.tags) && d.tags.length ? d.tags.join(', ') : 'None';
        const messages = Array.isArray(d.messages) && d.messages.length
            ? `<ul>${d.messages.map(m => `<li>${m}</li>`).join('')}</ul>`
            : '<p>No messages</p>';
        const periods = Array.isArray(d.applicationPeriods) && d.applicationPeriods.length
            ? `<ul>${d.applicationPeriods.map(p => `<li>${formatDateTime(p.begin)} → ${formatDateTime(p.end)}</li>`).join('')}</ul>`
            : '<p>No application periods</p>';

        return `
            <div class="card disruption-card">
                <h4>${d.lineName || d.lineCode || d.lineId || 'Unknown line'}</h4>
                <p><strong>Severity:</strong> ${d.severity || 'n/a'} (${d.effect || 'n/a'}) • Priority: ${d.priority ?? 'n/a'}</p>
                <p><strong>Status:</strong> ${d.status || 'n/a'} • <strong>Category:</strong> ${d.category || 'n/a'} • <strong>Cause:</strong> ${d.cause || 'n/a'}</p>
                <p><strong>Updated:</strong> ${updated}</p>
                <p><strong>Tags:</strong> ${tags}</p>
                <div><strong>Messages:</strong> ${messages}</div>
                <div><strong>Application periods:</strong> ${periods}</div>
            </div>
        `;
    }).join('');

    disruptionResultsDiv.innerHTML = cards;
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

        const journey = await resp.json();
        displayJourney(journey);
    } catch (err) {
        resultsDiv.innerHTML = `<p class="error-message">Error: ${err.message}</p>`;
    }
}

function displayJourney(journey) {
    const departure = journey.plannedDeparture ? formatDateTime(journey.plannedDeparture) : '—';
    const arrival = journey.plannedArrival ? formatDateTime(journey.plannedArrival) : '—';
    const legs = journey.legs || [];

    const legsHtml = legs.length
        ? legs.map(leg => `
            <li>
                <span class="leg-mode">${leg.mode || 'Unknown'}</span>
                ${leg.originLabel || '?'} → ${leg.destinationLabel || '?'}
                <div class="leg-times">
                    ${leg.estimatedDeparture ? formatDateTime(leg.estimatedDeparture) : '?'} - 
                    ${leg.estimatedArrival ? formatDateTime(leg.estimatedArrival) : '?'}
                    (${leg.durationSeconds ? formatDuration(leg.durationSeconds) : '?'})
                </div>
            </li>
        `).join('')
        : '<li>No route details available</li>';

    resultsDiv.innerHTML = `
        <div class="journey-result">
            <h3>${journey.originLabel} → ${journey.destinationLabel}</h3>
            <p class="journey-meta">Depart: ${departure} • Arrive: ${arrival}</p>
            <div class="journey-modes">
                <span>Comfort: ${journey.comfortModeEnabled ? 'On' : 'Off'}</span>
                <span>Touristic: ${journey.touristicModeEnabled ? 'On' : 'Off'}</span>
            </div>
            <ul class="journey-legs">${legsHtml}</ul>
        </div>
    `;
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

function generateId() {
    if (window.crypto && typeof window.crypto.randomUUID === 'function') {
        return window.crypto.randomUUID();
    }
    return `user-${Date.now()}`;
}
