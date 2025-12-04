package in.bored.api.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;

@Data
@Entity
@Table(name = "user_puzzle_view", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "user_profile_id", "puzzle_content_id" })
})
public class UserPuzzleView {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_profile_id", nullable = false)
    private UserProfile userProfile;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "puzzle_content_id", nullable = false)
    private PuzzleContent puzzleContent;

    @CreationTimestamp
    @Column(name = "viewed_at", updatable = false)
    private OffsetDateTime viewedAt;

    @Column(name = "is_correct")
    private Boolean isCorrect;
}
