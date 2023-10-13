package com.defen.yunyun.task;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.defen.yunyun.constant.RedisConstant;
import com.defen.yunyun.model.entity.User;
import com.defen.yunyun.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RSet;
import org.redisson.api.RedissonClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.defen.yunyun.constant.SystemConstant.MAX_PAGE_SIZE;


/**
 * 预缓存任务类
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PreCacheTask {

    @Resource
    private UserService userService;
    @Resource
    private RedissonClient redissonClient;

    /**
     * 启动时处理缓存过期的情况
     */
    @PostConstruct
    public void cacheNotCachedData() {
        // 若标签缓存过期则自动重建
        if (redissonClient.getKeys()
                          .getKeysStreamByPattern(RedisConstant.TAGS_KEY + "*")
                          .count() == 0)
            this.doCacheTagsTask();
    }

    /**
     * 标签的定时缓存任务
     */
    @Scheduled(cron = "0 0 0 * * ?")
    public void doCacheTagsTask() {
        // 获取分布式锁实例
        RLock lock = redissonClient.getLock(RedisConstant.LOCK_CACHE_TAGS_KEY);
        try {
            // 未获取到锁立即放弃, 并使用看门狗机制将锁自动续期
            if (lock.tryLock(-1, -1, TimeUnit.SECONDS)) {
                log.info("上锁成功, 正在预缓存标签数据...");

                // 根据可用的处理器核心数创建线程池
                ExecutorService executorService = Executors.newFixedThreadPool(
                        Runtime.getRuntime().availableProcessors());

                // 计算分页参数
                long totalUsers = userService.count();
                int pageSize = MAX_PAGE_SIZE;
                int totalPages = (int) ((totalUsers + pageSize - 1) / pageSize);

                // 创建计数器
                CountDownLatch latch = new CountDownLatch(totalPages);

                for (int currentPage = 1; currentPage <= totalPages; currentPage++) {
                    int finalPage = currentPage;
                    // 提交子任务到线程池
                    executorService.submit(() -> {
                        try {
                            List<User> users = userService.page(new Page<>(finalPage, pageSize))
                                                          .getRecords();
                            // 保存当前页出现的标签及对应的用户
                            Map<String, Set<Long>> tagToUserIds = new HashMap<>();
                            users.forEach(user -> {
                                List<String> userTags = user.getTags();
                                if (userTags != null) {
                                    userTags.forEach(tag ->
                                            tagToUserIds.computeIfAbsent(tag, k -> new HashSet<>())
                                                    .add(user.getId())
                                    );
                                }
                            });
                            // 将标签及对应的用户缓存到 Redis
                            tagToUserIds.forEach((tag, userIds) -> {
                                RSet<Long> userSet = redissonClient.getSet(RedisConstant.TAGS_KEY + tag);
                                userSet.addAll(userIds);
                                userSet.expire(RedisConstant.TAGS_TTL);
                            });
                        } finally {
                            // 子任务完成, 减少计数
                            latch.countDown();
                        }
                    });
                }
                // 等待所有子任务完成
                latch.await();
                // 关闭线程池
                executorService.shutdown();
            } else {
                log.info("上锁失败, 标签数据正在其他服务器预缓存中");
            }
        } catch (InterruptedException e) {
            log.error("标签数据预缓存失败", e);
        } finally {
            // 释放锁
            if (lock.isHeldByCurrentThread())
                lock.unlock();
        }
    }
}
