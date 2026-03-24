package edu.iu.p566.videoScheduler.controllers;

import java.security.Principal;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import edu.iu.p566.videoScheduler.data.ScheduleRepository;
import edu.iu.p566.videoScheduler.model.Schedule;

@Controller
public class HomeController {

    @Autowired
    ScheduleRepository schedRepo;

    @GetMapping("/")
    public String home(Model model, Principal principal) {

        String username = principal.getName();

        Instant now = Instant.now();

        Optional<Schedule> video =
            schedRepo.findFirstByUserUsernameAndSchedTimeLessThanEqualAndEndTimeGreaterThanEqualOrderBySchedTimeAsc(
                username, now, now
            );

        if (video.isPresent()) {

            Schedule v = video.get();

            model.addAttribute("video", v);

            long offset = Duration.between(
                v.getSchedTime(),
                Instant.now()
            ).getSeconds();

            if (offset < 30) {
                offset = 0;
            }

            model.addAttribute("startOffset", offset);

            schedRepo.save(v);
        }

        return "home";
    }
}