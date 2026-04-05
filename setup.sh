#!/bin/bash

# Exit immediately if a command exits with a non-zero status
set -e

echo "🚀 Bootstrapping InnerView Platform Architecture..."

# 1. Create Root Directory
mkdir -p innerview-platform
cd innerview-platform

# 2. GitHub Actions
echo "📂 Creating .github workflows..."
mkdir -p .github/workflows
touch .github/workflows/{api-gateway.yml,spring-services.yml,go-services.yml,nodejs-services.yml,frontend.yml}

# 3. Documentation
echo "📂 Creating docs..."
mkdir -p docs/{architecture,api,deployment}
touch docs/architecture/{system-design.md,data-flow.md,communication-protocols.md}
touch docs/api/{rest-api-specs.md,websocket-protocols.md}
touch docs/deployment/{infrastructure.md,environments.md}

# 4. Spring Boot Services
echo "📂 Creating Spring Boot Microservices..."
mkdir -p services/spring-boot
touch services/spring-boot/pom.xml

# API Gateway
mkdir -p services/spring-boot/api-gateway/src/main/java/com/innerview/gateway/{config,filter,exception}
mkdir -p services/spring-boot/api-gateway/src/main/resources
touch services/spring-boot/api-gateway/src/main/java/com/innerview/gateway/GatewayApplication.java
touch services/spring-boot/api-gateway/src/main/resources/{application.yml,logback-spring.xml}
touch services/spring-boot/api-gateway/{pom.xml,Dockerfile,README.md}

# User Service
mkdir -p services/spring-boot/user-service/src/main/java/com/innerview/user/{api,application,domain,infrastructure}
mkdir -p services/spring-boot/user-service/src/main/resources
touch services/spring-boot/user-service/src/main/java/com/innerview/user/UserServiceApplication.java
touch services/spring-boot/user-service/src/main/resources/{application.yml,application-prod.yml}
touch services/spring-boot/user-service/{pom.xml,Dockerfile,README.md}

# Matching Service
mkdir -p services/spring-boot/matching-service/src/main/java/com/innerview/matching/{api,application,domain,infrastructure}
mkdir -p services/spring-boot/matching-service/src/main/resources
touch services/spring-boot/matching-service/src/main/java/com/innerview/matching/MatchingServiceApplication.java
touch services/spring-boot/matching-service/src/main/resources/application.yml
touch services/spring-boot/matching-service/{pom.xml,Dockerfile,README.md}

# Problem Hosting Service
mkdir -p services/spring-boot/problem-hosting-service/src/main/java/com/innerview/problem/{api,application,domain,infrastructure}
mkdir -p services/spring-boot/problem-hosting-service/src/main/resources
touch services/spring-boot/problem-hosting-service/src/main/java/com/innerview/problem/ProblemHostingApplication.java
touch services/spring-boot/problem-hosting-service/src/main/resources/application.yml
touch services/spring-boot/problem-hosting-service/{pom.xml,Dockerfile,README.md}

# Feedback Service
mkdir -p services/spring-boot/feedback-service/src/main/java/com/innerview/feedback/{api,application,domain,infrastructure}
mkdir -p services/spring-boot/feedback-service/src/main/resources
touch services/spring-boot/feedback-service/src/main/java/com/innerview/feedback/FeedbackServiceApplication.java
touch services/spring-boot/feedback-service/src/main/resources/application.yml
touch services/spring-boot/feedback-service/{pom.xml,Dockerfile,README.md}

# Notification Service
mkdir -p services/spring-boot/notification-service/src/main/java/com/innerview/notification/{api,application,domain,infrastructure}
mkdir -p services/spring-boot/notification-service/src/main/resources
touch services/spring-boot/notification-service/src/main/java/com/innerview/notification/NotificationServiceApplication.java
touch services/spring-boot/notification-service/src/main/resources/application.yml
touch services/spring-boot/notification-service/{pom.xml,Dockerfile,README.md}

