package com.eventtracker.controller;

import com.eventtracker.security.SecurityProperties;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/** Sign-in page. Spring Security handles the form POST to /login and the POST to /logout. */
@Controller
public class LoginController {

    private final SecurityProperties properties;

    public LoginController(SecurityProperties properties) {
        this.properties = properties;
    }

    @GetMapping("/login")
    public String login(Authentication authentication, Model model) {
        if (authentication != null && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken)) {
            return "redirect:/dashboard";
        }
        if (properties.demoHint()) {
            model.addAttribute("demoAdmin", properties.adminUsername());
            model.addAttribute("demoAdminPassword", properties.adminPassword());
            if (properties.viewerEnabled()) {
                model.addAttribute("demoViewer", properties.viewerUsername());
                model.addAttribute("demoViewerPassword", properties.viewerPassword());
            }
        }
        return "login";
    }
}
