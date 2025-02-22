package com.swe574.group2.backend.service;

import com.swe574.group2.backend.dao.CommentRepository;
import com.swe574.group2.backend.dao.PostRepository;
import com.swe574.group2.backend.dao.UserRepository;
import com.swe574.group2.backend.dto.CommentCreateDto;
import com.swe574.group2.backend.dto.CommentDetailsDto;
import com.swe574.group2.backend.entity.Comment;
import com.swe574.group2.backend.entity.Post;
import com.swe574.group2.backend.entity.User;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CommentService {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public CommentService(CommentRepository commentRepository, PostRepository postRepository, UserRepository userRepository, NotificationService notificationService) {
        this.commentRepository = commentRepository;
        this.postRepository = postRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    public Map<String, Long> createComment(CommentCreateDto commentCreateDto, String email) {
        User user = userRepository.findByEmail(email).orElseThrow();
        Post post = postRepository.findById(commentCreateDto.getPostId()).orElseThrow();

        Comment parentComment = null;
        if (commentCreateDto.getParentCommentId() != null) {
            parentComment = commentRepository.findById(commentCreateDto.getParentCommentId()).orElseThrow();
        }

        Comment comment = new Comment();
        comment.setContent(commentCreateDto.getContent());
        comment.setUser(user);
        comment.setPost(post);
        comment.setParentComment(parentComment);

        Comment savedComment = commentRepository.save(comment);

        notificationService.sendCommentNotification(post.getUser().getId(), post, savedComment);
        if(parentComment != null)
        {
            notificationService.sendCommentNotification(parentComment.getUser().getId(), post, savedComment);
        }

        Map<String, Long> response = new HashMap<>();
        response.put("commentId", savedComment.getId());

        return response;
    }

    public List<CommentDetailsDto> getCommentsForPost(Long postId, String email) {
        List<Comment> comments = commentRepository.findByPostId(postId);
        User user = email != null ? userRepository.findByEmail(email).orElseThrow() : null;

        List<CommentDetailsDto> commentDetailsDtos = new ArrayList<>();
        for (var comment : comments) {
            if (comment.getParentComment() == null)
            {
                commentDetailsDtos.add(mapCommentToDto(comment, user));
            }
        }

        return commentDetailsDtos;
    }

    public Map<String, Long> upvoteComment(Long commentId, String email) {
        User user = userRepository.findByEmail(email).orElseThrow();
        Comment comment = commentRepository.findById(commentId).orElseThrow();

        // Check if user already upvoted
        if (comment.getUpvotedBy().contains(user)) {
            // If already upvoted, remove the upvote
            comment.getUpvotedBy().remove(user);
            comment.setUpvotesCount(comment.getUpvotesCount() - 1);
        } else {
            // If not upvoted, add upvote
            comment.getUpvotedBy().add(user);
            comment.setUpvotesCount(comment.getUpvotesCount() + 1);

            // Remove downvote if exists
            if (comment.getDownvotedBy().contains(user)) {
                comment.getDownvotedBy().remove(user);
                comment.setDownvotesCount(comment.getDownvotesCount() - 1);
            }
        }

        commentRepository.save(comment);
        notificationService.sendUpvoteNotification(comment.getUser().getId(), comment);

        Map<String, Long> response = new HashMap<>();
        response.put("commentId", comment.getId());

        return response;
    }

    public Map<String, Long> downvoteComment(Long commentId, String email) {
        User user = userRepository.findByEmail(email).orElseThrow();
        Comment comment = commentRepository.findById(commentId).orElseThrow();

        // Check if user already downvoted
        if (comment.getDownvotedBy().contains(user)) {
            // If already downvoted, remove the downvote
            comment.getDownvotedBy().remove(user);
            comment.setDownvotesCount(comment.getDownvotesCount() - 1);
        } else {
            // If not downvoted, add downvote
            comment.getDownvotedBy().add(user);
            comment.setDownvotesCount(comment.getDownvotesCount() + 1);

            // Remove upvote if exists
            if (comment.getUpvotedBy().contains(user)) {
                comment.getUpvotedBy().remove(user);
                comment.setUpvotesCount(comment.getUpvotesCount() - 1);
            }
        }

        commentRepository.save(comment);

        Map<String, Long> response = new HashMap<>();
        response.put("commentId", comment.getId());

        return response;
    }

    public List<CommentDetailsDto> getUserComments(Long userId) {
        List<Comment> comments = commentRepository.findByUserId(userId);
        User currentUser = userRepository.findById(userId).orElseThrow();

        return comments.stream()
                .map(comment -> mapCommentToDto(comment, currentUser))
                .collect(Collectors.toList());
    }

    private CommentDetailsDto mapCommentToDto(Comment comment, User currentUser) {
        boolean userUpvoted = currentUser != null ? comment.getUpvotedBy().contains(currentUser) : false;
        boolean userDownvoted = currentUser != null ? comment.getDownvotedBy().contains(currentUser) : false;

        return new CommentDetailsDto(
                comment.getId(),
                comment.getContent(),
                comment.getUser().getUsername(),
                comment.getCreatedAt(),
                comment.getUpdatedAt(),
                comment.getReplies().stream()
                        .map(reply -> mapCommentToDto(reply, currentUser))
                        .collect(Collectors.toList()),
                comment.getUpvotesCount(),
                comment.getDownvotesCount(),
                userUpvoted,
                userDownvoted,
                comment.isBestAnswer(),
                comment.getPost().getId()
        );
    }
}
