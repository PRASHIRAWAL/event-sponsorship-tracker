/*
 * Event Sponsorship Tracker - small progressive enhancements. Every page works without JavaScript;
 * this only adds the theme switch, menus, confirmation dialogs, banners and sign-in helpers.
 */
(function () {
    "use strict";

    // The saved theme is applied by the inline script in the page head (before first paint)
    var THEME_KEY = "est-theme";
    var root = document.documentElement;

    function currentTheme() {
        var theme = root.getAttribute("data-theme");
        if (theme) {
            return theme;
        }
        return window.matchMedia && window.matchMedia("(prefers-color-scheme: dark)").matches ? "dark" : "light";
    }

    // Theme toggle: remembers the choice for this browser only
    document.querySelectorAll("[data-theme-toggle]").forEach(function (button) {
        button.addEventListener("click", function () {
            var next = currentTheme() === "dark" ? "light" : "dark";
            root.setAttribute("data-theme", next);
            try {
                localStorage.setItem(THEME_KEY, next);
            } catch (e) {
                // Storage can be blocked (private mode); the theme still applies to this page
            }
            button.setAttribute("aria-pressed", String(next === "dark"));
        });
        button.setAttribute("aria-pressed", String(currentTheme() === "dark"));
    });

    // Mobile navigation: the section links fold into a drop-down on small screens
    var body = document.body;
    var navToggle = document.querySelector("[data-nav-toggle]");
    function setNavOpen(open) {
        body.classList.toggle("nav-open", open);
        if (navToggle) {
            navToggle.setAttribute("aria-expanded", String(open));
            navToggle.setAttribute("aria-label", open ? "Close menu" : "Open menu");
        }
    }
    if (navToggle) {
        navToggle.addEventListener("click", function () {
            setNavOpen(!body.classList.contains("nav-open"));
        });
    }

    // Drop-down menus (<details class="menu">): only one open at a time, closed by outside click or Escape
    var menus = document.querySelectorAll("details.menu");
    menus.forEach(function (menu) {
        menu.addEventListener("toggle", function () {
            if (menu.open) {
                menus.forEach(function (other) {
                    if (other !== menu) {
                        other.open = false;
                    }
                });
                setNavOpen(false);
            }
        });
    });
    document.addEventListener("click", function (event) {
        menus.forEach(function (menu) {
            if (menu.open && !menu.contains(event.target)) {
                menu.open = false;
            }
        });
        if (body.classList.contains("nav-open") && !event.target.closest(".appbar")) {
            setNavOpen(false);
        }
    });
    document.addEventListener("keydown", function (event) {
        if (event.key === "Escape") {
            menus.forEach(function (menu) {
                menu.open = false;
            });
            setNavOpen(false);
        }
    });

    // Sign-in page: show/hide the password and fill in a demo account
    document.querySelectorAll("[data-reveal]").forEach(function (button) {
        var field = document.getElementById(button.getAttribute("data-reveal"));
        if (!field) {
            return;
        }
        button.addEventListener("click", function () {
            var show = field.type === "password";
            field.type = show ? "text" : "password";
            field.toggleAttribute("data-revealed", show);
            button.textContent = show ? "Hide" : "Show";
            button.setAttribute("aria-pressed", String(show));
            button.setAttribute("aria-label", show ? "Hide password" : "Show password");
        });
    });
    document.querySelectorAll("[data-fill-user]").forEach(function (button) {
        button.addEventListener("click", function () {
            var user = document.getElementById("username");
            var pass = document.getElementById("password");
            if (user && pass) {
                user.value = button.getAttribute("data-fill-user");
                pass.value = button.getAttribute("data-fill-pass");
                pass.focus();
            }
        });
    });

    // Dismissible banners
    document.querySelectorAll("[data-dismiss]").forEach(function (button) {
        button.addEventListener("click", function () {
            var banner = button.closest(".banner");
            if (banner) {
                banner.remove();
            }
        });
    });

    // Confirmation for destructive forms: <form data-confirm="Delete X?" data-confirm-detail="...">
    var dialog = document.getElementById("confirm-dialog");
    var pendingForm = null;
    document.querySelectorAll("form[data-confirm]").forEach(function (form) {
        form.addEventListener("submit", function (event) {
            if (form.dataset.confirmed === "true") {
                return;
            }
            event.preventDefault();
            var title = form.getAttribute("data-confirm");
            var detail = form.getAttribute("data-confirm-detail") || "This cannot be undone.";
            if (!dialog || typeof dialog.showModal !== "function") {
                if (window.confirm(title + "\n\n" + detail)) {
                    form.dataset.confirmed = "true";
                    form.submit();
                }
                return;
            }
            pendingForm = form;
            dialog.querySelector("[data-confirm-title]").textContent = title;
            dialog.querySelector("[data-confirm-detail]").textContent = detail;
            dialog.showModal();
        });
    });
    if (dialog) {
        dialog.addEventListener("close", function () {
            if (dialog.returnValue === "confirm" && pendingForm) {
                pendingForm.dataset.confirmed = "true";
                pendingForm.submit();
            }
            pendingForm = null;
            dialog.returnValue = "";
        });
    }

    // Live character counters: <textarea data-count="id-of-counter">
    document.querySelectorAll("[data-count]").forEach(function (field) {
        var counter = document.getElementById(field.getAttribute("data-count"));
        if (!counter) {
            return;
        }
        var max = field.getAttribute("maxlength");
        var update = function () {
            counter.textContent = field.value.length + (max ? " / " + max : "");
        };
        field.addEventListener("input", update);
        update();
    });

    // "/" focuses the global search (like many web apps), unless the user is typing
    document.addEventListener("keydown", function (event) {
        if (event.key !== "/" || event.ctrlKey || event.metaKey || event.altKey) {
            return;
        }
        var tag = (event.target.tagName || "").toLowerCase();
        if (tag === "input" || tag === "textarea" || tag === "select" || event.target.isContentEditable) {
            return;
        }
        var search = document.getElementById("global-search");
        if (search) {
            event.preventDefault();
            search.focus();
        }
    });
})();
