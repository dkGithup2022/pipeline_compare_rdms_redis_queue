## 쿠폰 시스템 사례를 통해 확장성 설계 도출하기 

이 프로젝트는 쿠폰발급 기능을 RDMS, Aysnc, Cache 적용 유무 등을 통해 초당 최대 몇개의 발급요청을 유효하게 요청을 처리할 수 있는지 비교합니다.

<br/>

### 결론 

#### 1. 로컬 테스트 시, 레디스 큐에 요청을 쌓는 방식으로 처리하는게 10 배 이상 빨랐다. 

로컬테스트 수행시  

    - 요청 시, RDMS 에 직접 저장 : 60 QPS
    - reids 자료형을 통해서 검증하고  비동기로 저장 ( 2000 QPS ), batch consumer 로 rdms 저장 ( 600 QPS )

 로 10 배 가까이 차이납니다.

알림, 쿠폰, 피드등의 기능은 비동기 큐를 미리 두어서 확장시 시스템을 걷어내지 않게 캐어를 할 수 있습니다. 
게다가 redis 는 rdms 보다 추가하기 쉽습니다. 



</br>

#### 2. redis 분산락이 항상 select for update 보다 좋은 성능을 보장하는 것은 아니다.

로컬에서 돌려보니 성능은 비슷합니다.

성능보다는 역할적인 측면이 두드러지는 것 같은데 정리하자면 

###### redis 분산락 
redis string 을 통해 요청의 "순서만" 보장한다.

###### select..for update 
db 에서 해당 로우에 대한 접근을 막는다.


select..for update 는 쿼리가 많아지면 문제 발생했을 때, 추적하기 어려워질 것 같습니다.

</br>


### 목적

1. async keyword를 통한 발급 가능 갯수 유효성 검사 후 쿠폰 발급의 처리량 
2. 분산락을 통한 쿠폰발급 유효성 검증 후 발급의 처리량
3. 비동기 요청으로 redis 에 요청을 쌓은 뒤, 배치 컨슈머로 처리할 때의 처리량 


1,2,3 을 비교하여 기능확장과 트래픽 증가에 대한 대비를 할 수 있는 구조에 대해 알아봅니다.

<br/>

사실, 모두가 (3)이 가장 좋은 방법인것은 알지만 , "그래서 얼마나 차이가 나길래 " 라는 의문을 해결하기 위해 각 단계의 locust 테스트 결과를 비교합니다. 


<br/>

### 쿠폰 발급 기능 내용 

1. 유저가 요청을 보낸다.
2. 쿠폰에 해당한 총 발급량을 확인 후 발급가능하면 쿠폰 발급 요청을 수행한다. 
3. 쿠폰 발급 후 이행까지 ( RDMS 에 CouponIssue 생성 ) 처리 순서가 유지되어야 한다. 



### 분산락 을 통한 쿠폰 유효성 검증 후 rdms 에 이력 생성 : 100  rows per sec

![](/Users/gimdohyeon/code2024/coupon2/locust/coupon_issue/withRedisLock/60RPS.png)

60 RPS -> 버벅 거릴때 테스트 돌려서 60 나오는데, 보통 100 ~ 120 정도... 사진 다시찍기 귀찬아서 60짜리 올림.

#### 작업 요청 


```java
   @PostMapping("/withRedisLock")
    public ResponseEntity issueWithRedisLock(@RequestBody CouponIssueRequestDto couponIssueRequestDto) {

        lockManager.proceed(LOCK_PREFIX_ISSUE_COUPON + couponIssueRequestDto.couponId(), 300000L,
                () -> couponIssueRequestService.issueRequestNeedRedis(couponIssueRequestDto)
        );

        return ResponseEntity.ok().build();
    }
```
```java
@Component
@RequiredArgsConstructor
@Slf4j
public class RedisDistributeLockManager {

    private final RedissonClient redissonClient;

    public void proceed(String key, Long millisInWait, Runnable runnable){

        RLock lock = redissonClient.getLock(key);
        try{
            var available = lock.tryLock(millisInWait, millisInWait, TimeUnit.MILLISECONDS);
            if(available){
                runnable.run();
            }
            else{
                log.error("CANNOT GET DISTRIBUTED LOCK KEY : {}" , key);
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            if(lock.isLocked() && lock.isHeldByCurrentThread()){
                lock.unlock();
            }
        }
    }
}
```


```java

  @Transactional
    public  void issue(long couponId, long userId) {
        var coupon = findCoupon(couponId);
        coupon.issue();
        saveCouponIssue(couponId, userId);
    }

```



### 비동기 요청으로 redis 에 요청을 쌓은 뒤, 배치 컨슈머로 처리할 때의 처리량  

#### 검증 후 비동기 요청으로 큐에 요청 쌓기 : 2000 rows per sec

![](/Users/gimdohyeon/code2024/coupon2/locust/coupon_issue/asyncWithScript/2000RPS.png)


#### 주요 함수 


##### 엔드포인트 호출함수 : 메타데이터 조회 시, 캐싱된 정보 조회 
```java
 public void issueRequestWithScript(long couponId, long userId) {
        var cachedCoupon = couponCacheService.getCouponCache(couponId); // 쿠폰 메타 데이터에 대한 로컬 & redis cache
        redisRepository.issueRequestWithScript(couponId,userId, cachedCoupon.getTotalQuantity());
    }
```




##### REDIS LIST 에 검증 , 후 lpush 를 lua scipt 로 최적화
```java
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
```

##### lua script 실행을 위한 서비스레이어  함수 
```java
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
```


#### 큐에 있는 요청 배치 처리 - coupon-consumer

![](/Users/gimdohyeon/code2024/coupon2/locust/coupon_issue/asyncWithScript/batch_consume_560Rows_per_sec.png)

배치 업데이트 초당 500 개 



##### batch hibernate 설정 
```yaml
  jpa:
    database: mysql
    database-platform: org.hibernate.dialect.MySQL8Dialect
    hibernate:
      ddl-auto: update
    show-sql: true
    properties:
      hibernate:
        format_sql: true
        generate_statistics: true
        order_inserts: true
        jdbc:
          batch_size: 1000

```

##### 컨슈머 
```java
 @Scheduled(fixedDelay = 500)
    public void issue() throws JsonProcessingException {
        log.info("listen .... ");
        CouponIssueRequest target = getIssueTarget();
        List<CouponIssue> toBePersisted = redisRepository.popItemsWithScript(queueName, 500)
                .stream().map(e -> {
                    try {
                        var issueReq = objectMapper.readValue(e, CouponIssueRequest.class);
                        return CouponIssue.builder().couponId(issueReq.couponId()).userId(issueReq.userId()).dateIssued(LocalDateTime.now()).build();
                    } catch (JsonProcessingException ex) {
                        throw new RuntimeException(ex);
                    }
                }).toList();

        fetched += couponIssueJpaRepository.saveAll(toBePersisted).size();
        log.info("발급 완료 target " + target);
        log.info("FETCHED {} ROWS ..... TOTAL TIME TOOK : {}, ", fetched, System.currentTimeMillis() - begin);
        removeIssueTarget();
    }
```

##### 당겨오는 요청도 lua script로 성능 최적화
```java
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

```