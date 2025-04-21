package com.swe574.group2.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_badges")
@Data
@NoArgsConstructor
@AllArgsConstructor
@IdClass(UserBadgeId.class) // Use a composite key class
public class UserBadge {

    @Id
    @ManyToOne
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    private User user;

    @Id
    @ManyToOne
    @JoinColumn(name = "badge_id", referencedColumnName = "id")
    private Badge badge;

    @Column(nullable = false)
    private LocalDateTime earnedDate = LocalDateTime.now();
}
