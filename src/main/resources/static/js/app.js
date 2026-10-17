/*
 * Event Sponsorship Tracker - small progressive enhancements. Every page works without JavaScript;
 * this only adds the theme switch, the mobile menu, confirmation dialogs and dismissible banners.
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

    // Mobile sidebar
    var body = document.body;
    document.querySelectorAll("[data-sidebar-toggle]").forEach(function (button) {
        button.addEventListener("click", function () {
            var open = body.classList.toggle("sidebar-open");
            button.setAttribute("aria-expanded", String(open));
        });
    });
    document.querySelectorAll("[data-sidebar-close]").forEach(function (element) {
        element.addEventListener("click", function () {
            body.classList.remove("sidebar-open");
        });
    });
    document.addEventListener("keydown", function (event) {
        if (event.key === "Escape") {
            body.classList.remove("sidebar-open");
        }
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
