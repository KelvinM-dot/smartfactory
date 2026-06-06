# smartfactory

集成订单、生产、能源等综合业务功能的数字工厂可视化管理 POC（配置驱动制造数据平台）。

包含 API 服务、Web 工作台、产线模拟器与 JSON Schema 主数据。

## 项目结构

| 目录 | 说明 |
|------|------|
| `dataplatform-api` | Spring Boot API（端口 3001） |
| `dataplatform-web` | Vue 3 前端（端口 3000） |
| `line-simulator` | Python 产线仿真与遥测推送（端口 3002） |
| `schemas` | 配置模型、主数据与接口契约 |
| `scripts` | 数据校验与验收脚本 |

## 环境要求

- JDK 17、Maven 3.8+
- Node.js 18+
- Python 3.9+

## 快速启动

在三个终端中分别执行：

**1. API**

```bash
cd dataplatform-api
mvn spring-boot:run
```

**2. 产线模拟器**

```bash
cd line-simulator
python3 -m venv .venv
source .venv/bin/activate   # Windows: .venv\Scripts\activate
pip install -r requirements.txt
python main.py
```

**3. 前端**

```bash
cd dataplatform-web
npm install
npm run dev
```

浏览器访问：<http://localhost:3000>

## 配置说明

- API 配置：`dataplatform-api/src/main/resources/application.yml`
- 模拟器配置：`line-simulator/simulator/config.py`（默认连接本地 API `http://127.0.0.1:3001`）
- 主数据与领域配置：见 [schemas/README.md](./schemas/README.md)

## 说明

本项目为 POC 演示，使用 SQLite 本地库，主数据为示例工厂配置，不适用于生产环境。

## 许可证

本项目采用 [GPL-3.0](./LICENSE) 许可证。
