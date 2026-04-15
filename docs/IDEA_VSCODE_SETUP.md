# 用 IDEA + VSCode 从零拉代码并运行（适合你当前环境）

你说你：
- 后端用 **IntelliJ IDEA**
- 前端用 **VSCode**

这份文档就按这个组合来。

---


## 0）先解决你现在这个报错（git 命令找不到）

你截图里是：

```text
git : 无法将“git”项识别为 cmdlet
```

这表示 Git 没安装或 PATH 没生效。

### 立刻修复（Windows）

```powershell
winget install --id Git.Git -e --source winget
```

安装完成后：
1. 关掉当前 PowerShell 窗口；
2. 重新打开 PowerShell；
3. 执行：

```powershell
git --version
```

看到版本号再继续下面步骤。

---

## 1）先准备工具

请确认你电脑已安装：

1. Git
2. JDK 17（后端 Java）
3. Python 3.10+（跑 mock 服务、前端静态服务）
4. IntelliJ IDEA
5. VSCode

在终端检查：

```bash
git --version
java -version
javac -version
python --version
```

---

## 2）把代码拉到你电脑（最关键）

### 方式 A（推荐，Git）

打开终端执行：

```bash
git clone https://github.com/zzk777-cpu/codex.git
cd codex
```

### 方式 B（Git 不通时）

1. 打开浏览器访问：`https://github.com/zzk777-cpu/codex`
2. 点 `Code -> Download ZIP`
3. 解压后进入 `codex` 目录

---

## 3）用 IDEA 打开后端并运行

### 3.1 打开项目

- 打开 IDEA
- `File -> Open` 选择 `codex` 目录（或者只选 `codex/backend` 目录也可）

### 3.2 配置 JDK

- `File -> Project Structure -> Project SDK` 选择 JDK 17

### 3.3 运行后端（推荐先用脚本整体跑通）

因为本项目是“纯 JDK + 脚本”模式，不是 Maven/Spring Boot 标准项目，**最稳做法**是在 IDEA 内置 Terminal 运行：

```bash
bash scripts/quick_start.sh api
```

如果你要常驻后端（给前端联调）：

```bash
bash scripts/quick_start.sh frontend
```

---

## 4）用 VSCode 打开前端并联调

### 4.1 打开前端代码

- 打开 VSCode
- `File -> Open Folder` 选择 `codex/frontend`

### 4.2 前端页面如何启动

这个前端是静态页面（`index.html`），不需要 npm。

你有两种方式：

#### 方式 1（推荐）
在终端直接跑：

```bash
cd codex
bash scripts/quick_start.sh frontend
```

然后浏览器打开：

```text
http://127.0.0.1:5173
```

#### 方式 2（只开前端静态服务）

```bash
cd codex/frontend
python -m http.server 5173
```

> 注意：只开前端还不够，后端也要启动（否则按钮调用接口会失败）。

---

## 5）你应该看到的运行效果

### 接口模式（`quick_start.sh api`）
会打印：
- `/health` 返回 `{"status":"ok"}`
- `analyze` 返回分类+关键词
- `upload-and-index` 返回分析+索引结果
- `search` 返回命中结果

### 前端模式（`quick_start.sh frontend`）
浏览器打开页面后：
- 填写标题/内容
- 点“调用 /api/documents/analyze”
- 下方 `JSON` 会出现分析结果

---

## 6）常见问题（你这个场景最常见）

### Q1：IDEA 里 Run 不起来 main 类？
A：这是正常的。这个项目主要靠脚本编译运行，优先在 IDEA Terminal 里执行脚本。

### Q2：Windows 执行 `bash` 报错？
A：你可以直接改用 PowerShell 脚本（无需 bash）：

```powershell
./scripts/quick_start.ps1 frontend
# 或
./scripts/quick_start.ps1 api
```

若提示脚本执行被系统拦截，可先执行：

```powershell
Set-ExecutionPolicy -Scope Process Bypass
```

也可以安装 Git Bash / WSL。

### Q3：点击前端按钮没反应/报错？
A：后端可能没启动。先执行 `./scripts/quick_start.ps1 frontend`（或 bash 版本）。

### Q4：端口占用（8080/5173/18000/19200）
A：关闭占用端口的进程再重启。

---

## 7）给你一套最短可执行步骤（复制即可）

> 如果你就在 PowerShell 里，不想装 bash，直接用 `quick_start.ps1`。


```bash
# 1) 拉代码
git clone https://github.com/zzk777-cpu/codex.git
cd codex

# 2) 跑前后端联调（最直观）
./scripts/quick_start.ps1 frontend

# 3) 打开浏览器看页面
# http://127.0.0.1:5173
```

