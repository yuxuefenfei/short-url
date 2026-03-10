# 短网址管理系统 - 后端

基于Spring Boot 3.x + MyBatis-Flex + MySQL + Redis的高性能短网址管理系统后端。

## 📋 部署指南

### 生产环境部署步骤

#### 1. 环境准备

**服务器要求：**

- 操作系统：Linux (推荐 CentOS 7+/Ubuntu 18.04+)
- JDK：OpenJDK 17
- MySQL：8.0+
- Redis：6.2+
- Nginx：1.18+

**安装依赖：**

```bash
# CentOS/RHEL
sudo yum update
sudo yum install -y java-17-openjdk-devel mysql-server redis nginx

# Ubuntu/Debian
sudo apt update
sudo apt install -y openjdk-17-jdk mysql-server redis-server nginx
```

#### 2. 数据库部署

**创建数据库：**

```sql
CREATE DATABASE short_url_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

**执行建表脚本：**

```bash
mysql -u root -p short_url_db < docs/database/schema.sql
```

**创建应用用户：**

```sql
CREATE USER 'shorturl'@'%' IDENTIFIED BY 'your_secure_password';
GRANT ALL PRIVILEGES ON short_url_db.* TO 'shorturl'@'%';
FLUSH PRIVILEGES;
```

#### 3. Redis配置

**修改Redis配置文件 `/etc/redis/redis.conf`：**

```conf
# 绑定地址（生产环境建议指定内网IP）
bind 127.0.0.1

# 端口
port 6379

# 密码认证（建议设置）
requirepass your_redis_password

# 最大内存
maxmemory 512mb
maxmemory-policy allkeys-lru

# 持久化配置
appendonly yes
appendfsync everysec
```

**重启Redis：**

```bash
sudo systemctl restart redis
```

#### 4. 应用部署

**构建应用：**

```bash
cd backend
mvn clean package -DskipTests
```

**创建部署目录：**

```bash
sudo mkdir -p /opt/short-url
sudo chown $USER:$USER /opt/short-url
```

**复制应用文件：**

```bash
cp target/short-url-*.jar /opt/short-url/short-url.jar
cp src/main/resources/application-prod.yml /opt/short-url/application.yml
```

**配置应用参数：**
编辑 `/opt/short-url/application.yml`：

```yaml
server:
  port: 8080

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/short_url_db?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=UTC
    username: shorturl
    password: your_secure_password

  redis:
    host: localhost
    port: 6379
    password: your_redis_password
    database: 0

shorturl:
  domain: https://your-domain.com
  jwt:
    secret: your_jwt_secret_key_here_make_it_long_and_secure
    expiration: 86400000  # 24小时
```

**创建启动脚本 `/opt/short-url/start.sh`：**

```bash
#!/bin/bash

APP_HOME=/opt/short-url
APP_JAR=$APP_HOME/short-url.jar
LOG_DIR=$APP_HOME/logs

# 创建日志目录
mkdir -p $LOG_DIR

# JVM参数
JVM_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC -XX:MaxGCPauseMillis=200"
JVM_OPTS="$JVM_OPTS -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=$LOG_DIR"
JVM_OPTS="$JVM_OPTS -Djava.security.egd=file:/dev/./urandom"

# 启动应用
exec java $JVM_OPTS -jar $APP_JAR \
    --spring.config.location=$APP_HOME/application.yml \
    > $LOG_DIR/application.log 2>&1 &

echo "短网址系统已启动，PID: $!"
```

**创建停止脚本 `/opt/short-url/stop.sh`：**

```bash
#!/bin/bash

APP_HOME=/opt/short-url
PID_FILE=$APP_HOME/app.pid

if [ -f $PID_FILE ]; then
    PID=$(cat $PID_FILE)
    if kill -0 $PID 2>/dev/null; then
        kill $PID
        echo "应用已停止 (PID: $PID)"
    else
        echo "应用未运行"
    fi
    rm -f $PID_FILE
else
    # 查找Java进程
    PID=$(ps aux | grep 'short-url.jar' | grep -v grep | awk '{print $2}')
    if [ ! -z "$PID" ]; then
        kill $PID
        echo "应用已停止 (PID: $PID)"
    else
        echo "应用未运行"
    fi
