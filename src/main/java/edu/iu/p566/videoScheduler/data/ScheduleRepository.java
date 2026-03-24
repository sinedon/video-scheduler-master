package edu.iu.p566.videoScheduler.data;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.repository.CrudRepository;

import edu.iu.p566.videoScheduler.model.Schedule;

public interface ScheduleRepository extends CrudRepository<Schedule,Long> {
    List<Schedule> findByUserUsername(String username);
    Optional<Schedule> findFirstByUserUsernameAndSchedTimeLessThanEqualOrderBySchedTimeAsc(
        String username,
        Instant time
    );
}
