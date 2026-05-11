package com.bjutzxq.pojo;
import com.bjutzxq.pojo.entity.*;

import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Comment 实体类测试
 */
class CommentTest {

    @Test
    void testConstructorAndGetters() {
        Comment comment = new Comment();
        assertNull(comment.getId());
        assertNull(comment.getUserId());
        assertNull(comment.getProjectId());
        assertNull(comment.getContent());
        assertNull(comment.getLikeCount());
        assertNull(comment.getCreatedAt());
        assertNull(comment.getUpdatedAt());
        assertNull(comment.getStatus());
    }

    @Test
    void testSetters() {
        Comment comment = new Comment();
        LocalDateTime now = LocalDateTime.now();

        comment.setId(1);
        comment.setUserId(100);
        comment.setProjectId(200);
        comment.setContent("这是一条评论");
        comment.setLikeCount(10);
        comment.setCreatedAt(now);
        comment.setUpdatedAt(now);
        comment.setStatus(1);

        assertEquals(1, comment.getId());
        assertEquals(100, comment.getUserId());
        assertEquals(200, comment.getProjectId());
        assertEquals("这是一条评论", comment.getContent());
        assertEquals(10, comment.getLikeCount());
        assertEquals(now, comment.getCreatedAt());
        assertEquals(now, comment.getUpdatedAt());
        assertEquals(1, comment.getStatus());
    }

    @Test
    void testEqualsAndHashCode() {
        LocalDateTime now = LocalDateTime.now();

        Comment comment1 = new Comment();
        comment1.setId(1);
        comment1.setUserId(100);
        comment1.setContent("测试评论");
        comment1.setCreatedAt(now);

        Comment comment2 = new Comment();
        comment2.setId(1);
        comment2.setUserId(100);
        comment2.setContent("测试评论");
        comment2.setCreatedAt(now);

        assertEquals(comment1, comment2);
        assertEquals(comment1.hashCode(), comment2.hashCode());
    }

    @Test
    void testToString() {
        Comment comment = new Comment();
        comment.setId(1);
        comment.setContent("测试评论");
        comment.setLikeCount(5);

        String toString = comment.toString();
        assertNotNull(toString);
        assertTrue(toString.contains("Comment"));
        assertTrue(toString.contains("id=1"));
        assertTrue(toString.contains("content=测试评论"));
    }

    @Test
    void testCommentStatus() {
        Comment activeComment = new Comment();
        activeComment.setStatus(1);
        assertEquals(1, activeComment.getStatus());

        Comment deletedComment = new Comment();
        deletedComment.setStatus(0);
        assertEquals(0, deletedComment.getStatus());
    }

    @Test
    void testZeroLikeCount() {
        Comment comment = new Comment();
        comment.setLikeCount(0);

        assertEquals(0, comment.getLikeCount());
    }
}