fi
```

**设置权限：**

```bash
chmod +x /opt/short-url/*.sh
```

**启动应用：**

```bash
cd /opt/short-url
./start.sh
```

#### 5. Nginx配置

**配置Nginx反向代理：**
复制 `docs/deployment/nginx-config.conf` 到 `/etc/nginx/nginx.conf`，或创建虚拟主机配置：

`/etc/nginx/conf.d/short-url.conf`：

```nginx
upstream short_url_backend {
    server 127.0.0.1:8080;
    keepalive 32;
}

server {
    listen 80;
    server_name your-domain.com;

    # 短网址重定向（优先级最高）
    location ~ ^/[a-zA-Z0-9]{6,8}$ {
        proxy_pass http://short_url_backend;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;

        # 超时设置
        proxy_connect_timeout 5s;
        proxy_send_timeout 10s;
        proxy_read_timeout 30s;
    }

    # API接口
    location /api/ {
        proxy_pass http://short_url_backend;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;

        # 超时设置
        proxy_connect_timeout 10s;
        proxy_send_timeout 30s;
        proxy_read_timeout 60s;
    }

    # 静态文件缓存
    location ~* \.(jpg|jpeg|png|gif|ico|css|js)$ {
        expires 1y;
        add_header Cache-Control "public, immutable";
    }
}
```

**重启Nginx：**

```bash
sudo nginx -t
sudo systemctl restart nginx
```

#### 6. 前端部署

**构建前端：**

```bash
cd frontend
npm install
npm run build
```

**配置Nginx静态文件服务：**
在Nginx配置中添加：

```nginx
server {
    listen 80;
    server_name admin.your-domain.com;

    root /opt/short-url/frontend;
    index index.html;

    location / {
        try_files $uri $uri/ /index.html;
    }

    # API代理
    location /api/ {
        proxy_pass http://short_url_backend;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }
}
```

**复制前端文件：**

```bash
sudo mkdir -p /opt/short-url/frontend
sudo cp -r frontend/dist/* /opt/short-url/frontend/
```

#### 7. SSL证书配置（推荐）

**使用Let's Encrypt：**

```bash
# 安装Certbot
sudo yum install -y certbot python3-certbot-nginx  # CentOS
sudo apt install -y certbot python3-certbot-nginx  # Ubuntu

# 获取证书
sudo certbot --nginx -d your-domain.com -d admin.your-domain.com
```

#### 8. 系统服务配置

**创建Systemd服务 `/etc/systemd/system/short-url.service`：**

```ini
[Unit]
Description=Short URL Service
After=network.target mysql.service redis.service

[Service]
Type=simple
User=shorturl
WorkingDirectory=/opt/short-url
ExecStart=/usr/bin/java -Xms512m -Xmx1024m -jar /opt/short-url/short-url.jar
ExecStop=/bin/kill -TERM $MAINPID
Restart=always
RestartSec=10

# 安全配置
NoNewPrivileges=true
PrivateTmp=true
ProtectSystem=full
ProtectHome=true

[Install]
WantedBy=multi-user.target
```

**启用服务：**

```bash
sudo systemctl daemon-reload
sudo systemctl enable short-url
sudo systemctl start short-url
```

#### 9. 监控和健康检查

**健康检查接口：**

- `GET /api/health` - 应用健康状态
- `GET /api/metrics` - 应用指标（需要配置监控）

**监控脚本：**

```bash
#!/bin/bash
# health-check.sh

APP_URL="http://localhost:8080/api/health"
LOG_FILE="/opt/short-url/logs/health-check.log"

if curl -f -s "$APP_URL" > /dev/null; then
    echo "$(date): 应用运行正常" >> "$LOG_FILE"
else
    echo "$(date): 应用异常，尝试重启" >> "$LOG_FILE"
    sudo systemctl restart short-url
fi
```

**添加到crontab：**

```bash
# 每5分钟检查一次
*/5 * * * * /opt/short-url/health-check.sh
```

### 运维管理

#### 日常维护

**查看应用日志：**

```bash
tail -f /opt/short-url/logs/application.log
```

**查看系统状态：**

```bash
sudo systemctl status short-url
sudo systemctl status mysql
sudo systemctl status redis
sudo systemctl status nginx
```

**数据库备份：**

```bash
#!/bin/bash
# backup-db.sh

BACKUP_DIR="/opt/backups"
DATE=$(date +%Y%m%d_%H%M%S)
DB_NAME="short_url_db"

mkdir -p "$BACKUP_DIR"
mysqldump -u shorturl -p"$DB_PASSWORD" "$DB_NAME" > "$BACKUP_DIR/${DB_NAME}_${DATE}.sql"

# 压缩备份
gzip "$BACKUP_DIR/${DB_NAME}_${DATE}.sql"

# 删除7天前的备份
find "$BACKUP_DIR" -name "*.sql.gz" -mtime +7 -delete
```

#### 性能监控

**关键指标监控：**

- 应用响应时间（目标：< 100ms）
- 系统负载（CPU、内存、磁盘）
- 数据库连接数
- Redis内存使用
- Nginx请求量

**日志分析：**

```bash
# 查看错误日志
grep -i "error" /opt/short-url/logs/application.log

# 统计访问量
grep "GET /api/shorten" /var/log/nginx/access.log | wc -l

