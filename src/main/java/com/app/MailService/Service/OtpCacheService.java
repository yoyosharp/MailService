package com.app.MailService.Service;

import com.app.MailService.Entity.Otp;
import com.app.MailService.Repository.OtpRepository;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public class OtpCacheService {
    private final OtpRepository otpRepository;

    public OtpCacheService(OtpRepository otpRepository) {
        this.otpRepository = otpRepository;
    }

    @CachePut(value = "otpCache", key = "#otp.getTrackingId()")
    public Otp saveOtp(Otp otp) {
        return otpRepository.save(otp);
    }

    @Cacheable(value = "otpCache", key = "#trackingId", unless = "#result == null")
    public Otp getOtpByTrackingId(String trackingId) {
        return otpRepository.findByTrackingId(trackingId);
    }

    @CachePut(value = "otpCache", key = "#otp.trackingId")
    public Otp updateOtp(Otp otp) {
        return otpRepository.save(otp);
    }
}
