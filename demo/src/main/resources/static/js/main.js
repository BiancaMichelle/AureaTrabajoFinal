const navToggle = document.querySelector(".nav-toggle");
const navMenu = document.querySelector(".nav-menu");

navToggle.addEventListener("click", () => {
    navMenu.classList.toggle("nav-menu_visible");
});

function toggleDropdown() {
    document.getElementById("dropdownMenu").classList.toggle("show");
}

// Cierra el dropdown si se hace clic fuera de él
window.onclick = function(event) {
    if (!event.target.matches('.profile-pic')) {
        var dropdowns = document.getElementsByClassName("dropdown-menu");
        for (var i = 0; i < dropdowns.length; i++) {
            var openDropdown = dropdowns[i];
            if (openDropdown.classList.contains('show')) {
                openDropdown.classList.remove('show');
            }
        }
    }
}