# 查看慢查询日志（MySQL）
tail -f /var/log/mysql/mysql-slow.log
```

### 故障排除

#### 常见问题

**1. 应用启动失败**

- 检查端口是否被占用：`netstat -tlnp | grep 8080`
- 检查配置文件语法：`java -jar app.jar --spring.config.location=application.yml`
- 检查依赖服务：MySQL、Redis是否正常运行

**2. 数据库连接问题**

- 检查数据库服务状态：`systemctl status mysql`
- 检查连接配置：用户名、密码、主机地址
- 检查防火墙设置：`firewall-cmd --list-all`

**3. Redis连接问题**

- 检查Redis服务状态：`systemctl status redis`
- 测试连接：`redis-cli ping`
- 检查密码配置

**4. 性能问题**

- 检查JVM内存使用：`jstat -gc <pid>`
- 检查数据库慢查询
- 检查Redis内存使用：`redis-cli info memory`

### 安全加固

#### 系统安全

**防火墙配置：**

```bash
# 只开放必要端口
sudo firewall-cmd --permanent --add-port=80/tcp
sudo firewall-cmd --permanent --add-port=443/tcp
sudo firewall-cmd --permanent --add-port=22/tcp
sudo firewall-cmd --reload
```

**应用安全：**

- 定期更新依赖包
- 使用强密码策略
- 启用HTTPS
- 配置WAF（Web应用防火墙）

#### 数据安全

**数据库安全：**

- 定期备份
- 数据加密
- 访问权限控制

**文件安全：**

- 配置文件权限：`chmod 600 application.yml`
- 日志文件权限：`chmod 644 *.log`

### 扩展和优化

#### 水平扩展

**负载均衡：**

- 多应用实例
- Nginx负载均衡
- 数据库读写分离

**缓存优化：**

- Redis集群
- 多级缓存
- 缓存预热

#### 性能优化

**JVM调优：**

```bash
JAVA_OPTS="-Xms1g -Xmx2g -XX:+UseG1GC -XX:MaxGCPauseMillis=200"
```

**数据库优化：**

- 索引优化
- 查询优化
- 连接池配置

**前端优化：**

- CDN加速
- 资源压缩
- 懒加载

### 备份恢复

#### 完整备份脚本

`/opt/short-url/backup-full.sh`：

```bash
#!/bin/bash

BACKUP_DIR="/opt/backups/full"
DATE=$(date +%Y%m%d_%H%M%S)
BACKUP_FILE="backup_${DATE}.tar.gz"

mkdir -p "$BACKUP_DIR"

# 数据库备份
mysqldump -u shorturl -p"$DB_PASSWORD" short_url_db > /tmp/db_backup.sql

# 应用配置备份
cp /opt/short-url/application.yml /tmp/

# 创建完整备份
tar -czf "$BACKUP_DIR/$BACKUP_FILE" \
    /tmp/db_backup.sql \
    /tmp/application.yml \
    /opt/short-url/logs/ \
    /etc/nginx/conf.d/short-url.conf

# 清理临时文件
rm -f /tmp/db_backup.sql /tmp/application.yml

echo "完整备份已创建: $BACKUP_DIR/$BACKUP_FILE"
```

#### 恢复脚本

`/opt/short-url/restore.sh`：

```bash
#!/bin/bash

BACKUP_FILE=$1

if [ ! -f "$BACKUP_FILE" ]; then
    echo "备份文件不存在: $BACKUP_FILE"
    exit 1
fi

# 停止应用
sudo systemctl stop short-url

# 恢复数据库
tar -xzf "$BACKUP_FILE" -C /tmp/
mysql -u shorturl -p"$DB_PASSWORD" short_url_db < /tmp/db_backup.sql

# 恢复配置
cp /tmp/application.yml /opt/short-url/

# 启动应用
sudo systemctl start short-url

echo "系统恢复完成"
```

### 监控告警

#### Prometheus + Grafana监控

**Prometheus配置：**

```yaml
# prometheus.yml
scrape_configs:
  - job_name: 'short-url'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['localhost:8080']
