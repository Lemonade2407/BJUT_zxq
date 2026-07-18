-- Refresh Token 原子化轮换脚本
-- 在单个 Redis 事务中完成：验证旧 token -> 标记已消费 -> 删除旧 token -> 创建新 token
--
-- KEYS[1] = jwt:refresh:{sha256(oldToken)}
-- KEYS[2] = jwt:refresh-consumed:{sha256(oldToken)}
-- KEYS[3] = jwt:refresh:{sha256(newToken)}
-- KEYS[4] = jwt:user-refresh-list:{userId}
-- ARGV[1] = family (UUID 家族标识)
-- ARGV[2] = gen (新代数)
-- ARGV[3] = userId
-- ARGV[4] = TTL (秒)
-- ARGV[5] = currentUserVersion (期望的 token 版本号)

-- 1. 检查是否已被消费（重放攻击检测）
local consumed = redis.call('EXISTS', KEYS[2])
if consumed == 1 then
    -- 重放攻击：递增用户版本号使所有 token 失效
    local verKey = 'jwt:user-version:' .. ARGV[3]
    redis.call('INCR', verKey)
    redis.log(redis.LOG_WARNING, 'Token reuse detected for user ' .. ARGV[3] .. ', all sessions invalidated')
    return {0, 'TOKEN_REUSED'}
end

-- 2. 读取旧 token 数据
local oldUserId = redis.call('HGET', KEYS[1], 'userId')
if not oldUserId then
    return {0, 'TOKEN_NOT_FOUND'}
end

-- 3. 验证版本号是否匹配
local oldVer = redis.call('HGET', KEYS[1], 'ver')
if oldVer ~= ARGV[5] then
    return {0, 'VERSION_MISMATCH'}
end

-- 4. 标记旧 token 为已消费
redis.call('SET', KEYS[2], '1', 'EX', ARGV[4])

-- 5. 删除旧 token
redis.call('DEL', KEYS[1])

-- 6. 创建新 token hash
redis.call('HSET', KEYS[3],
    'userId', ARGV[3],
    'family', ARGV[1],
    'gen', ARGV[2],
    'ver', ARGV[5]
)
redis.call('EXPIRE', KEYS[3], ARGV[4])

-- 7. 更新用户的 token 列表
redis.call('SADD', KEYS[4], KEYS[3])
redis.call('EXPIRE', KEYS[4], ARGV[4])

return {1, 'OK'}
