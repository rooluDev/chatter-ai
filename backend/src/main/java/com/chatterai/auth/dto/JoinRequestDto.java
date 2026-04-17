package com.chatterai.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
public class JoinRequestDto {

    @NotBlank(message = "아이디를 입력해 주세요.")
    @Pattern(regexp = "^[a-zA-Z0-9]{4,20}$",
             message = "아이디는 영문자와 숫자 조합 4~20자로 입력해 주세요.")
    private String username;

    @NotBlank(message = "비밀번호를 입력해 주세요.")
    @Pattern(regexp = "^(?=.*[a-zA-Z])(?=.*\\d)(?=.*[!@#$%^&*])[a-zA-Z\\d!@#$%^&*]{8,20}$",
             message = "비밀번호는 영문자, 숫자, 특수문자(!@#$%^&*)를 포함한 8~20자로 입력해 주세요.")
    private String password;

    @NotBlank(message = "닉네임을 입력해 주세요.")
    @Size(min = 2, max = 20, message = "닉네임은 2~20자로 입력해 주세요.")
    private String nickname;
}
