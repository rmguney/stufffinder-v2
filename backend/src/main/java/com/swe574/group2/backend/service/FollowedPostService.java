package com.swe574.group2.backend.service;

import com.swe574.group2.backend.dao.FollowedPostRepository;
import com.swe574.group2.backend.dao.PostRepository;
import com.swe574.group2.backend.dao.UserRepository;
import com.swe574.group2.backend.entity.FollowedPost;
import com.swe574.group2.backend.entity.Post;
import com.swe574.group2.backend.entity.User;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@Service
public class FollowedPostService {

    private final FollowedPostRepository followedPostRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;

    public FollowedPostService(FollowedPostRepository followedPostRepository,
            PostRepository postRepository,
            UserRepository userRepository) {
        this.followedPostRepository = followedPostRepository;
        this.postRepository = postRepository;
        this.userRepository = userRepository;
    }

    public void followPost(String followerUsername, Long postId) {
        User follower = userRepository.findByEmail(followerUsername).orElseThrow();
        Post post = postRepository.findById(postId).orElseThrow();

        if (followedPostRepository.findByFollowerAndPost(follower, post).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Already following this post");
        }

        FollowedPost followedPost = new FollowedPost();
        followedPost.setFollower(follower);
        followedPost.setPost(post);

        followedPostRepository.save(followedPost);
    }

    public void unfollowPost(String followerUsername, Long postId) {
        User follower = userRepository.findByEmail(followerUsername).orElseThrow();
        Post post = postRepository.findById(postId).orElseThrow();

        FollowedPost existingFollow = followedPostRepository.findByFollowerAndPost(follower, post).orElseThrow();

        followedPostRepository.delete(existingFollow);
    }

    public int getPostFollowerCount(Long postId) {
        Post post = postRepository.findById(postId).orElseThrow();
        return followedPostRepository.countByPost(post);
    }

    public boolean isFollowing(String followerUsername, Long postId) {
        User follower = userRepository.findByEmail(followerUsername).orElseThrow();
        Post post = postRepository.findById(postId).orElseThrow();
        return followedPostRepository.findByFollowerAndPost(follower, post).isPresent();
    }
    
}
