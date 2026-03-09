package com.yunchat.chat.domain.friend.entity;

import com.yunchat.chat.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"user_id", "friend_id"})
        }
)
public class Friend {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 요청 보낸 사람
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    // 친구 대상
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "friend_id")
    private User friend;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FriendStatus status;

    public Friend(User user, User friend) {
        this.user = user;
        this.friend = friend;
        this.status = FriendStatus.PENDING;
    }

    public void accept() {
        this.status = FriendStatus.ACCEPTED;
    }

    public void block() {
        this.status = FriendStatus.BLOCKED;
    }
}