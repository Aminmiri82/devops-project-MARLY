package org.marly.mavigo.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class FrontendController {

    @GetMapping({"/search", "/tasks", "/results"})
    public String forwardToIndex() {
        return "forward:/index.html";
    }
}
