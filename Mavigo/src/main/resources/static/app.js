const form = document.getElementById('journeyForm');
const resultsDiv = document.getElementById('results');

form.addEventListener('submit', async (e) => {
    e.preventDefault();
    resultsDiv.innerHTML = "Loading...";

    const from = document.getElementById('from').value;
    const to = document.getElementById('to').value;

    try {
        const response = await fetch(`http://localhost:8080/api/journeys?from=${encodeURIComponent(from)}&to=${encodeURIComponent(to)}`);
        if (!response.ok) throw new Error(`HTTP ${response.status}`);

        const data = await response.json();
        displayJourneys(data.journeys);
    } catch (err) {
        resultsDiv.innerHTML = `<p style="color:red">Error fetching journeys: ${err}</p>`;
        console.error(err);
    }
});

function displayJourneys(journeys) {
    if (!journeys || journeys.length === 0) {
        resultsDiv.innerHTML = '<p>No journeys found</p>';
        return;
    }

    resultsDiv.innerHTML = journeys.map(j => {
        const sections = j.sections.map(s =>
            `<li>${s.displayInformations?.commercialMode || s.type} from ${s.from.name} to ${s.to.name} (${s.duration}s)</li>`
        ).join('');
        return `
            <div class="journey">
                <h3>${j.departureDateTime} → ${j.arrivalDateTime} | Duration: ${j.duration}s | Transfers: ${j.nbTransfers}</h3>
                <ul>${sections}</ul>
            </div>
        `;
    }).join('<hr>');
}