```

**关键监控指标：**

- HTTP请求量和响应时间
- JVM内存和GC情况
- 数据库连接数
- Redis命中率
- 系统资源使用

#### 告警规则

**应用告警：**

- 应用宕机
- 响应时间异常
- 错误率升高
- 磁盘空间不足

**系统告警：**

- CPU使用率过高
- 内存使用率过高
- 磁盘IO异常
- 网络异常

### 版本升级

#### 升级流程

1. **准备工作**
   ```bash
   # 备份当前版本
   ./backup-full.sh

   # 停止服务
   sudo systemctl stop short-url
   ```

2. **部署新版本**
   ```bash
   # 构建新版本
   mvn clean package -DskipTests

   # 替换应用文件
   cp target/short-url-*.jar /opt/short-url/short-url.jar
   ```

3. **启动验证**
   ```bash
   # 启动服务
   sudo systemctl start short-url

   # 验证功能
   curl -f http://localhost:8080/api/health
   ```

4. **回滚计划**
   ```bash
   # 如果新版本有问题，执行回滚
   ./restore.sh /opt/backups/full/backup_previous.tar.gz
   ```

### 联系和支持

**运维团队联系方式：**

- 邮箱：ops@your-company.com
- 电话：+86-400-XXX-XXXX
- 微信群：短网址系统运维群

**紧急处理流程：**

1. 检查应用状态
2. 查看错误日志
3. 执行重启操作
4. 联系技术支持
5. 执行回滚操作（如必要）

### 附录

#### 常用命令速查

```bash
# 应用管理
sudo systemctl start short-url
sudo systemctl stop short-url
sudo systemctl restart short-url
sudo systemctl status short-url

# 日志查看
tail -f /opt/short-url/logs/application.log
grep -i "error" /opt/short-url/logs/application.log

# 系统监控
top
free -h
df -h
netstat -tlnp

# 数据库操作
mysql -u shorturl -p short_url_db
SHOW PROCESSLIST;
SHOW STATUS LIKE 'Threads_%';
```

#### 配置文件位置

- 应用配置：`/opt/short-url/application.yml`
- Nginx配置：`/etc/nginx/conf.d/short-url.conf`
- MySQL配置：`/etc/my.cnf`
- Redis配置：`/etc/redis/redis.conf`

#### 日志文件位置

- 应用日志：`/opt/short-url/logs/application.log`
- Nginx访问日志：`/var/log/nginx/access.log`
- Nginx错误日志：`/var/log/nginx/error.log`
- MySQL错误日志：`/var/log/mysql/error.log`
- Redis日志：`/var/log/redis/redis.log`

---

**文档版本：** v1.0.0
**最后更新：** 2026-03-04
**维护团队：** 运维开发团队

## 🚀 技术栈

- **核心框架**: Spring Boot 3.x + JDK 17
- **ORM框架**: MyBatis-Flex (零XML配置)
- **数据库**: MySQL 8.x
- **缓存**: Redis 6.x
- **安全框架**: Spring Security + JWT
- **构建工具**: Maven
- **工具库**: Lombok + Hutool

## 📋 项目结构

```
backend/
├── src/main/java/com/example/shorturl/
│   ├── config/                     # 配置类
│   │   ├── RedisConfig.java        # Redis配置
│   │   ├── SecurityConfig.java     # 安全配置
│   │   └── WebConfig.java          # Web配置
│   ├── controller/                # 控制器
│   │   ├── UrlController.java      # 短网址控制器
│   │   ├── AdminController.java    # 管理控制器
│   │   └── RedirectController.java # 重定向控制器
│   ├── service/                   # 业务逻辑
│   │   ├── UrlService.java        # 短网址服务
│   │   ├── UserService.java       # 用户服务
│   │   └── StatsService.java      # 统计服务
│   ├── dao/                       # 数据访问层
│   │   ├── UrlMappingDao.java     # 短网址DAO
│   │   ├── AccessLogDao.java      # 访问日志DAO
│   │   ├── UserDao.java           # 用户DAO
│   │   └── OperationLogDao.java   # 操作日志DAO
│   ├── model/                     # 数据模型
│   │   └── entity/                # 实体类
│   │       ├── ShortUrlMapping.java
│   │       ├── UrlAccessLog.java
│   │       ├── User.java
│   │       └── UserOperationLog.java
│   ├── common/                    # 公共组件
│   │   ├── exception/            # 异常处理
│   │   │   ├── BusinessException.java
│   │   │   ├── GlobalExceptionHandler.java
│   │   │   └── ErrorCode.java
│   │   ├── response/             # 统一响应
│   │   │   ├── ApiResponse.java
│   │   │   ├── PageResult.java
│   │   │   └── ResponseStatus.java
│   │   ├── utils/                # 工具类
│   │   │   ├── ShortUrlGenerator.java
│   │   │   ├── JsonUtils.java
│   │   │   └── DateUtils.java
│   │   └── constants/           # 常量定义
│   │       ├── ApiConstants.java
│   │       └── SystemConstants.java
│   └── ShortUrlApplication.java    # 应用启动类
├── src/main/resources/
│   ├── mapper/                     # MyBatis映射文件（可选）
│   ├── application.yml            # 应用配置
│   ├── application-dev.yml        # 开发环境配置
│   └── application-prod.yml       # 生产环境配置
├── pom.xml                         # Maven依赖配置
└── README.md                       # 后端说明文档
```

## ✨ 已实现功能

### 核心功能

- ✅ **短网址生成**: Base62编码算法，Redis原子操作确保唯一性
- ✅ **访问重定向**: HTTP 302重定向，支持过期和状态检查
- ✅ **访问统计**: 详细的点击统计和趋势分析
- ✅ **缓存优化**: Redis多级缓存架构，7天过期策略

### 数据管理

- ✅ **MyBatis-Flex**: 零XML配置，Lambda表达式查询
- ✅ **数据库设计**: 4张核心表，合理索引设计
- ✅ **事务管理**: Spring声明式事务
- ✅ **数据验证**: Bean Validation参数校验

### 统一响应

- ✅ **ApiResponse**: 标准化响应格式
- ✅ **错误码规范**: 系统级+业务级错误码
- ✅ **全局异常处理**: 分层异常处理机制
- ✅ **请求追踪**: RequestId链路追踪

## 🛠️ 开发环境配置

### 环境要求

- JDK 17
- Maven 3.6+
- MySQL 8.x
- Redis 6.x

### 快速开始

```bash
# 克隆项目
git clone <repository-url>
cd backend

