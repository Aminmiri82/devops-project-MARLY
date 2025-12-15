const form = document.getElementById('journeyForm');
const resultsDiv = document.getElementById('results');

form.addEventListener('submit', async (e) => {
    e.preventDefault();
    resultsDiv.innerHTML = "Loading...";

    const from = document.getElementById('from').value;
    const to = document.getElementById('to').value;

    try {
        const response = await fetch(`/api/journeys?from=${encodeURIComponent(from)}&to=${encodeURIComponent(to)}`);
        if (!response.ok) throw new Error(`HTTP ${response.status}`);

        const journeys = await response.json();
        displayJourneys(journeys);
    } catch (err) {
        resultsDiv.innerHTML = `<p style="color:red">Error fetching journeys: ${err}</p>`;
        console.error(err);
    }
});

function displayJourneys(journeys) {
    resultsDiv.innerHTML = journeys.map(j => {
        const realSections = j.sections?.filter(s => s.type !== 'crow_fly') || [];

        if (realSections.length === 0) return '<p>No valid sections</p>';

        // Get departure and arrival from first and last real section
        const first = realSections[0];
        const last = realSections[realSections.length - 1];

        const dep = first?.from?.departureDateTime 
            ? new Date(first.from.departureDateTime).toLocaleTimeString([], {hour:'2-digit',minute:'2-digit'}) 
            : 'Unknown';
        const arr = last?.to?.arrivalDateTime 
            ? new Date(last.to.arrivalDateTime).toLocaleTimeString([], {hour:'2-digit',minute:'2-digit'}) 
            : 'Unknown';

        // Duration in h:m from total journey seconds
        let duration = '?';
        if (j.duration) {
            const hours = Math.floor(j.duration / 3600);
            const minutes = Math.floor((j.duration % 3600) / 60);
            duration = `${hours}h${minutes}m`;
        }

        const transfers = j.nbTransfers !== undefined ? j.nbTransfers : '?';

        const sectionsHtml = realSections.map(s => {
            const mode = s.displayInformations?.commercialMode || s.type || 'Unknown';
            const fromName = s.from?.name || 'Unknown';
            const toName = s.to?.name || 'Unknown';
            let sectionDuration = '?';
            if (s.duration) {
                const h = Math.floor(s.duration / 3600);
                const m = Math.floor((s.duration % 3600) / 60);
                sectionDuration = `${h}h${m}m`;
            }
            return `<li>${mode} from ${fromName} to ${toName} (${sectionDuration})</li>`;
        }).join('');

        return `
            <div class="journey">
                <h3>${dep} → ${arr} | Duration: ${duration} | Transfers: ${transfers}</h3>
                <ul>${sectionsHtml}</ul>
            </div>
        `;
    }).join('<hr>');
}



// Helper to format duration in seconds to "hh:mm"
function formatDuration(seconds) {
    const h = Math.floor(seconds / 3600);
    const m = Math.floor((seconds % 3600) / 60);
    return `${h}h${m}m`;
}

// Helper to format ISO datetime string
function formatDateTime(dt) {
    const date = new Date(dt);
    return date.toLocaleString();
}
