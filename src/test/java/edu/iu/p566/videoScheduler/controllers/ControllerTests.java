package edu.iu.p566.videoScheduler.controllers;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import edu.iu.p566.videoScheduler.data.ScheduleRepository;
import edu.iu.p566.videoScheduler.data.UserRepository;
import edu.iu.p566.videoScheduler.model.Schedule;
import edu.iu.p566.videoScheduler.model.User;
import edu.iu.p566.videoScheduler.security.YoutubeService;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class ControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ScheduleRepository scheduleRepo;

    @MockBean
    private UserRepository userRepo;

    @MockBean
    private YoutubeService youtubeService;

    @Test
    void testLoginPageLoads() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("login"));
    }

    @Test
    void testRegisterPageLoads() throws Exception {
        mockMvc.perform(get("/register"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"));
    }

    @Test
    void testPostScheduleSuccess() throws Exception {

        User user = new User();
        user.setUsername("test");
        user.setTimezone("America/New_York");

        when(userRepo.findByUsername("test")).thenReturn(user);
        when(scheduleRepo.findByUserUsername("test")).thenReturn(List.of());
        when(youtubeService.getVideoDuration(anyString())).thenReturn(60L);

        mockMvc.perform(post("/schedule")
                .with(user("test"))
                .with(csrf())
                .param("videoName", "Test Video")
                .param("youtubeURL", "http://youtube.com/test")
                .param("schedTime", "2026-03-25T10:00"))
            .andExpect(status().is3xxRedirection())
            .andExpect(view().name("redirect:/schedule"));

        verify(scheduleRepo, times(1)).save(org.mockito.ArgumentMatchers.any(Schedule.class));
    }

    @Test
    void testOverlapScheduleFails() throws Exception {

        User user = new User();
        user.setUsername("test");
        user.setTimezone("America/New_York");

        Instant existingStart = Instant.parse("2026-03-25T14:00:00Z"); 
        Schedule existing = new Schedule();
        existing.setSchedTime(existingStart);
        existing.setDurationSeconds(300L); 

        when(userRepo.findByUsername("test")).thenReturn(user);

        when(scheduleRepo
            .findFirstByUserUsernameAndSchedTimeLessThanEqualOrderBySchedTimeAsc(
                eq("test"), any()))
            .thenReturn(Optional.empty());

        when(scheduleRepo.findByUserUsername("test")).thenReturn(List.of(existing));

        when(youtubeService.getVideoDuration(anyString())).thenReturn(300L);

        mockMvc.perform(post("/schedule")
                .with(user("test"))
                .with(csrf())
                .param("videoName", "Overlap Video")
                .param("youtubeURL", "http://youtube.com/test")
                .param("schedTime", "2026-03-25T10:02")) 
            .andExpect(status().isOk())
            .andExpect(model().attributeExists("error"))
            .andExpect(model().attribute("error",
                    "Video overlaps with an existing scheduled video."))
            .andExpect(view().name("schedule"));

        verify(scheduleRepo, never()).save(any());
    }

    @Test
    void testDeleteSchedule() throws Exception {

        User user = new User();
        user.setUsername("test");

        Schedule schedule = new Schedule();
        schedule.setVideoID(1L);
        schedule.setUser(user);

        when(scheduleRepo.findById(1L)).thenReturn(Optional.of(schedule));

        mockMvc.perform(
                post("/schedule/delete/1")
                    .with(user("test"))
                    .with(csrf())
            )
            .andExpect(status().is3xxRedirection())
            .andExpect(view().name("redirect:/schedule"));

        verify(scheduleRepo, times(1)).deleteById(1L);
    }

    @Test
    void testRedirectToHomeWhenVideoDue() throws Exception {

        Schedule due = new Schedule();
        due.setSchedTime(Instant.now().minusSeconds(10)); // already due

        when(scheduleRepo
                .findFirstByUserUsernameAndSchedTimeLessThanEqualOrderBySchedTimeAsc(
                        eq("test"), any()))
            .thenReturn(Optional.of(due));

        mockMvc.perform(get("/schedule")
                .with(user("test")))
            .andExpect(status().is3xxRedirection())
            .andExpect(view().name("redirect:/"));
    }

    @Test
    void testOverridePreventsRedirect() throws Exception {

        Schedule due = new Schedule();
        due.setSchedTime(Instant.now().minusSeconds(10));

        when(scheduleRepo
                .findFirstByUserUsernameAndSchedTimeLessThanEqualOrderBySchedTimeAsc(
                        eq("test"), any()))
            .thenReturn(Optional.of(due));

        when(scheduleRepo.findByUserUsername("test")).thenReturn(List.of());

        mockMvc.perform(get("/schedule")
                .param("override", "true")
                .with(user("test")))
            .andExpect(status().isOk())
            .andExpect(view().name("schedule"));
    }
}