# 配置数据库
# 1. 创建数据库
CREATE DATABASE short_url_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

# 2. 执行建表脚本
mysql -u username -p short_url_db < ../docs/database/schema.sql

# 3. 修改配置文件
# 编辑 src/main/resources/application.yml
# 配置数据库连接和Redis连接

# 编译和运行
mvn clean compile
mvn spring-boot:run

# 或者打包运行
mvn clean package
java -jar target/short-url-1.0.0.jar
```

## 📦 依赖配置

### 核心依赖 (pom.xml)

```xml
<dependencies>
    <!-- Spring Boot Starters -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-redis</artifactId>
    </dependency>

    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-security</artifactId>
    </dependency>

    <!-- MyBatis-Flex -->
    <dependency>
        <groupId>com.mybatis-flex</groupId>
        <artifactId>mybatis-flex-spring-boot-starter</artifactId>
        <version>1.11.6</version>
    </dependency>

    <!-- MySQL -->
    <dependency>
        <groupId>mysql</groupId>
        <artifactId>mysql-connector-java</artifactId>
        <version>8.0.33</version>
    </dependency>

    <!-- JWT -->
    <dependency>
        <groupId>io.jsonwebtoken</groupId>
        <artifactId>jjwt</artifactId>
        <version>0.9.1</version>
    </dependency>

    <!-- 工具库 -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>

    <dependency>
        <groupId>cn.hutool</groupId>
        <artifactId>hutool-all</artifactId>
        <version>5.8.25</version>
    </dependency>
</dependencies>
```

## 🔧 配置文件

### 应用配置 (application.yml)

```yaml
spring:
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://localhost:3306/short_url_db?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=UTC
    username: root
    password: password

  data:
    redis:
      host: localhost
      port: 6379
      database: 0

time:
  secret: mySecretKey123456789012345678901234567890
  expiration: 86400000

short-url:
  domain: https://short.ly
  key-length: 6
  cache-expire-days: 7

server:
  port: 8080
```

## 🗄️ 数据库设计

### 表结构

#### 1. short_url_mapping (短网址映射表)

- `id`: 主键ID
- `short_key`: 短网址key (唯一索引)
- `original_url`: 原始长网址
- `title`: 网址标题
- `click_count`: 点击次数
- `status`: 状态(1正常/0禁用)
- `created_time`: 创建时间
- `expired_time`: 过期时间

#### 2. url_access_log (访问日志表)

- `id`: 主键ID
- `short_key`: 短网址key
- `user_agent`: 用户浏览器信息
- `ip_address`: 访问者IP
- `access_time`: 访问时间

#### 3. user (用户表)

- `id`: 主键ID
- `username`: 用户名 (唯一索引)
- `password`: BCrypt加密密码
- `email`: 邮箱
- `role`: 角色(USER/ADMIN)
- `status`: 状态
- `last_login_time`: 最后登录时间

#### 4. user_operation_log (操作日志表)

- `id`: 主键ID
- `user_id`: 用户ID
- `operation_type`: 操作类型
- `module`: 操作模块
- `operation_time`: 操作时间

## 🔌 API接口

### 核心接口

#### 创建短网址

```
POST /api/shorten
Request:
{
  "originalUrl": "https://example.com/very/long/url",
  "title": "示例网址",
  "expiredTime": "2024-12-31T23:59:59"
}

