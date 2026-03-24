package edu.iu.p566.videoScheduler.controllers;

import java.security.Principal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import edu.iu.p566.videoScheduler.data.ScheduleRepository;
import edu.iu.p566.videoScheduler.data.UserRepository;
import edu.iu.p566.videoScheduler.model.Schedule;
import edu.iu.p566.videoScheduler.model.User;
import edu.iu.p566.videoScheduler.security.YoutubeService;

import org.springframework.ui.Model;

import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/schedule")
@RequiredArgsConstructor
public class ScheduleController {
    private final ScheduleRepository scheduleRepo;
    private final UserRepository userRepo;
    private final YoutubeService youtubeService;

    @GetMapping()
    public String displaySchedule(Model model, Principal principal,
            @RequestParam(required = false) Boolean override) {

        String username = principal.getName();

        Optional<Schedule> dueVideo =
            scheduleRepo.findFirstByUserUsernameAndSchedTimeUtcLessThanEqualOrderBySchedTimeUtcAsc(
                username,
                Instant.now()
            );

        if (dueVideo.isPresent() && (override == null || !override)) {
            return "redirect:/";
        }

        List<Schedule> schedules = scheduleRepo.findByUserUsername(username);

        model.addAttribute("schedules", schedules);
        model.addAttribute("username", username);
        model.addAttribute("schedule", new Schedule());

        return "schedule";
    }

    @PostMapping("/delete/{id}")
    public String deleteSchedule(@PathVariable Long id, Principal principal) {

        Optional<Schedule> scheduleOpt = scheduleRepo.findById(id);

        if(scheduleOpt.isPresent()) {

            Schedule sched = scheduleOpt.get();

            if(sched.getUser().getUsername().equals(principal.getName())) {
                scheduleRepo.deleteById(id);
            }
        }

        return "redirect:/schedule";
    }
    @PostMapping()
    public String saveSchedule(
        @ModelAttribute Schedule schedule,
        Principal principal,
        Model model) {

    String username = principal.getName();
    User user = userRepo.findByUsername(username);

    schedule.setUser(user);

    if (schedule.getTimeZone() == null || schedule.getTimeZone().isBlank()) {
        schedule.setTimeZone("America/New_York");
    }

    long duration = youtubeService.getVideoDuration(schedule.getYoutubeURL());
    schedule.setDurationSeconds(duration);

    LocalDateTime localTime = schedule.getSchedTime();
    ZoneId userZone = ZoneId.of(schedule.getTimeZone());

    Instant newStart = localTime.atZone(userZone).toInstant();
    Instant newEnd = newStart.plusSeconds(duration);

    List<Schedule> existingSchedules = scheduleRepo.findByUserUsername(username);

    for (Schedule existing : existingSchedules) {

        Instant existingStart = existing.getSchedTimeUtc();

        if (existingStart == null) {
            existingStart = existing.getSchedTime()
                    .atZone(ZoneId.of(existing.getTimeZone())) // ✅ FIX HERE TOO
                    .toInstant();
        }

        Instant existingEnd = existingStart.plusSeconds(existing.getDurationSeconds());

        if (newStart.isBefore(existingEnd) && newEnd.isAfter(existingStart)) {

            model.addAttribute("error", "Video overlaps with an existing scheduled video.");
            model.addAttribute("schedules", existingSchedules);
            model.addAttribute("schedule", schedule);
            model.addAttribute("username", username);

            return "schedule";
        }
    }

    schedule.setSchedTimeUtc(newStart);

    scheduleRepo.save(schedule);

    return "redirect:/schedule";
}
}
