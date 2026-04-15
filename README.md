# 课程文档智能管理系统（可离线构建版）

本项目按你的毕设目标生成，覆盖：

- 文档智能分类（Scikit-learn）
- 关键词自动提取（Jieba）
- 基于内容的全文检索（Elasticsearch）
- Java 后端统一 API

> 已解决你提到的 **Maven Central 403** 问题：后端改为 **纯 JDK 17 构建**（`javac + jar`），不再依赖 Maven 下载任何远程依赖。

## 目录结构

```text
.
├── backend/                     # Java 后端（纯 JDK，无 Maven 依赖）
├── python-nlp/                  # Python NLP 微服务
├── docker-compose.yml           # 一键启动 MySQL/ES/NLP/Backend
└── 毕设-课程文档智能管理系统设计与实现.md
```

## 技术栈映射

- **Java**: JDK 17 + `com.sun.net.httpserver` + `java.net.http.HttpClient`
- **MySQL**: 元数据持久化（compose 已提供）
- **Elasticsearch**: 全文索引与检索
- **NLP**:
  - Scikit-learn：文本分类（TF-IDF + LinearSVC）
  - Jieba：中文分词与关键词提取

## 新手完整安装教程

如果你不确定命令在哪运行，先看下面这一节。


## 命令运行位置说明

默认都在项目根目录 `codex/` 执行（除非命令里明确写了 `cd xxx`）。

```bash
cd codex
```


如果你电脑几乎是空环境，请先看：`docs/INSTALL_ZH.md`。

如果你用 IDEA + VSCode，直接看：`docs/IDEA_VSCODE_SETUP.md`。

## 快速启动

## 30 秒快速运行（推荐）

### A. 看前端页面（最直观）

```bash
bash scripts/quick_start.sh frontend
```

然后浏览器打开：`http://127.0.0.1:5173`

### B. 看接口效果（命令行）

```bash
bash scripts/quick_start.sh api
```

### C. PowerShell（无 bash）启动

如果你在 Windows PowerShell 报 `bash 不是内部或外部命令`，请改用：

```powershell
./scripts/quick_start.ps1 frontend
# 或
./scripts/quick_start.ps1 api
```

若提示脚本执行被系统拦截，可先执行：

```powershell
Set-ExecutionPolicy -Scope Process Bypass
```

会自动调用并打印：
- `GET /health`
- `POST /api/documents/analyze`
- `POST /api/documents/upload-and-index`
- `GET /api/documents/search`

> 说明：这两种快速运行都使用本地 Mock 服务，不依赖 Docker、MySQL、Elasticsearch、FastAPI 安装。


### 方式一：Docker Compose（推荐）

```bash
docker compose up --build
```

服务端口：

- Backend: `http://localhost:8080`
- NLP: `http://localhost:8000`
- Elasticsearch: `http://localhost:9200`
- MySQL: `localhost:3306`

### 方式二：本地分别启动

1. 启动 Elasticsearch 与 MySQL
2. 启动 NLP 服务：

```bash
cd python-nlp
pip install -r requirements.txt
uvicorn app.main:app --reload --port 8000
```

3. 启动 Java 后端（无需 Maven）：

```bash
cd backend
mkdir -p out
javac -encoding UTF-8 -d out $(find src/main/java -name "*.java")
java -cp out com.example.docsys.StandaloneServer
```


### 方式三：一键本地演示（无 Docker、无第三方 Python 依赖）

如果你只是想“先跑起来看看”，可以直接运行：

```bash
bash scripts/run_local_demo.sh
```

该脚本会：

1. 启动本地 Mock NLP 服务（18000）和 Mock Elasticsearch 服务（19200）；
2. 编译并启动 Java 后端（8080）；
3. 自动调用 `health/analyze/upload-and-index/search` 四个接口并打印结果。

## 核心接口

### 1）文档分析（分类 + 关键词）

`POST /api/documents/analyze`

```json
{
  "title": "机器学习课程教案",
  "content": "本节课讲解支持向量机与文本分类",
  "docType": "教案",
  "courseName": "机器学习"
}
```

### 2）上传并索引

`POST /api/documents/upload-and-index`

后端会先调用 NLP 服务分析，再将文档写入 Elasticsearch。

### 3）全文检索

`GET /api/documents/search?q=支持向量机&size=10`

返回 ES 相关性排序结果和高亮片段。


### 前端展示（可视化演示）

已提供前端演示页面 `frontend/index.html`，可直接通过浏览器操作三个核心流程：

- 文档分析（`/api/documents/analyze`）
- 上传并索引（`/api/documents/upload-and-index`）
- 全文检索（`/api/documents/search`）

一键启动前后端联调演示：

```bash
bash scripts/run_frontend_demo.sh
```

启动后访问：

- `http://127.0.0.1:5173`（前端页面）

## 后续你可以直接扩展

- 接入真实文件上传与 PDF/Word 解析
- 增加 MySQL 文档元数据表
- 引入模型训练数据集与定时重训
- 增加前端页面（Vue/React）
- 加入权限系统（管理员/教师/学生）



## 推送到 GitHub 仓库

你可以直接用下面命令把当前分支推到你的 GitHub：

```bash
bash scripts/push_to_github.sh https://github.com/<你的用户名>/<你的仓库>.git
```

也可以指定分支：

```bash
bash scripts/push_to_github.sh https://github.com/<你的用户名>/<你的仓库>.git main
```



### 网络受限时（GitHub 403）备用方案

如果当前环境无法访问 GitHub（如 `CONNECT tunnel failed, response 403`），可先导出 bundle：

```bash
bash scripts/create_git_bundle.sh
```

生成的 `*.bundle` 文件可在有网环境恢复并推送到 GitHub。