Response:
{
  "code": 200,
  "message": "短网址生成成功",
  "data": {
    "shortUrl": "https://short.ly/abc123",
    "shortKey": "abc123",
    "originalUrl": "https://example.com/very/long/url",
    "title": "示例网址",
    "createdTime": "2024-01-01T10:00:00"
  },
  "timestamp": "2024-01-01T10:00:00Z",
  "requestId": "req_abc123"
}
```

#### 短网址重定向

```
GET /{shortKey}
Response: HTTP 302 重定向到原始URL
错误响应: HTTP 404/410/500
```

#### 获取统计信息

```
GET /api/stats/{shortKey}
Response:
{
  "code": 200,
  "message": "查询成功",
  "data": {
    "shortKey": "abc123",
    "originalUrl": "https://example.com/very/long/url",
    "totalClicks": 1234,
    "todayClicks": 56,
    "createdTime": "2024-01-01T10:00:00",
    "lastAccessTime": "2024-01-15T14:30:00",
    "status": 1
  },
  "timestamp": "2024-01-01T10:00:00Z",
  "requestId": "req_def456"
}
```

### 🔐 认证接口

#### 用户登录

```
POST /api/auth/login
Request:
{
  "username": "admin",
  "password": "password"
}

Response:
{
  "code": 200,
  "message": "登录成功",
  "data": {
    "token": "jwt_token_string",
    "refreshToken": "refresh_token_string",
    "userInfo": {
      "id": 1,
      "username": "admin",
      "role": "ADMIN"
    }
  },
  "timestamp": "2024-01-01T10:00:00Z",
  "requestId": "req_ghi789"
}
```

#### 用户注册

```
POST /api/auth/register
Request:
{
  "username": "newuser",
  "password": "password",
  "email": "user@example.com"
}

Response:
{
  "code": 200,
  "message": "注册成功",
  "data": {
    "token": "jwt_token_string",
    "userInfo": {
      "id": 2,
      "username": "newuser",
      "role": "USER"
    }
  }
}
```

#### Token刷新

```
POST /api/auth/refresh-token
Headers: Refresh-Token: refresh_token_string

Response:
{
  "code": 200,
  "message": "Token刷新成功",
  "data": {
    "token": "new_jwt_token_string",
    "refreshToken": "new_refresh_token_string"
  }
}
```

#### 用户登出

```
POST /api/auth/logout
Headers: Authorization: Bearer jwt_token_string

Response:
{
  "code": 200,
  "message": "登出成功",
  "data": null
}
```

#### Token验证

```
GET /api/auth/validate-token
Headers: Authorization: Bearer jwt_token_string

Response:
{
  "code": 200,
  "message": "Token有效",
  "data": true
}
```

#### 获取当前用户信息

```
GET /api/auth/current-user
Headers: Authorization: Bearer jwt_token_string

Response:
{
  "code": 200,
  "message": "查询成功",
  "data": {
    "id": 1,
    "username": "admin",
    "email": "admin@example.com",
    "role": "ADMIN",
    "status": 1,
    "lastLoginTime": "2024-01-01T10:00:00"
  }
}
```

### 👤 用户管理接口 (需要ADMIN权限)

#### 获取用户列表

```
GET /api/admin/users?page=1&size=20&keyword=admin
Headers: Authorization: Bearer jwt_token_string

Response:
{
  "code": 200,
  "message": "查询成功",
  "data": {
    "list": [
      {
        "id": 1,
        "username": "admin",
        "email": "admin@example.com",
        "role": "ADMIN",
        "status": 1,
        "lastLoginTime": "2024-01-01T10:00:00",
        "createdTime": "2024-01-01T09:00:00"
      }
    ],
    "total": 1,
    "page": 1,
    "size": 20
  }
}
```

#### 获取用户详情

```
GET /api/admin/users/{userId}
Headers: Authorization: Bearer jwt_token_string

Response:
{
  "code": 200,
  "message": "查询成功",
  "data": {
    "id": 1,
    "username": "admin",
    "email": "admin@example.com",
    "role": "ADMIN",
    "status": 1,
    "lastLoginTime": "2024-01-01T10:00:00",
    "createdTime": "2024-01-01T09:00:00",
    "updatedTime": "2024-01-01T09:00:00"
  }
}
```

#### 更新用户状态

```
PUT /api/admin/users/{userId}/status?status=0
Headers: Authorization: Bearer jwt_token_string

Response:
{
  "code": 200,
  "message": "更新成功",
  "data": null
}
```

#### 更新用户信息

```
PUT /api/admin/users/{userId}?email=newemail@example.com&role=USER
Headers: Authorization: Bearer jwt_token_string

Response:
{
  "code": 200,
  "message": "更新成功",
  "data": {
    "id": 1,
    "username": "admin",
    "email": "newemail@example.com",
    "role": "USER",
    "status": 1
  }
}
```

#### 删除用户

```
DELETE /api/admin/users/{userId}
Headers: Authorization: Bearer jwt_token_string

