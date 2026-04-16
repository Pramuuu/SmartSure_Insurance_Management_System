package com.smartSure.adminService.feign;

import com.smartSure.adminService.dto.UserDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(name = "authservice")
public interface UserFeignClient {

    // Single user — uses internal endpoint for service-to-service calls
    @GetMapping("/user/internal/{userId}/profile")
    UserDTO getUserById(@PathVariable Long userId);

    // All users — ADMIN role header propagated by FeignInterceptor
    @GetMapping("/user/all")
    List<UserDTO> getAllUsers();

//    /**
//     * FIX: was PUT /user/admin/{userId}/deactivate — that endpoint does not exist
//     * in UserController. Only DELETE /user/delete/{userId} exists.
//     * Mapped to the existing delete endpoint so the admin panel does not 404.
//     *
//     * NOTE: If you want true soft-deactivation (flag user as inactive without deleting),
//     * add this to UserController:
//     *
//     *   @PutMapping("/admin/{userId}/deactivate")
//     *   @PreAuthorize("hasRole('ADMIN')")
//     *   public ResponseEntity<UserResponseDto> deactivateUser(@PathVariable Long userId) {
//     *       return ResponseEntity.ok(service.deactivate(userId));
//     *   }
//     *
//     * Then update this Feign mapping back to:
//     *   @PutMapping("/user/admin/{userId}/deactivate")
//     */
    @DeleteMapping("/user/delete/{userId}")
    UserDTO deactivateUser(@PathVariable Long userId);
}