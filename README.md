
<br>

## ✨ 마이크로서비스 아키텍처에서 API Gateway와 인증 서비스를 통합하여 보안과 사용자 인증을 간소화하는 필터

<hr>

<img src="https://github.com/user-attachments/assets/79d29dcd-6fa6-4a0d-9902-34a88790c2dc" alt="3조 logo" width="80%">

#### 개요

> * Spring Cloud Gateway 기반으로 구축된 커스텀 인증 필터 라이브러리
> * 애플리케이션 레벨에서 구현된 커스텀 솔루션으로, 인증 로직을 모듈화하고 재사용성에 중점
> * 개발자와 운영팀 모두에게 편리하고 안정적인 인증 솔루션을 제공

<br><br>


## 😄 서비스/프로젝트 소개

<aside>

💡 마이크로서비스 아키텍처에서 API Gateway와 인증 서비스를 통합하여 **보안과 사용자 인증을 간소화할 수 있는 모듈**입니다. 
이 라이브러리는 **토큰 기반 인증, SSO 통합, 권한 관리 및 IP 화이트리스트 기능을 제공**합니다.

</aside>
<br>

## 💪🏻 서비스/프로젝트 목표

- **개발 생산성 향상**
    - 미리 구현된 기능들을 제공함으로써 개발자가 보안 및 인증 관련 작업에 소요되는 시간을 줄이고, 비즈니스 로직 개발에 집중할 수 있도록 지원합니다.

- **보안 강화**
    - API Gateway와 인증 서비스를 통합하여 안전한 인증 및 권한 부여를 통해 애플리케이션의 보안을 강화합니다.

- **효율적인 리소스 관리**
    - Redis를 이용한 세션 관리와 요청 속도 제한 기능을 통해 시스템의 성능을 최적화하고 리소스를 효율적으로 사용할 수 있게 합니다.

- **유연성 제공**
    - 다양한 인증 방법(예: JWT, OAuth 등)을 지원하여 개발자가 자신의 요구에 맞는 인증 방식을 선택할 수 있도록 유연성을 제공합니다.

- **사용자 경험 개선**
    - SSO(단일 로그인)를 지원하여 사용자가 여러 서비스에 쉽게 접근할 수 있도록 하여 사용자 경험을 개선합니다.
 
  <br>

## 🌐 인프라 설계도
<img width="1193" alt="Screenshot 2024-10-23 at 1 11 14 PM" src="https://github.com/user-attachments/assets/4575a548-e38e-424b-bfa5-07e1a0a41f93">



<br><br>

## 🔐 주요 기능

- **토큰 검증**:
    - Redis와의 연동을 통해 액세스 토큰을 검증하고 유효성을 체크합니다.

- **SSO 지원**
    - SSO 서버와 통합하여 사용자 인증을 간소화합니다.

- **IP 화이트리스트**
    - 안전한 API 접근을 위해 IP 화이트리스트 기능을 포함하고 있습니다.

- **세션 관리**
    - 사용자의 세션을 효율적으로 관리하여 불필요한 리소스 소모를 줄입니다.

- **사용자 권한 기반 자원 접근 제한**
    - 사용자 권한을 기반으로 API 접근을 제어하여 보안을 강화합니다.
  
  <br>

## 🔨 적용 기술

- MSA

- BackEnd
    - Java 17
    - SpringBoot 3.3.4
    - WebFlux
    - Spring Security

- DataBase
    - Redis
    - PostgreSQL 16.4

- Infra
    - Docker
    - Docker Compose

- 협업 툴 및 소스 관리
    - Github
    - Slack
    - Notion
<br>

## 👯 기술적 의사결정

- JWT
    - AccessToken, RefreshToken 를 발급하여 보안성 강화.
    - JWT는 자체적으로 인증 정보를 포함하고 있어 추가적인 세션 저장소가 필요하지 않으며, 다양한 서비스에서 효율적인 인증을 가능하게 함.

- Redis
    - AccessToken, RefreshToken 을 캐시하는 구조를 설계, Gateway 필터에서 Redis를 사용해 인증 토큰의 유효성을 확인하도록하여, AuthService에 대한 요청을 최소화 함.
    - 외부 서버에 대한 요청을 최소화 할 수 있고, TTL기능을 통해 스케줄링 구현 없이 토큰을 관리함.
    - JWT 토큰의 인증 정보 캐싱을 통해 성능을 최적화하고, 서비스 간 빠른 데이터 공유가 가능하도록 함.

- WebFlux
    - 인증필터가 마이크로서비스 환경에서 높은 처리량과 빠른 응답 속도를 요구하기 때문.
<br>

## 💥 트러블슈팅