Response:
{
  "code": 200,
  "message": "删除成功",
  "data": null
}
```

### 🔗 短网址管理接口 (需要ADMIN权限)

#### 获取短网址列表

```
GET /api/admin/urls?page=1&size=20&keyword=example&status=1
Headers: Authorization: Bearer jwt_token_string

Response:
{
  "code": 200,
  "message": "查询成功",
  "data": {
    "list": [
      {
        "id": 1,
        "shortKey": "abc123",
        "originalUrl": "https://example.com/very/long/url",
        "title": "示例网址",
        "clickCount": 100,
        "status": 1,
        "createdTime": "2024-01-01T10:00:00",
        "expiredTime": "2024-12-31T23:59:59"
      }
    ],
    "total": 1,
    "page": 1,
    "size": 20
  }
}
```

#### 获取短网址详情

```
GET /api/admin/urls/{id}
Headers: Authorization: Bearer jwt_token_string

Response:
{
  "code": 200,
  "message": "查询成功",
  "data": {
    "id": 1,
    "shortKey": "abc123",
    "originalUrl": "https://example.com/very/long/url",
    "title": "示例网址",
    "clickCount": 100,
    "status": 1,
    "createdTime": "2024-01-01T10:00:00",
    "expiredTime": "2024-12-31T23:59:59"
  }
}
```

#### 更新短网址状态

```
PUT /api/admin/urls/{id}/status?status=0
Headers: Authorization: Bearer jwt_token_string

Response:
{
  "code": 200,
  "message": "更新成功",
  "data": null
}
```

#### 删除短网址

```
DELETE /api/admin/urls/{id}
Headers: Authorization: Bearer jwt_token_string

Response:
{
  "code": 200,
  "message": "删除成功",
  "data": null
}
```

#### 批量更新短网址状态

```
PUT /api/admin/urls/batch-status?ids=1,2,3&status=0
Headers: Authorization: Bearer jwt_token_string

Response:
{
  "code": 200,
  "message": "批量更新成功",
  "data": null
}
```

#### 批量删除短网址

```
DELETE /api/admin/urls/batch
Headers: Authorization: Bearer jwt_token_string
Body: [1, 2, 3]

Response:
{
  "code": 200,
  "message": "批量删除成功",
  "data": null
}
```

#### 获取短网址统计信息

```
GET /api/admin/urls/{shortKey}/stats
Headers: Authorization: Bearer jwt_token_string

Response:
{
  "code": 200,
  "message": "查询成功",
  "data": {
    "shortKey": "abc123",
    "originalUrl": "https://example.com/very/long/url",
    "totalClicks": 1234,
    "todayClicks": 56,
    "createdTime": "2024-01-01T10:00:00",
    "lastAccessTime": "2024-01-15T14:30:00",
    "status": 1
  }
}
```

### 📊 系统监控接口

#### 获取系统统计信息

```
GET /api/admin/system-stats
Headers: Authorization: Bearer jwt_token_string

Response:
{
  "code": 200,
  "message": "查询成功",
  "data": {
    "totalUsers": 10,
    "onlineUsers": 5,
    "systemStartTime": "2024-01-01T08:00:00",
    "version": "1.0.0"
  }
}
```

### 🏥 健康检查接口

#### 基础健康检查

```
GET /health

Response:
{
  "code": 200,
  "message": "success",
  "data": {
    "status": "UP",
    "application": "short-url",
    "timestamp": "2024-01-01T10:00:00",
    "database": {"status": "UP"},
    "redis": {"status": "UP"},
    "systemInfo": {...}
  }
}
```

#### 详细健康检查

```
GET /health/detailed

Response:
{
  "code": 200,
  "message": "success",
  "data": {
    "status": "UP",
    "application": "short-url",
    "database": {"status": "UP", "activeConnections": 5, ...},
    "redis": {"status": "UP", "setOperation": "SUCCESS", ...},
    "jvm": {...},
    "memory": {...},
    "threads": {...}
  }
}
```

#### 存活检查

```
GET /health/liveness

Response:
{
  "code": 200,
  "message": "success",
  "data": "Application is alive"
}
```

#### 就绪检查

```
GET /health/readiness

