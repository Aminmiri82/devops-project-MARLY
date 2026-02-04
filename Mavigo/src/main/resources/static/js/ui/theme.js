export function initTheme() {
  const themeToggle = document.getElementById("themeToggle");
  const themeIcon = document.getElementById("themeIcon");

  const savedTheme = localStorage.getItem("mavigo_theme");
  const prefersDark = window.matchMedia("(prefers-color-scheme: dark)").matches;
  const initialTheme = savedTheme || (prefersDark ? "dark" : "light");

  setTheme(initialTheme);

  if (themeToggle) {
    themeToggle.classList.remove("hidden");
  }

  window
    .matchMedia("(prefers-color-scheme: dark)")
    .addEventListener("change", (e) => {
      if (!localStorage.getItem("mavigo_theme")) {
        setTheme(e.matches ? "dark" : "light");
      }
    });

  themeToggle?.addEventListener("click", () => {
    const currentTheme =
      document.documentElement.getAttribute("data-theme") || "light";
    const newTheme = currentTheme === "dark" ? "light" : "dark";
    setTheme(newTheme);
    localStorage.setItem("mavigo_theme", newTheme);
  });
}

export function setTheme(theme) {
  const themeIcon = document.getElementById("themeIcon");
  document.documentElement.setAttribute("data-theme", theme);

  if (themeIcon) {
    themeIcon.textContent = theme === "dark" ? "☀️" : "🌙";
  }
}
