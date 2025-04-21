package com.swe574.group2.backend.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class UserBadgeId implements Serializable {
    private Long user; // Corresponds to the 'user' field in UserBadge, which is of type User
    private Long badge; // Corresponds to the 'badge' field in UserBadge, which is of type Badge
}
