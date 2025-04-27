package com.swe574.group2.backend.enums;

public enum FollowerNotificationType {
    // Follows a user who created a new post
    FOLLOWED_USER_POST_CREATED,

    // Follows a post, and these are triggered
    POST_UPVOTED,
    POST_DOWNVOTED,
    POST_COMMENTED,
    POST_RESOLVED
}
