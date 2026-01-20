package com.kuberfashion.backend.service;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class InMemoryOtpService implements OtpService {
    private static class Entry {
        String otp;
        long expiresAt;
        int attempts;
        boolean locked;
    }

    private final Map<String, Entry> store = new ConcurrentHashMap<>();
    private final Random random = new Random();
    private static final long TTL_SECONDS = 300;
    private static final int MAX_ATTEMPTS = 5;

    @Override
    public boolean sendOtp(String phone) {
        String code = String.format("%06d", random.nextInt(1_000_000));
        Entry e = new Entry();
        e.otp = code;
        e.expiresAt = Instant.now().getEpochSecond() + TTL_SECONDS;
        e.attempts = 0;
        e.locked = false;
        store.put(phone, e);
        System.out.println("OTP for "+phone+": "+code);
        return true;
    }

    @Override
    public boolean verifyOtp(String phone, String otp) {
        Entry e = store.get(phone);
        if (e == null) return false;
        if (e.locked) return false;
        if (Instant.now().getEpochSecond() > e.expiresAt) {
            store.remove(phone);
            return false;
        }
        e.attempts++;
        if (e.attempts > MAX_ATTEMPTS) {
            e.locked = true;
            return false;
        }
        boolean ok = e.otp.equals(otp);
        if (ok) store.remove(phone);
        return ok;
    }
}