# Shared Java Library
mkdir -p services/spring-boot/shared/src/main/java/com/innerview/shared/{dto,exception,security,util}
touch services/spring-boot/shared/pom.xml

# 5. Go Services
echo "📂 Creating Go Services..."
mkdir -p services/go/session-service/{cmd/server,internal/{handler,service,repository,model},pkg}
touch services/go/session-service/cmd/server/main.go
touch services/go/session-service/{go.mod,go.sum,Dockerfile,README.md}

mkdir -p services/go/video-service/{cmd,internal,pkg}
touch services/go/video-service/{go.mod,Dockerfile,README.md}

mkdir -p services/go/shared/{auth,database,logger}

# 6. Node.js Services
echo "📂 Creating Node.js Services..."
mkdir -p services/nodejs/collaboration-canvas/src/{websocket,redis,models}
touch services/nodejs/collaboration-canvas/src/index.ts
touch services/nodejs/collaboration-canvas/{package.json,tsconfig.json,Dockerfile,README.md}

mkdir -p services/nodejs/collaboration-editor/src
touch services/nodejs/collaboration-editor/{package.json,tsconfig.json,Dockerfile,README.md}

mkdir -p services/nodejs/shared/{websocket-utils,redis-client}

# 7. Python Services
echo "📂 Creating Python Services..."
mkdir -p services/python/ai-cv-analyzer/src/{llm,parsers,models}
touch services/python/ai-cv-analyzer/src/main.py
touch services/python/ai-cv-analyzer/{requirements.txt,Dockerfile,README.md}

mkdir -p services/python/interview-copilot/src/{diff_analyzer,redis_subscriber,models}
touch services/python/interview-copilot/src/main.py
touch services/python/interview-copilot/{requirements.txt,Dockerfile,README.md}

# 8. Web App (Frontend)
echo "📂 Creating Frontend Application..."
mkdir -p apps/web/src/{app,components,features/{auth,interview,matching,profile,feedback},hooks,lib,services/{api,websocket,webrtc},types}
mkdir -p apps/web/public
touch apps/web/{package.json,next.config.js,tsconfig.json,Dockerfile,README.md}

# 9. Infrastructure
echo "📂 Creating Infrastructure definitions..."
mkdir -p infrastructure/docker/nginx
touch infrastructure/docker/{docker-compose.yml,docker-compose.prod.yml}
touch infrastructure/docker/nginx/nginx.conf

mkdir -p infrastructure/kubernetes/{base/{api-gateway,services,ingress},overlays/{dev,staging,production}}
touch infrastructure/kubernetes/kustomization.yaml

mkdir -p infrastructure/terraform/{modules/{database,redis,vpc,eks},environments/{dev,staging,prod}}
touch infrastructure/terraform/main.tf

mkdir -p infrastructure/scripts
touch infrastructure/scripts/{setup-dev.sh,deploy.sh,db-migrations.sh}

# 10. Global Shared Resources
echo "📂 Creating Shared Resources..."
mkdir -p shared/{proto,schemas,database/{migrations,seeds}}
touch shared/proto/{user.proto,session.proto,feedback.proto}
touch shared/schemas/{rest-api.yaml,websocket-events.yaml}
touch shared/database/migrations/{V001__create_users.sql,V002__create_sessions.sql,V003__create_problems.sql}
touch shared/database/seeds/dev-data.sql

# 11. Tests & Root Files
echo "📂 Creating Tests and Root configs..."
mkdir -p tests/{integration,e2e,load}
mkdir -p scripts
touch scripts/{dev-setup.sh,build-all.sh,test-all.sh,deploy.sh}

touch .gitignore .env.example README.md CONTRIBUTING.md LICENSE Makefile

# Make scripts executable
chmod +x scripts/*.sh
chmod +x infrastructure/scripts/*.sh

echo "✅ Setup Complete! The innerView-platform monorepo has been generated."
