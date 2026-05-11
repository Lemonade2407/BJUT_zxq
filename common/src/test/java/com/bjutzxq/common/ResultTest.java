package com.bjutzxq.common;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Result 类测试
 */
class ResultTest {

    @Test
    void testDefaultConstructor() {
        Result<String> result = new Result<>();
        assertNull(result.getCode());
        assertNull(result.getMessage());
        assertNull(result.getData());
    }

    @Test
    void testParameterizedConstructor() {
        Result<String> result = new Result<>(200, "success", "test data");
        assertEquals(200, result.getCode());
        assertEquals("success", result.getMessage());
        assertEquals("test data", result.getData());
    }

    @Test
    void testSuccessWithoutData() {
        Result<Void> result = Result.success();
        assertEquals(200, result.getCode());
        assertEquals("success", result.getMessage());
        assertNull(result.getData());
    }

    @Test
    void testSuccessWithData() {
        String testData = "test data";
        Result<String> result = Result.success(testData);
        assertEquals(200, result.getCode());
        assertEquals("success", result.getMessage());
        assertEquals(testData, result.getData());
    }

    @Test
    void testSuccessWithMessageAndData() {
        String testData = "test data";
        String message = "operation successful";
        Result<String> result = Result.success(message, testData);
        assertEquals(200, result.getCode());
        assertEquals(message, result.getMessage());
        assertEquals(testData, result.getData());
    }

    @Test
    void testErrorWithMessage() {
        String errorMessage = "error occurred";
        Result<Void> result = Result.error(errorMessage);
        assertEquals(500, result.getCode());
        assertEquals(errorMessage, result.getMessage());
        assertNull(result.getData());
    }

    @Test
    void testErrorWithCodeAndMessage() {
        Integer errorCode = 404;
        String errorMessage = "not found";
        Result<Void> result = Result.error(errorCode, errorMessage);
        assertEquals(errorCode, result.getCode());
        assertEquals(errorMessage, result.getMessage());
        assertNull(result.getData());
    }

    @Test
    void testSettersAndGetters() {
        Result<Integer> result = new Result<>();
        result.setCode(200);
        result.setMessage("ok");
        result.setData(42);

        assertEquals(200, result.getCode());
        assertEquals("ok", result.getMessage());
        assertEquals(42, result.getData());
    }

    @Test
    void testResultWithComplexData() {
        // 测试使用复杂对象作为数据
        java.util.Map<String, Object> complexData = new java.util.HashMap<>();
        complexData.put("name", "test");
        complexData.put("value", 123);

        Result<java.util.Map<String, Object>> result = Result.success(complexData);
        assertEquals(200, result.getCode());
        assertNotNull(result.getData());
        assertEquals("test", result.getData().get("name"));
        assertEquals(123, result.getData().get("value"));
    }

    @Test
    void testDifferentStatusCodes() {
        // 测试不同的状态码
        Result<Void> badRequest = Result.error(400, "Bad Request");
        assertEquals(400, badRequest.getCode());

        Result<Void> unauthorized = Result.error(401, "Unauthorized");
        assertEquals(401, unauthorized.getCode());

        Result<Void> forbidden = Result.error(403, "Forbidden");
        assertEquals(403, forbidden.getCode());
    }
}
