package com.dk0124.couponcore.repository.redis;



import com.dk0124.couponcore.RedisKey;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Repository;

import java.util.List;

import static com.dk0124.couponcore.RedisKey.*;

@RequiredArgsConstructor
@Repository
@Slf4j
public class RedisRepository {

    private final RedisTemplate<String, String> redisTemplate;

    private final ObjectMapper objectMapper;
    private final RedisScript asyncIssueScript = issueRequestScript();


    public Long sAdd(String key, String value) {
        return redisTemplate.opsForSet().add(key, value);
    }

    public Boolean zAdd(String key, String value, double score) {
        return redisTemplate.opsForZSet().add(key, value, score);
    }

    public Long sCard(String key) {
        return redisTemplate.opsForSet().size(key);
    }

    public Boolean sIsMemeber(String key, String value) {
        return redisTemplate.opsForSet().isMember(key, value);
    }

    public Long rPush(String key, String value) {
        return redisTemplate.opsForList().rightPush(key, value);
    }

    public String lIndex(String key, long index) {
        return redisTemplate.opsForList().index(key, index);
    }

    public String lPop(String key) {
        return redisTemplate.opsForList().leftPop(key);
    }

    public void issueRequestWithScript(long couponId, long userId, int totalIssueQuantity) {
        var couponIssueSetKey = getCouponIssueSetKey(couponId);
        var issueRequestQueueKey = RedisKey.getIssueRequestKey();
        var couponIssueRequest = new CouponIssueRequest(couponId, userId);
        try {
            // coupon 검증 후 발급에 redis 통신 횟수를 줄이기 위한 script 쿼리 ( 잠재적인 동시성 이슈도 해결된다. )
            String code = redisTemplate.execute(
                    asyncIssueScript,
                    List.of(couponIssueSetKey, issueRequestQueueKey),
                    String.valueOf(userId),
                    String.valueOf(totalIssueQuantity),
                    objectMapper.writeValueAsString(couponIssueRequest)
            ).toString();
            CouponIssueRequestCode.checkRequestResult(CouponIssueRequestCode.find(code));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("input: %s".formatted(couponIssueRequest));
        }
    }

    private RedisScript<String> issueRequestScript() {
        String script = """
                if redis.call('SISMEMBER', KEYS[1], ARGV[1]) == 1 then
                    return '2'
                end
                                
                if tonumber(ARGV[2]) > redis.call('SCARD', KEYS[1]) then
                    redis.call('SADD', KEYS[1], ARGV[1])
                    redis.call('RPUSH', KEYS[2], ARGV[3])
                    return '1'
                end
                                
                return '3'
                """;
        return RedisScript.of(script, String.class);
    }

    public List<String> popItemsWithScript(String listKey, int n) {
        // Lua 스크립트
        String luaScript = """
        local listLength = redis.call('LLEN', KEYS[1])
        local n = tonumber(ARGV[1])
        if n > listLength then
            n = listLength
        end
        
        if n == 0 then
            return {}
        end
        
        local items = redis.call('LRANGE', KEYS[1], 0, n - 1)
        redis.call('LTRIM', KEYS[1], n, -1)
        return items
        """;

        // 스크립트 실행
        RedisScript<List> script = RedisScript.of(luaScript, List.class);
        List<String> result = redisTemplate.execute(script, List.of(listKey), String.valueOf(n));
        return result;
    }
}
