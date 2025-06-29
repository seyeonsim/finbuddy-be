package com.http200ok.finbuddy.sign.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ViewController {

    @GetMapping({"/signup"})
    public String forward() {
        return "forward:/index.html";
    }
}
