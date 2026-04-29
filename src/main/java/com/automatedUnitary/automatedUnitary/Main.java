package com.automatedUnitary.automatedUnitary;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Controller
public class Main {

    @Autowired
    private OpenAIService openAIService;

    @GetMapping("/")
    public String index() {
        return "index";
    }

    @PostMapping("/generate")
    public String generate(
            @RequestParam(value = "code", required = false) String code,
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam("testType") String testType,
            Model model) {

        try {
            String javaCode = code;

            if (file != null && !file.isEmpty()) {
                javaCode = new String(file.getBytes());
            }

            if (javaCode == null || javaCode.trim().isEmpty()) {
                model.addAttribute("error", "Please paste or upload your code here in .java");
                return "index";
            }

            if (javaCode.length() > 8000) {
                javaCode = javaCode.substring(0, 8000);
            }

            String result = openAIService.generateTests(javaCode, testType);
            model.addAttribute("result", result);
            model.addAttribute("code", code);
            model.addAttribute("testType", testType);

        } catch (Exception e) {
            model.addAttribute("error", "Error: " + e.getMessage());
        }

        return "index";
    }
}