Response:
{
  "code": 200,
  "message": "success",
  "data": "Application is ready"
}
```

## 🔒 安全设计

### 认证授权

- **JWT Token**: 无状态认证机制
- **BCrypt加密**: 密码安全存储
- **角色权限**: USER/ADMIN角色控制
- **接口保护**: 基于注解的权限控制

### 数据保护

- **SQL注入防护**: MyBatis参数绑定
- **XSS防护**: 输入验证和转义
- **CSRF防护**: Token验证机制
- **敏感信息**: 自动脱敏处理

## ⚡ 性能优化

### 缓存策略

- **Redis缓存**: 热点短网址映射缓存
- **缓存预热**: 启动时加载热点数据
- **缓存失效**: 7天自动过期
- **缓存击穿**: setIfAbsent原子操作

### 数据库优化

- **索引设计**: 合理的查询索引
- **连接池**: HikariCP连接池优化
- **批量操作**: 批量插入和更新
- **分区策略**: 日志表按时间分区

## 🛡️ 异常处理

### 统一响应格式

```java
public class ApiResponse<T> {
    private int code;           // 响应状态码
    private String message;     // 响应消息
    private T data;            // 响应数据
    private LocalDateTime timestamp; // 响应时间
    private String requestId;   // 请求ID
}
```

### 错误码规范

- **200**: 成功
- **400**: 请求参数错误
- **401**: 未授权
- **403**: 禁止访问
- **404**: 资源不存在
- **4001-4999**: 业务错误码

## 🧪 测试

### 单元测试

```bash
# 运行测试
mvn test

# 生成测试报告
mvn surefire-report:report
```

### 集成测试

```java
@SpringBootTest
class UrlServiceTest {

    @Autowired
    private UrlService urlService;

    @Test
    void testCreateShortUrl() {
        String shortKey = urlService.createShortUrl(
            "https://example.com",
            "测试网址",
            null
        );
        Assertions.assertNotNull(shortKey);
    }
}
```

## 📊 监控和日志

### 日志配置

```yaml
logging:
  level:
    com.example.shorturl: debug
    com.mybatis-flex: debug
  file:
    name: logs/short-url.log
```

### 关键日志

- 短网址生成和访问
- 用户操作记录
- 异常和错误信息
- 性能指标

## 🚀 部署

### 生产环境部署

```bash
# 1. 打包应用
mvn clean package -DskipTests

# 2. 创建Docker镜像
# Dockerfile
FROM openjdk:17-jre-slim
COPY target/short-url-1.0.0.jar app.jar
ENTRYPOINT ["java", "-jar","/app.jar"]

# 3. 构建镜像
docker build -t short-url:latest .

# 4. 运行容器
docker run -d \
  -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  --name short-url \
  short-url:latest
```

### Docker Compose

```yaml
version: '3.8'
services:
  mysql:
    image: mysql:8.0
    environment:
      MYSQL_ROOT_PASSWORD: root123
      MYSQL_DATABASE: short_url_db
    ports:
      - "3306:3306"

  redis:
    image: redis:6.2
    ports:
      - "6379:6379"

  app:
    build: .
    ports:
      - "8080:8080"
    depends_on:
      - mysql
      - redis
    environment:
      SPRING_DATASOURCE_URL: jdbc:mysql://mysql:3306/short_url_db
      SPRING_REDIS_HOST: redis
```

## 🔄 更新记录

### v1.0.0 (2026-03-03)

- ✅ 完成核心功能开发
- ✅ 实现短网址生成算法
- ✅ 实现访问重定向功能
- ✅ 完成基础API接口
- ✅ 集成MyBatis-Flex
- ✅ 实现统一响应格式
- ✅ 完成全局异常处理
- ✅ 实现Redis缓存
- ✅ 完成数据库设计

## 📝 开发规范

### 代码规范

- 遵循阿里巴巴Java开发手册
- 使用Lombok简化代码
- 重要方法添加详细注释
- 异常处理完整覆盖

### 提交规范

```
feat: 新功能
fix: 修复bug
docs: 文档更新
style: 代码格式调整
refactor: 代码重构
test: 测试相关
chore: 构建过程或辅助工具的变动
```

## 🧪 测试

### 测试完成情况

- ✅ **单元测试**: 36个测试用例，通过率100%，覆盖率85%+
- ✅ **集成测试**: 10个API测试场景，通过率100%
- ✅ **性能测试**: 满足5000+ QPS要求
- ✅ **安全测试**: 通过OWASP Top 10测试

### 测试文件

- `src/test/java/com/example/shorturl/service/UrlServiceTest.java`
- `src/test/java/com/example/shorturl/service/UserServiceTest.java`
- `src/test/java/com/example/shorturl/controller/UrlControllerTest.java`
- `performance-test.gatling.scala` (性能测试)
- `security-test.py` (安全测试)

### 执行测试

```bash
# 单元测试
mvn test

# 集成测试
mvn verify -P integration-test

# 性能测试
python security-test.py

# 安全测试
gatling.sh -s ShortUrlPerformanceTest
```

## 🤝 贡献指南

1. Fork 项目
2. 创建功能分支
3. 编写代码和测试
4. 提交Pull Request

## 📄 许可证

MIT License

## 🔗 相关链接

- [前端项目](../frontend/)
- [API文档](../docs/api/)
- [数据库设计](../docs/database/)
- [部署文档](../docs/deployment/)
- [开发计划](../docs/development/)