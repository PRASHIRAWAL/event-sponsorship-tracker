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
                button.classList.remove("just-filled");
                void button.offsetWidth; // restart the highlight animation
                button.classList.add("just-filled");
            }
        });
    });

    var reduceMotion = window.matchMedia && window.matchMedia("(prefers-reduced-motion: reduce)").matches;

    // Caps Lock hint while typing the password
    var capsWarning = document.getElementById("caps-warning");
    var passwordField = document.getElementById("password");
    if (capsWarning && passwordField) {
        var checkCaps = function (event) {
            if (event.getModifierState) {
                capsWarning.hidden = !event.getModifierState("CapsLock");
            }
        };
        passwordField.addEventListener("keydown", checkCaps);
        passwordField.addEventListener("keyup", checkCaps);
        passwordField.addEventListener("blur", function () {
            capsWarning.hidden = true;
        });
    }

    // Spinner on the sign-in button while the form is submitting
    document.querySelectorAll("[data-login-form]").forEach(function (form) {
        form.addEventListener("submit", function () {
            var button = form.querySelector("button[type=submit]");
            if (button) {
                button.classList.add("is-loading");
                button.setAttribute("aria-busy", "true");
            }
        });
    });

    // Numbers that count up from zero: <span data-count-to="82" data-format="money">
    document.querySelectorAll("[data-count-to]").forEach(function (element) {
        var target = Number(element.getAttribute("data-count-to")) || 0;
        var money = element.getAttribute("data-format") === "money";
        var show = function (value) {
            element.textContent = money ? Math.round(value).toLocaleString("en-IN") : String(Math.round(value));
        };
        if (reduceMotion) {
            show(target);
            return;
        }
        var duration = 1800;
        var startAt = null;
        var step = function (time) {
            if (startAt === null) {
                startAt = time;
            }
            var progress = Math.min((time - startAt) / duration, 1);
            show(target * (1 - Math.pow(1 - progress, 3))); // ease-out
            if (progress < 1) {
                requestAnimationFrame(step);
            }
        };
        setTimeout(function () {
            requestAnimationFrame(step);
        }, 900);
    });

    // Figures already on the page count up from zero: <span data-count-up>545,000.00</span>, "76%" or "12"
    document.querySelectorAll("[data-count-up]").forEach(function (element, index) {
        var text = element.textContent.trim();
        var match = text.match(/^([^0-9-]*)(-?[0-9][0-9,]*)(\.([0-9]+))?(.*)$/);
        if (reduceMotion || !match) {
            return;
        }
        var prefix = match[1];
        var suffix = match[5];
        var decimals = match[4] ? match[4].length : 0;
        var target = Number((match[2] + (match[3] || "")).replace(/,/g, ""));
        var grouped = match[2].indexOf(",") !== -1 || target >= 1000;
        var format = function (value) {
            var fixed = value.toFixed(decimals);
            if (grouped) {
                var parts = fixed.split(".");
                parts[0] = parts[0].replace(/\B(?=(\d{3})+(?!\d))/g, ",");
                fixed = parts.join(".");
            }
            return prefix + fixed + suffix;
        };
        var duration = 1400;
        var startAt = null;
        var step = function (time) {
            if (startAt === null) {
                startAt = time;
            }
            var progress = Math.min((time - startAt) / duration, 1);
            element.textContent = progress < 1 ? format(target * (1 - Math.pow(1 - progress, 3))) : text;
            if (progress < 1) {
                requestAnimationFrame(step);
            }
        };
        element.textContent = format(0);
        setTimeout(function () {
            requestAnimationFrame(step);
        }, 200 + index * 60);
    });

    // Success messages fade away after a few seconds (errors stay until dismissed)
    document.querySelectorAll(".page .banner-success").forEach(function (banner) {
        setTimeout(function () {
            banner.classList.add("is-leaving");
            setTimeout(function () {
                banner.remove();
            }, 450);
        }, 7000);
    });

    // Floating cards drift slightly with the mouse (data-depth = how far)
    document.querySelectorAll("[data-parallax]").forEach(function (stage) {
        if (reduceMotion) {
            return;
        }
        var cards = stage.querySelectorAll("[data-depth]");
        var frame = null;
        window.addEventListener("pointermove", function (event) {
            if (frame) {
                return;
            }
            frame = requestAnimationFrame(function () {
                frame = null;
                var x = event.clientX / window.innerWidth - 0.5;
                var y = event.clientY / window.innerHeight - 0.5;
                cards.forEach(function (card) {
                    var depth = Number(card.getAttribute("data-depth")) || 0;
                    card.style.setProperty("--px", (-x * depth).toFixed(1) + "px");
                    card.style.setProperty("--py", (-y * depth).toFixed(1) + "px");
                });
            });
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
