# 从零开始在你的电脑运行本项目（Windows/macOS/Linux）

> 目标：你电脑“什么都没有”的情况下，把代码拉到本地并跑起来。


## 0. 这些命令“在哪儿运行”

统一规则：
- 除非命令里写了 `cd xxx`，否则都在**项目根目录 `codex/`**运行；
- 你需要先打开终端（Windows 推荐 Git Bash 或 PowerShell）；
- 先进入项目目录再执行：

```bash
cd codex
```

例如：
- `bash scripts/quick_start.sh api` 在 `codex/` 运行；
- `bash scripts/quick_start.sh frontend` 在 `codex/` 运行；
- 只有写了 `cd frontend` 的命令才是在 `frontend/` 子目录运行。

---

## 1. 先安装最少工具

### 必装
1. **Git**（拉代码）
2. **Python 3.10+**（跑 mock 服务和前端静态服务器）
3. **JDK 17**（编译并运行 Java 后端）

### 可选（后面进阶再装）
- Docker Desktop（如果你要跑 mysql/elasticsearch/python-nlp 的完整版）

---

## 2. 检查安装是否成功

打开终端（Windows 用 PowerShell），执行：

```bash
git --version
python --version
java -version
javac -version
```

只要都能输出版本号就行。

---

## 3. 把代码下载到你电脑

```bash
git clone https://github.com/zzk777-cpu/codex.git
cd codex
```

如果你在国内网络慢：
- 可以使用你自己的代理后再 clone；
- 或先在浏览器下载 ZIP 再解压。

---

## 4. 第一种运行方式（推荐新手）

这是最简单的：不依赖 Docker、不依赖 MySQL/ES/FastAPI。

### 4.1 只看接口效果（自动跑完）

```bash
bash scripts/quick_start.sh api
```

你会看到 4 个接口的返回：
- `/health`
- `/api/documents/analyze`
- `/api/documents/upload-and-index`
- `/api/documents/search`

### 4.2 看前端页面效果（可视化）

```bash
bash scripts/quick_start.sh frontend
```

然后浏览器打开：

```text
http://127.0.0.1:5173
```

看到页面后，可点按钮调用分析/上传索引/检索。

> 停止服务：终端按 `Ctrl + C`。

---

## 5. 第二种运行方式（分别手动启动）

如果你想理解每一步，可手动运行：

### 5.1 启动 mock 服务

```bash
python scripts/mock_services.py
```

它会占用端口：
- 18000（Mock NLP）
- 19200（Mock ES）

### 5.2 新开一个终端，编译并启动 Java 后端

```bash
mkdir -p backend/out
javac -encoding UTF-8 -d backend/out $(find backend/src/main/java -name '*.java')
SERVICES_NLP_BASE_URL=http://127.0.0.1:18000 SERVICES_ES_BASE_URL=http://127.0.0.1:19200 SERVICES_ES_INDEX=course_documents java -cp backend/out com.example.docsys.StandaloneServer
```

### 5.3 再开一个终端，启动前端静态服务

```bash
cd frontend
python -m http.server 5173
```

浏览器打开 `http://127.0.0.1:5173`。

---

## 6. Windows 用户注意

上面的脚本是 Bash 风格（`.sh`），建议：

### 方案 A（推荐）
安装 **Git Bash**，在 Git Bash 里执行这些命令。

### 方案 B
用 WSL（Windows Subsystem for Linux）执行。

### 方案 C（纯 PowerShell）
不跑 `.sh`，改为按第 5 节手动逐条执行（命令语法要做少量适配）。

---

## 7. 常见报错排查

### 7.1 `javac: command not found`
说明 JDK 没装好或没配 PATH。

### 7.2 端口被占用（8080/5173/18000/19200）
关掉占用进程，或改脚本端口。

### 7.3 Python 命令不可用
尝试改为 `python3`。

### 7.4 GitHub 拉代码失败
检查网络/代理；也可先下载 ZIP。


### 7.5 `bash : 无法将“bash”项识别为 cmdlet...`
这是 Windows PowerShell 常见错误。你有两种方案：

1. 安装 Git Bash / WSL 后继续执行 `.sh`；
2. 直接使用 PowerShell 版本脚本：

```powershell
./scripts/quick_start.ps1 frontend
# 或
./scripts/quick_start.ps1 api
```

若提示脚本执行被系统拦截，可先执行：

```powershell
Set-ExecutionPolicy -Scope Process Bypass
```

### 7.6 `git : 无法将“git”项识别为 cmdlet...`
这是你截图里的错误，说明 **Git 没安装** 或 **没加到 PATH**。

处理步骤（Windows）：

1. 安装 Git（任选其一）
   - 官网安装包：`https://git-scm.com/download/win`
   - 或 PowerShell：

   ```powershell
   winget install --id Git.Git -e --source winget
   ```

2. 安装后**关闭并重新打开** PowerShell（很重要）
3. 验证：

   ```powershell
   git --version
   ```

4. 再执行：

   ```powershell
   git clone https://github.com/zzk777-cpu/codex.git
   cd codex
   ```

如果第 3 步仍报错，说明 PATH 没生效：
- 重启电脑后再试；
- 或手动把 `C:\Program Files\Git\cmd` 加入系统 PATH。

---

## 8. 下一步建议

先跑通第 4 节，再考虑：
- 接入真实 Elasticsearch/MySQL；
- 启动 python-nlp 真服务；
- 对接你自己的前端页面和数据库表。

