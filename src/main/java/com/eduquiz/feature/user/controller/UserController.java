package com.eduquiz.feature.user.controller;

/**
 * @RestController @RequestMapping("/api/v1/users")
 * @PreAuthorize("hasRole('ADMIN')") GET  /         → getAllUsers (paginated)
 * GET  /{id}     → getUserById
 * PUT  /{id}/role → updateUserRole
 * PUT  /{id}/deactivate → deactivateUser
 * TODO: Implement
 */
public class UserController {
}
