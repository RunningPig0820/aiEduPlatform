# AI Edu Architect Coordinator Memory

## Project Structure

### Backend Path
- Root: `/Users/minzhang/Documents/work/ai/aiEduPlatform/ai-edu-backend`
- Domain: `ai-edu-domain`
- Application: `ai-edu-application`
- Infrastructure: `ai-edu-infrastructure`
- Interface: `ai-edu-interface`

### Frontend (SSR) Path
- Templates: `ai-edu-interface/src/main/resources/templates/`
- Static: `ai-edu-interface/src/main/resources/static/`

## Key Files

### Authentication APIs
- Controller: `ai-edu-interface/src/main/java/com/ai/edu/interface_/api/AuthApiController.java`
- Service: `ai-edu-application/src/main/java/com/ai/edu/application/service/UserAppService.java`
- DTOs: `ai-edu-application/src/main/java/com/ai/edu/application/dto/`

### Page Routes
- Controller: `ai-edu-interface/src/main/java/com/ai/edu/interface_/controller/PageController.java`
- Login Page: `templates/auth/login.html`
- Role Home Pages: `templates/pages/{student,teacher,parent}/home.html`

## API Endpoints

| Method | Path | Description |
|--------|------|-------------|
| POST | /api/auth/login | User login (username or phone) |
| POST | /api/auth/demo-login | Demo account quick login |
| POST | /api/auth/register | User registration |
| POST | /api/auth/send-code | Send verification code |
| POST | /api/auth/logout | Logout |
| GET | /api/auth/current-user | Get current user |
| GET | /login | Login page |

## Demo Accounts

| Role | Username | Password | Real Name |
|------|----------|----------|-----------|
| Student | student | 123456 | Demo Student |
| Teacher | teacher | 123456 | Demo Teacher |
| Parent | parent | 123456 | Demo Parent |

## Frontend Tech Stack

- Thymeleaf (SSR)
- Tailwind CSS v3 + daisyUI (CDN)
- Alpine.js v3 (CDN)

## DDD Layer Mapping

| Layer | Module | Example Files |
|-------|--------|---------------|
| Domain | ai-edu-domain | User.java, UserRepository.java |
| Application | ai-edu-application | UserAppService.java, LoginRequest.java |
| Infrastructure | ai-edu-infrastructure | UserRepositoryImpl.java, UserMapper.java |
| Interface | ai-edu-interface | AuthApiController.java, login.html |