export function escapeHtml(str) {
  return String(str)
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#039;");
}

export function formatDuration(seconds) {
  if (seconds === null || seconds === undefined) return "?";
  const s = Number(seconds);
  if (Number.isNaN(s)) return "?";
  const h = Math.floor(s / 3600);
  const m = Math.floor((s % 3600) / 60);
  return h > 0 ? `${h}h ${m}m` : `${m}m`;
}

export function formatDateTime(dt) {
  return new Date(dt).toLocaleString();
}

export function formatMode(mode) {
  if (!mode) return "Unknown";
  if (mode === "OTHER") return "Connection";
  if (mode === "WALK") return "Walk";
  return mode.charAt(0).toUpperCase() + mode.slice(1).toLowerCase();
}

/**
 * Generates HTML for a transport line badge based on mode and line info.
 * - Metro/RER: Circle with line code (number or letter)
 * - Tram: Circle with T + number
 * - Bus: Rounded pill with line number
 * - Walk/Other: Icon-based representation
 */
export function formatLineBadge(mode, lineCode, lineColor) {
  if (!lineCode) {
    if (mode === "WALK" || mode === "WALKING") {
      return '<span class="line-badge line-badge-walk"></span>';
    }
    if (mode === "OTHER" || mode === "TRANSFER") {
      return '<span class="line-badge line-badge-transfer"></span>';
    }
    return "";
  }

  const bgColor = lineColor ? `#${lineColor}` : "#666";
  const textColor = getContrastColor(bgColor);

  let badgeClass = "line-badge";
  let displayCode = escapeHtml(lineCode);

  switch (mode) {
    case "METRO":
      badgeClass += " line-badge-metro";
      break;
    case "RER":
      badgeClass += " line-badge-rer";
      break;
    case "TRAM":
      badgeClass += " line-badge-tram";
      if (!lineCode.toUpperCase().startsWith("T")) {
        displayCode = "T" + displayCode;
      }
      break;
    case "BUS":
      badgeClass += " line-badge-bus";
      break;
    case "TRANSILIEN":
      badgeClass += " line-badge-transilien";
      break;
    default:
      badgeClass += " line-badge-other";
  }

  return `<span class="${badgeClass}" style="background-color: ${bgColor}; color: ${textColor};">${displayCode}</span>`;
}

/**
 * Returns black or white text color based on background color brightness.
 */
export function getContrastColor(hexColor) {
  const hex = hexColor.replace("#", "");
  const r = parseInt(hex.substr(0, 2), 16);
  const g = parseInt(hex.substr(2, 2), 16);
  const b = parseInt(hex.substr(4, 2), 16);
  const luminance = (0.299 * r + 0.587 * g + 0.114 * b) / 255;
  return luminance > 0.5 ? "#000000" : "#ffffff";
}

export function generateId() {
  if (window.crypto && typeof window.crypto.randomUUID === "function")
    return window.crypto.randomUUID();
  return `user-${Date.now()}`;
}

export function getTomorrowDateString() {
  const d = new Date();
  d.setDate(d.getDate() + 1);
  const yyyy = d.getFullYear();
  const mm = String(d.getMonth() + 1).padStart(2, "0");
  const dd = String(d.getDate()).padStart(2, "0");
  return `${yyyy}-${mm}-${dd}`;
}

export function getTomorrowDepartureLocalIso() {
  const d = new Date();
  d.setDate(d.getDate() + 1);
  d.setHours(9, 0, 0, 0);
  d.setMinutes(d.getMinutes() - d.getTimezoneOffset());
  return d.toISOString().slice(0, 16);
}
