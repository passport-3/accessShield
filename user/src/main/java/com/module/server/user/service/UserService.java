package com.module.server.user.service;

import com.module.server.user.client.AuthServiceClient;
import com.module.server.user.dto.RegisterRequestDto;
import com.module.server.user.dto.TokenResponseDto;
import com.module.server.user.dto.UserInfoDto;
import com.module.server.user.model.User;
import com.module.server.user.model.UserRoleEnum;
import com.module.server.user.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final AuthServiceClient authServiceClient;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(AuthServiceClient authServiceClient, UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.authServiceClient = authServiceClient;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // 로그인
    public String login(String username, String password) {
        // 1. 사용자 정보 조회
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("해당 회원은 존재하지 않습니다."));

        // 2. 사용자 ID와 역할을 UserInfoDto로 묶어서 Auth 서비스로 전달
        UserInfoDto UserInfoDto = new UserInfoDto(user.getUserId().toString(), user.getRole().name());

        // 3. Auth 서비스에 객체를 넘겨 로그인을 요청한다. (토큰 발급을 요청한다.)
        ResponseEntity<TokenResponseDto> response = authServiceClient.login(UserInfoDto);

        // 4. 응답 성공 여부 확인 (accessToken 반환)
        if (response.getStatusCode().is2xxSuccessful()) {
            return response.getBody().getAccessToken();  // accessToken만 반환
        } else {
            throw new IllegalStateException("로그인 실패");
        }
    }

    // 회원가입
    public void register(RegisterRequestDto registerReqeustDto) {
        // 1. 사용자 정보 중복 체크
        if (userRepository.findByUsername(registerReqeustDto.getUsername()).isPresent()) {
            throw new IllegalArgumentException("이미 사용 중인 아이디입니다.");
        }

        if(userRepository.findByEmail(registerReqeustDto.getEmail()).isPresent()) {
            throw new IllegalArgumentException("이미사용중인 이메일입니다.");
        }

        // 2. 비밀번호 해싱 처리
        String encodedPassword = passwordEncoder.encode(registerReqeustDto.getPassword());

        // 3. User 엔티티 생성 (role은 기본적으로 USER로 설정)
        User user = User.create(
                registerReqeustDto.getUsername(),
                encodedPassword,  // 해싱된 비밀번호 사용
                registerReqeustDto.getEmail(),
                registerReqeustDto.getPhone(),
                UserRoleEnum.USER  // 기본 역할 설정
        );

        // 4. User 저장
        userRepository.save(user);


    }
}
