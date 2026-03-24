package edu.iu.p566.videoScheduler.controllers;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import edu.iu.p566.videoScheduler.data.UserRepository;
import edu.iu.p566.videoScheduler.model.User;

import org.springframework.ui.Model;

@RequestMapping("/register")
@Controller
public class RegistrationController {
    private UserRepository userRepo;
    private PasswordEncoder passEnco;

    public RegistrationController(UserRepository userRepo, PasswordEncoder passEnco) {
        this.userRepo = userRepo;
        this.passEnco = passEnco;
    }

    @GetMapping()
    public String registerForm(Model model) {
        model.addAttribute("user", new User());
        return "register";
    }
    @PostMapping()
    public String processRegistration(@ModelAttribute User user) {
        user.setPassword(passEnco.encode(user.getPassword()));

        if (user.getTimeZone() == null) {
            user.setTimeZone("America/New_York"); 
        }
        userRepo.save(user);
        return "redirect:/login";
    }
}
