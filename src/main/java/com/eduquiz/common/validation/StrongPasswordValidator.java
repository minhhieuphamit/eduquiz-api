package com.eduquiz.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class StrongPasswordValidator implements ConstraintValidator<StrongPassword, String> {

    @Override
    public boolean isValid(String password, ConstraintValidatorContext context) {
        if (password == null || password.isBlank()) {
            return true; // @NotBlank handles this
        }

        context.disableDefaultConstraintViolation();

        if (password.length() < 8) {
            context.buildConstraintViolationWithTemplate("Mật khẩu phải có ít nhất 8 ký tự")
                    .addConstraintViolation();
            return false;
        }

        if (password.length() > 50) {
            context.buildConstraintViolationWithTemplate("Mật khẩu không được vượt quá 50 ký tự")
                    .addConstraintViolation();
            return false;
        }

        if (!password.matches(".*[a-z].*")) {
            context.buildConstraintViolationWithTemplate("Mật khẩu phải chứa ít nhất 1 chữ thường")
                    .addConstraintViolation();
            return false;
        }

        if (!password.matches(".*[A-Z].*")) {
            context.buildConstraintViolationWithTemplate("Mật khẩu phải chứa ít nhất 1 chữ hoa")
                    .addConstraintViolation();
            return false;
        }

        if (!password.matches(".*\\d.*")) {
            context.buildConstraintViolationWithTemplate("Mật khẩu phải chứa ít nhất 1 chữ số")
                    .addConstraintViolation();
            return false;
        }

        if (!password.matches(".*[@$!%*?&].*")) {
            context.buildConstraintViolationWithTemplate("Mật khẩu phải chứa ít nhất 1 ký tự đặc biệt (@$!%*?&)")
                    .addConstraintViolation();
            return false;
        }

        return true;
    }
}
