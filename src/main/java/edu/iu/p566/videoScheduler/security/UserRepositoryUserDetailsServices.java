package edu.iu.p566.videoScheduler.security;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import edu.iu.p566.videoScheduler.data.UserRepository;
import edu.iu.p566.videoScheduler.model.User;

@Service
public class UserRepositoryUserDetailsServices implements UserDetailsService {
    private UserRepository userRepo;

    public UserRepositoryUserDetailsServices (UserRepository userRepo) {
        this.userRepo = userRepo;
    }

    @Override 
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepo.findByUsername(username);
        return user;
    }
}
