package com.swe574.group2.backend.service;

import com.swe574.group2.backend.dao.FollowedUserRepository;
import com.swe574.group2.backend.dao.UserRepository;
import com.swe574.group2.backend.entity.FollowedUser;
import com.swe574.group2.backend.entity.User;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;



@Service
public class FollowedUserService {

    private final FollowedUserRepository followedUserRepository;
    private final UserRepository userRepository;

    public FollowedUserService(FollowedUserRepository followedUserRepository, UserRepository userRepository) {
        this.followedUserRepository = followedUserRepository;
        this.userRepository = userRepository;
    }

    public void followUser(String followerUsername, String followedUsername) {
        User follower = userRepository.findByEmail(followerUsername).orElseThrow();
        User followed = userRepository.findByUsername(followedUsername).orElseThrow();

        if (followedUserRepository.findByFollowerAndFollowed(follower, followed).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Already following this user");
        }

        FollowedUser followedUser = new FollowedUser();
        followedUser.setFollower(follower);
        followedUser.setFollowed(followed);

        followedUserRepository.save(followedUser);
    }

    public void unfollowUser(String followerUsername, String followedUsername) {
        User follower = userRepository.findByEmail(followerUsername).orElseThrow();
        User followed = userRepository.findByUsername(followedUsername).orElseThrow();

        FollowedUser existingFollow = followedUserRepository
                .findByFollowerAndFollowed(follower, followed)
                .orElseThrow();

        followedUserRepository.delete(existingFollow);
    }

    public int getFollowersCount(String userName) {
        User user = userRepository.findByUsername(userName).orElseThrow();
        return followedUserRepository.countByFollowed(user);
    }
    
    public int getFollowingCount(String userName) {
        User user = userRepository.findByUsername(userName).orElseThrow();
        return followedUserRepository.countByFollower(user);
    }

    
    

    public boolean isFollowing(String followerUsername, String followedUsername) {
        User follower = userRepository.findByEmail(followerUsername).orElseThrow();
        User followed = userRepository.findByUsername(followedUsername).orElseThrow();
        return followedUserRepository.findByFollowerAndFollowed(follower, followed).isPresent();
    }
    
}