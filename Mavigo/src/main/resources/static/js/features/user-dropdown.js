const userDropdownTrigger = document.getElementById("userDropdownTrigger");
const userDropdown = document.getElementById("userDropdown");

export function setupDropdown() {
  if (!userDropdownTrigger || !userDropdown) return;

  userDropdownTrigger.addEventListener("click", (e) => {
    e.stopPropagation();
    toggleDropdown();
  });

  document.addEventListener("click", (e) => {
    if (!userDropdown.classList.contains("open")) return;
    if (!userDropdown.contains(e.target) && e.target !== userDropdownTrigger) {
      closeDropdown();
    }
  });

  document.addEventListener("keydown", (e) => {
    if (e.key === "Escape" && userDropdown.classList.contains("open")) {
      closeDropdown();
    }
  });
}

function toggleDropdown() {
  const isOpen = userDropdown.classList.contains("open");
  if (isOpen) {
    closeDropdown();
  } else {
    openDropdown();
  }
}

function openDropdown() {
  userDropdown.classList.add("open");
  userDropdownTrigger.classList.add("open");
}

function closeDropdown() {
  userDropdown.classList.remove("open");
  userDropdownTrigger.classList.remove("open");
}
