package com.smartSure.authService.repository;

import com.smartSure.authService.entity.OtpRecord;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OtpRepository extends CrudRepository<OtpRecord, String> {
    // CrudRepository gives findById(preAuthToken), save(), deleteById()
    // No custom methods needed — Redis handles TTL automatically
}
