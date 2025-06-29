package com.http200ok.finbuddy.notification.repository;

import org.springframework.stereotype.Repository;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Repository
public class EmitterRepositoryImpl implements EmitterRepository {

    // 메모리상에서 관리, event = notification
    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();
    private final Map<String, Object> eventCache = new ConcurrentHashMap<>();

    // 사용
    @Override
    public SseEmitter save(String emitterId, SseEmitter sseEmitter) { // emitter 저장
        emitters.put(emitterId, sseEmitter);
        return sseEmitter;
    }

    @Override
    public void saveEventCache(String eventCacheId, Object event) { // 이벤트를 저장
        eventCache.put(eventCacheId, event);
    } // 이벤트 저장

    // 사용
    @Override
    public Map<String, SseEmitter> findAllEmitterStartWithByMemberId(String memberId) { // 해당 회원과 관련된 모든 emitter를 찾음
        return emitters.entrySet().stream()
                .filter(entry -> entry.getKey().startsWith(memberId))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    @Override
    public Map<String, Object> findAllEventCacheStartWithByMemberId(String memberId) { // 해당 회원과 관련된 모든 이벤트를 찾음
        return eventCache.entrySet().stream()
                .filter(entry -> entry.getKey().startsWith(memberId))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    // Completion, Timeout 시 해당 emitter 삭제될 때 사용되는 메소드
    @Override
    public void deleteById(String emitterId) { // emitter를 지움
        emitters.remove(emitterId);
    } // 해당 emitter 삭제

    @Override
    public void deleteAllEmitterStartWithMemberId(String memberId) { // 해당 회원과 관련된 모든 emitter 삭제
        emitters.forEach(
                (key, emitter) -> {
                    if (key.startsWith(memberId)) {
                        emitters.remove(key);
                    }
                }
        );
    }

    @Override
    public void deleteAllEventCacheStartWithMemberId(String memberId) { // 해당 회원과 관련된 모든 이벤트 삭제
        eventCache.forEach(
                (key, emitter) -> {
                    if (key.startsWith(memberId)) {
                        eventCache.remove(key);
                    }
                }
        );
    }
}
