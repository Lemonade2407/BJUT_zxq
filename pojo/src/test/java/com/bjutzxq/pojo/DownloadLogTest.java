package com.bjutzxq.pojo;
import com.bjutzxq.pojo.entity.*;

import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

/**
 * DownloadLog 实体类测试
 */
class DownloadLogTest {

    @Test
    void testConstructorAndGetters() {
        DownloadLog downloadLog = new DownloadLog();
        assertNull(downloadLog.getId());
        assertNull(downloadLog.getFileId());
        assertNull(downloadLog.getUserId());
        assertNull(downloadLog.getIpAddress());
        assertNull(downloadLog.getCreatedAt());
    }

    @Test
    void testSetters() {
        DownloadLog downloadLog = new DownloadLog();
        LocalDateTime now = LocalDateTime.now();

        downloadLog.setId(1);
        downloadLog.setFileId(100);
        downloadLog.setUserId(200);
        downloadLog.setIpAddress("192.168.1.1");
        downloadLog.setCreatedAt(now);

        assertEquals(1, downloadLog.getId());
        assertEquals(100, downloadLog.getFileId());
        assertEquals(200, downloadLog.getUserId());
        assertEquals("192.168.1.1", downloadLog.getIpAddress());
        assertEquals(now, downloadLog.getCreatedAt());
    }

    @Test
    void testEqualsAndHashCode() {
        LocalDateTime now = LocalDateTime.now();

        DownloadLog log1 = new DownloadLog();
        log1.setId(1);
        log1.setFileId(100);
        log1.setUserId(200);
        log1.setCreatedAt(now);

        DownloadLog log2 = new DownloadLog();
        log2.setId(1);
        log2.setFileId(100);
        log2.setUserId(200);
        log2.setCreatedAt(now);

        assertEquals(log1, log2);
        assertEquals(log1.hashCode(), log2.hashCode());
    }

    @Test
    void testToString() {
        DownloadLog downloadLog = new DownloadLog();
        downloadLog.setId(1);
        downloadLog.setFileId(100);
        downloadLog.setIpAddress("192.168.1.1");

        String toString = downloadLog.toString();
        assertNotNull(toString);
        assertTrue(toString.contains("DownloadLog"));
        assertTrue(toString.contains("id=1"));
        assertTrue(toString.contains("ipAddress=192.168.1.1"));
    }

    @Test
    void testIPv6Address() {
        DownloadLog downloadLog = new DownloadLog();
        downloadLog.setIpAddress("::1");
        assertEquals("::1", downloadLog.getIpAddress());
    }

    @Test
    void testDifferentIPs() {
        DownloadLog log1 = new DownloadLog();
        log1.setIpAddress("192.168.1.1");

        DownloadLog log2 = new DownloadLog();
        log2.setIpAddress("192.168.1.2");

        assertNotEquals(log1, log2);
    }
}
