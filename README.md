# 健康食谱管理与膳食规划系统

## 项目概述
健康食谱管理与膳食规划系统是一个集食谱管理、个性化营养分析、膳食规划为一体的健康管理平台。系统允许用户根据个人健康数据和饮食目标，获取定制化的营养分析和膳食建议，帮助用户实现健康饮食管理。

## 功能特性

### 用户管理
- 用户注册与登录
- 个人信息管理
- 健康数据维护（身高、体重、年龄、活动水平等）

### 食谱管理
- 食谱浏览与搜索
- 食谱分类与详情查看
- 管理员食谱增删改查

### 营养分析
- 基于用户健康数据的营养需求计算
- BMI分析
- 营养素需求分析
- 个性化营养建议生成

### 膳食规划
- 膳食计划创建与管理
- 日历视图与列表视图切换
- 食谱选择与添加

### 文件管理
- 图片文件上传与管理
- 基于阿里云OSS的对象存储

## 技术栈

### 前端
- **框架**：Vue.js 3.x
- **UI组件库**：Element Plus
- **HTTP客户端**：Axios

### 后端
- **框架**：Spring Boot 2.7.x
- **ORM**：MyBatis
- **数据库**：MySQL 8.x
- **对象存储**：阿里云OSS

## 系统架构
系统采用前后端分离架构，包含以下层次：

1. **前端应用层**：基于Vue.js构建的单页面应用
2. **后端服务层**：基于Spring Boot的RESTful API服务
3. **数据存储层**：MySQL数据库
4. **对象存储层**：阿里云OSS，用于存储用户上传的图片等文件资源

## 环境要求

### 开发环境
- Node.js 16+
- npm 8+
- JDK 1.8+
- MySQL 8.0+

## 项目结构

```
health_system/
├── README.md               # 项目说明文档
├── document/               # 文档目录
│   ├── 数据库设计文档.md    # 数据库设计文档
│   ├── 概要设计文档.md      # 概要设计文档
│   ├── 测试文档.md          # 测试文档
│   ├── 用户手册.md          # 用户手册
│   ├── 详细设计文档.md      # 详细设计文档
│   └── 需求说明书.md        # 需求说明书
├── system/                 # 系统主目录
│   ├── springboot/         # 后端代码
│   │   ├── src/            # 源代码
│   │   ├── files/          # 文件目录
│   │   └── pom.xml         # Maven配置
│   ├── vue/                # 前端代码
│   │   ├── src/            # 源代码
│   │   ├── public/         # 静态资源
│   │   ├── dist/           # 构建输出
│   │   └── package.json    # npm配置
│   └── sql/                # SQL脚本
│       └── health_system.sql  # 数据库初始化脚本
```

## 安装部署

### 1. 数据库准备
1. 安装MySQL 8.0数据库
2. 创建数据库：`CREATE DATABASE health_system CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;`
3. 导入初始化脚本：`source d:/health_system/system/sql/health_system.sql;`

### 2. 后端部署

#### 方式一：开发环境运行
1. 克隆项目代码
2. 使用IntelliJ IDEA打开springboot目录
3. 修改数据库配置文件（application.yml）
4. 运行SpringBootApplication启动类

#### 方式二：生产环境部署
1. 编译打包：`cd springboot && mvn clean package`
2. 运行jar包：`java -jar target/health_system.jar`

### 3. 前端部署

#### 开发环境
1. 进入vue目录：`cd vue`
2. 安装依赖：`npm install`
3. 运行开发服务器：`npm run dev`

#### 生产环境
1. 构建项目：`npm run build`
2. 将dist目录部署到Web服务器（如Nginx）

```nginx
server {
    listen 80;
    server_name localhost;
    
    location / {
        root /path/to/dist;
        index index.html;
        try_files $uri $uri/ /index.html;
    }
    
    location /api {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
```

## 系统配置

### 数据库配置
在后端配置文件中设置数据库连接信息：
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/health_system?useSSL=false&serverTimezone=UTC
    username: root
    password: your_password
    driver-class-name: com.mysql.cj.jdbc.Driver
```

### API配置
前端API基础路径配置：
```javascript
// .env.development
VITE_API_BASE_URL = '/api'

// .env.production
VITE_API_BASE_URL = 'http://your-api-server.com/api'
```

### OSS对象存储配置
系统使用阿里云OSS进行对象存储，需要在本地环境配置以下环境变量：

#### Windows系统配置
1. 右键点击「此电脑」→ 「属性」→ 「高级系统设置」→ 「环境变量」
2. 在「系统变量」区域点击「新建」，添加以下环境变量：
   - 变量名：`OSS_ACCESS_KEY_ID`，变量值：您的阿里云OSS AccessKey ID
   - 变量名：`OSS_ACCESS_KEY_SECRET`，变量值：您的阿里云OSS AccessKey Secret
3. 重启开发环境（IDE或命令行窗口）使配置生效

#### Linux/Mac系统配置
在终端中执行以下命令（临时生效）：
```bash
export OSS_ACCESS_KEY_ID=您的阿里云OSS AccessKey ID
export OSS_ACCESS_KEY_SECRET=您的阿里云OSS AccessKey Secret
```

或者将以上命令添加到`~/.bashrc`或`~/.zshrc`文件中使其永久生效。

**注意事项**：
- 请确保您的阿里云OSS账号具有相应的访问权限
- AccessKey信息属于敏感信息，请妥善保管，避免泄露
- 如需修改OSS其他配置（如endpoint、bucketName等），请直接编辑application.yml文件中的相关配置项

## 使用说明

### 1. 用户注册与登录
- 访问系统首页，点击「注册」按钮
- 填写个人信息完成注册
- 使用注册的账号密码登录系统

### 2. 浏览食谱
- 在首页浏览推荐食谱
- 使用搜索框查找特定食谱
- 通过分类筛选食谱

### 3. 制定膳食计划
- 进入「膳食计划」模块
- 点击「新建计划」按钮
- 添加食谱到每日膳食计划

### 4. 查看营养分析
- 在膳食计划详情页查看营养分析
- 根据分析结果调整膳食计划

## 开发指南

### 后端开发
1. 遵循RESTful API设计规范
2. 代码分层：controller、service、mapper、entity
3. 单元测试覆盖核心功能

### 前端开发
1. 组件化开发，提高代码复用性
2. 响应式设计，适配不同设备

## 安全说明
- 密码采用MD5加盐加密存储

## 常见问题

### 1. 数据库连接失败
- 检查MySQL服务是否启动
- 验证数据库用户名和密码是否正确
- 确认数据库名称是否存在

### 2. 前端页面空白
- 检查API接口是否正常响应
- 查看浏览器控制台错误信息
- 确认构建文件是否完整

### 3. 用户无法登录
- 检查密码是否正确
- 确认用户账号是否被禁用
- 查看系统日志中的错误信息