- 요청 header 중첩 문제
    - **문제 발생**: API 요청을 처리하는 과정에서 AuthFilter를 통해 요청 헤더에 인증 정보를 추가했습니다. 그러나 이 필터가 `/api/**` 경로에 적용되어 있었고, 해당 경로로 요청을 보낼 때 Auth 서비스로의 요청이 또 다시 같은 필터를 통과하게 되었습니다. 이로 인해 인증 관련 헤더가 중첩되어 여러 번 추가되었습니다.
  
    - **헤더 중첩 원인**: 요청이 AuthFilter를 통과할 때마다 인증 헤더가 다시 추가되었고, 이로 인해 헤더의 길이가 점점 길어졌습니다. HTTP 헤더는 길이에 제한이 있기 때문에, 헤더가 너무 길어지면 서버가 요청을 제대로 처리할 수 없게 됩니다. 결과적으로 헤더 길이 초과로 인해 400 Bad Request 오류가 발생했습니다.

    - **해결 방안**:
        
         필터 적용 경로를 조정하여 AuthFilter가 필요하지 않은 특정 서비스에 대해선 적용되지 않도록 설정했습니다.
<br><br>
- User 서비스와 Auth 서비스 간의 DTO 구조 차이 문제
    - **문제 발생** : User서비스와 Auth서비스 간의 DTO 구조 차이로 인해 로그인 과정에서 에러 발생
    이 차이로 인해, User서비스가 Auth서비스에 로그인 요청을 보낼때 role이 포함되지 않아, **Auth 서비스에서 "username과 role이 필요하다"**는 에러 메시지가 발생하였습니다.

    - **원인** : User서비스에서 로그인시 username, password만 전달하도록 설정,  Auth서비스에서는 username,role,password를 필요로 하는 Dto를 받도록 설정 되어있었습니다.  따라서 
    User 서비스에서 `username`, `password`만으로 로그인 요청을 했을 때, Auth 서비스는 `role` 값이 없어 로그인 요청을 처리 할 수 없었습니다.  이로인해 FeignClient를통해 Auth서비스로 로그인 요청을 보낼때, DTO구조 차이로 인해 Bad Request(400)   에러가 발생하거나, role정보가 누락되어 잘못된 요청으로 처리가 되었습니다.

    - **해결방안** : User 서비스에서 로그인 시 받은 `username`, `password` 정보를 사용하여 DB에서 해당 사용자의 role을 조회하여 username과 role를 Auth서비스로 전달하여 로그인 요청을 처리하였습니다. 클라이언트는 여전히 `username`, `password` 만 전달하도록 하여 내부적으로 필요한 정보를 Auth 서비스에 전달 하도록 처리하였습니다.

    - **결과** : User 서비스가 Auth 서비스에 role정보를 함께 전달하게 되어, Auth 서비스에서 더이상 DTO구조 차이로 인한 에러가 발생하지 않았습니다. 로그인 과정에서 필요한 모든 정보 (`username`, `role`) 가 올바르게 Auth서비스로 전달되어, 토큰 발급이 정상적으로 이루어지게 되었습니다. 두 서비스간의 DTO 구조 일관성을 유지하면서, 클라이언트 요청 방식도 간단하게 유지할 수 있었습니다.
<br><br>

## 🍎 CONTRIBUTORS
| 팀원명 | 포지션 | 담당(개인별 기여점) | 깃허브 링크 |
| --- | --- | --- | --- |
| 유희진 | 팀원 |  OAuth 2.0 인증 시스템 구현 | [https://github.com/heejin1023](https://github.com/heejin1023) | 
| 한수빈 | 팀원 |  사용자 권한 접근 제한<br>- API 경로 접근 권한 정의 및 Redis 저장<br>- Gateway에서 Redis를 통한 권한 처리<br><br>데모 사용자 서버 개발<br>- 회원가입, 로그인 구현<br>- 로그아웃 정보 AUTH 서버 전달 | [https://github.com/subinny2](https://github.com/subinny2) |




## 📝 Technologies & Tools (BE)📝

<img src="https://img.shields.io/badge/java-007396?style=for-the-badge&logo=java&logoColor=white"> <img src="https://img.shields.io/badge/SpringBoot-6DB33F?style=for-the-badge&logo=springboot&logoColor=white"/> <img src="https://img.shields.io/badge/SpringSecurity-6DB33F?style=for-the-badge&logo=SpringSecurity&logoColor=white"/> <br>
<img src="https://img.shields.io/badge/MSA-239120?style=for-the-badge&logo=Microservices&logoColor=white"/> <img src="https://img.shields.io/badge/PostgreSQL-4169E1?style=for-the-badge&logo=PostgreSQL&logoColor=white"/> <img src="https://img.shields.io/badge/Gradle-02303A?style=for-the-badge&logo=Gradle&logoColor=white"/>

<img src="https://img.shields.io/badge/github-181717?style=for-the-badge&logo=github&logoColor=white"/> <img src="https://img.shields.io/badge/git-F05032?style=for-the-badge&logo=git&logoColor=white"> <br/> <img src="https://img.shields.io/badge/Postman-FF6C37?style=for-the-badge&logo=Postman&logoColor=white"/>  <img src="https://img.shields.io/badge/Notion-000000?style=for-the-badge&logo=Notion&logoColor=white"/> <img src="https://img.shields.io/badge/Slack-4A154B?style=for-the-badge&logo=slack&logoColor=white"/> 

<br><br><br><br>

