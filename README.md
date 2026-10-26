<div align="center">

# Atlas

### A zero-dependency Java framework for building Model Context Protocol servers

Give any large language model safe, structured access to your tools, data and prompts — over plain stdio, with nothing but a JDK 21 on the classpath.

[![Java](https://img.shields.io/badge/Java-21-ED8B00?logo=openjdk&logoColor=white)](https://openjdk.org/)
[![License](https://img.shields.io/badge/License-MIT-green)](LICENSE)
[![Dependencies](https://img.shields.io/badge/Dependencies-None-0a0a0a)](https://github.com/WsNodeX/atlas)
[![Platform](https://img.shields.io/badge/Platform-JVM-blue)](https://en.wikipedia.org/wiki/Java_virtual_machine)
[![Build & Test](https://github.com/WsNodeX/atlas/actions/workflows/ci.yml/badge.svg)](https://github.com/WsNodeX/atlas/actions/workflows/ci.yml)
[![MCP](https://img.shields.io/badge/MCP-2024--11--05-9cf)](https://modelcontextprotocol.io/)

</div>

<p align="center">
  <a href="#-why-atlas">Why</a> ·
  <a href="#-features">Features</a> ·
  <a href="#-installation">Installation</a> ·
  <a href="#-quick-start">Quick Start</a> ·
  <a href="#-writing-your-own-server">Write a server</a> ·
  <a href="#-architecture">Architecture</a> ·
  <a href="#-api-reference">API</a> ·
  <a href="#-roadmap">Roadmap</a> ·
  <a href="#-contributing">Contributing</a>
</p>

---

## ✨ Why Atlas

The [Model Context Protocol](https://modelcontextprotocol.io/) (MCP) has become the lingua franca between LLMs and external capabilities. Almost every mainstream agent runtime can now talk to an MCP server over stdio — but most SDKs drag in a heavy dependency tree (Netty, Jackson, gRPC…) just to exchange JSON messages.

Atlas takes the opposite stance:

- **You should not need a build tool or a 40 MB dependency jar to expose a tool to a model.** The protocol is JSON-RPC 2.0 over newline-delimited stdio. Atlas implements exactly that, plus the small amount of structure the protocol needs, in ~15 Java files.
- **It is readable.** Every layer — JSON parsing, message routing, tool dispatch — is plain JDK code you can read in an afternoon and audit for your own security boundary.
- **It is testable.** The transport is an interface, so the entire server is exercised by unit tests through an in-memory channel with zero subprocesses.

If you want to understand how MCP *actually* works rather than treat it as a black box, Atlas is a good place to start.

## 🚀 Features

- **Full JSON-RPC 2.0 dispatch** over a pluggable transport. Ships `StdioTransport` (production) and `InMemoryTransport` (tests / demos).
- **Protocol methods**: `initialize`, `ping`, `tools/list`, `tools/call`, `resources/list`, `resources/read`, `prompts/list`, `prompts/get`, and silent handling of lifecycle notifications.
- **Strongly typed building blocks**: `Tool`, `Resource`, `Prompt`, `Content`, plus a tiny `Schemas` helper for JSON-Schema input definitions.
- **Capability negotiation**: the server only advertises the features it actually implements.
- **Zero runtime dependencies** — compiles and runs on a bare JDK 21.
- **28 unit tests** covering parsing, routing, tool execution and error paths.
- **MIT licensed.**

## 📦 Installation

Atlas has no build step and no external libraries. Clone and compile with the JDK you already have:

```bash
git clone https://github.com/WsNodeX/atlas.git
cd atlas
javac -d build/main \
  src/atlas/json/*.java src/atlas/protocol/*.java src/atlas/transport/*.java \
  src/atlas/registry/*.java src/atlas/server/*.java \
  src/atlas/examples/*.java src/atlas/examples/tools/*.java
```

That is the whole install. No Maven, no Gradle, no `pom.xml`, no network.

## ⚡ Quick Start

Run the bundled reference server. With no arguments it plays a scripted conversation over an in-memory channel so you can see the exact wire format; pass `--stdio` to launch the real server a client would connect to.

```bash
java -cp build/main atlas.examples.Demo
```

Expected output (trimmed to the interesting parts):

```json
{
  "jsonrpc": "2.0",
  "id": 1,
  "result": {
    "protocolVersion": "2024-11-05",
    "capabilities": { "tools": { "listChanged": false }, "resources": {…}, "prompts": {…} },
    "serverInfo": { "name": "atlas-demo", "version": "1.0.0" }
  }
}
---
{
  "jsonrpc": "2.0",
  "id": 3,
  "result": { "content": [ { "type": "text", "text": "(2+3)*4/2 = 10" } ], "isError": false }
}
---
{
  "jsonrpc": "2.0",
  "id": 5,
  "result": { "content": [ { "type": "text", "text": "2026-10-26T14:50:27+08:00 (Asia/Shanghai)" } ], "isError": false }
}
---
{
  "jsonrpc": "2.0",
  "id": 9,
  "result": { "content": [ { "type": "text", "text": "Could not evaluate expression: division by zero" } ], "isError": true }
}
```

To run the real stdio server (the one an MCP client launches as a subprocess):

```bash
java -cp build/main atlas.examples.Demo --stdio
```

Point any MCP-compatible client at that command and it will discover the four bundled tools (`calculator`, `echo`, `current_time`, `system_info`) plus a sample resource and prompt.

## 🛠️ Writing your own server

Exposing a tool is three lines of setup and one handler:

```java
import atlas.examples.tools.*;
import atlas.protocol.*;
import atlas.registry.Registry;
import atlas.server.McpServer;
import atlas.transport.StdioTransport;

public class MyServer {
    public static void main(String[] args) throws Exception {
        Registry registry = new Registry();
        registry.register(new Tool(
            "greet",
            "Greet someone by name",
            Schemas.object(Map.of("name", Schemas.string("who to greet")), List.of("name")),
            arguments -> List.of(Content.text("Hello, " + arguments.get("name") + "!"))));

        new McpServer(registry, "my-server", "1.0.0").serve(StdioTransport.system());
    }
}
```

That is a complete, client-connectable MCP server.

## 🏗️ Architecture

Atlas is a small stack of layers, each depending only on the one beneath it:

```
┌─────────────────────────────────────────────────────┐
│                   MCP Client                        │
│      (Claude Desktop · IDE plugin · custom agent)    │
└───────────────────────┬─────────────────────────────┘
                        │  stdio · newline-delimited JSON-RPC 2.0
┌───────────────────────▼─────────────────────────────┐
│                 Transport layer                     │
│      StdioTransport   ·   InMemoryTransport         │
└───────────────────────┬─────────────────────────────┘
                        │  read / write messages
┌───────────────────────▼─────────────────────────────┐
│               McpServer (dispatch)                  │
│  initialize · ping · tools/* · resources/* ·        │
│  prompts/*   →   JSON-RPC method routing            │
└───────────────────────┬─────────────────────────────┘
                        │  query / invoke
┌───────────────────────▼─────────────────────────────┐
│                   Registry                          │
│    tools   ·   resources   ·   prompts  (catalogue) │
└───────────────────────┬─────────────────────────────┘
                        │  backed by
┌───────────────────────▼─────────────────────────────┐
│             Protocol & JSON core                    │
│  Messages · Content · Tool · Resource · Prompt ·    │
│  Capabilities · Schemas · Json (dependency-free)    │
└─────────────────────────────────────────────────────┘
```

The **Transport** isolates I/O. The **McpServer** owns the request loop and never touches bytes directly — it consumes parsed `Messages.Inbound` and emits response maps. The **Registry** is the catalogue the dispatcher queries. The **core** types are plain data plus a hand-written JSON codec, so there is nothing between your tool and the wire but a couple of `Map` hops.

## 🔌 API Reference

| Class / Method | Responsibility |
| --- | --- |
| `McpServer.serve(Transport)` | Run the dispatch loop until the transport ends. |
| `McpServer.PROTOCOL_VERSION` | Protocol version advertised during `initialize`. |
| `Registry.register(Tool/Resource/Prompt)` | Register a capability. |
| `Registry.listTools() / getTool(name)` | Enumerate / look up tools. |
| `Tool(name, description, inputSchema, handler)` | Define a callable tool and its execution logic. |
| `ToolHandler.apply(args)` | Return `List<Content>`; may throw `ToolException`. |
| `Resource(uri, name, description, mimeType, supplier)` | Expose read-only, URI-addressable content. |
| `Prompt(name, description, args, template)` | Expose a reusable prompt template. |
| `Schemas.object/properties/string/number/integer/bool` | Build JSON-Schema input definitions. |
| `Content.text(String)` | Wrap text into a protocol content block. |
| `StdioTransport.system()` | Production transport bound to stdin/stdout. |
| `InMemoryTransport.feed/end` | Scripted transport for tests and demos. |
| `Json.parse / stringify / pretty` | Dependency-free JSON codec. |

## 🧭 Roadmap

- [ ] `logging/setLevel` and structured log notifications
- [ ] Optional `resources/subscribe` with change notifications
- [ ] A `StreamTransport` for remote (TCP/WebSocket) deployments
- [ ] Code generation from annotated Java methods (`@Tool`)
- [ ] A small reference client for end-to-end manual testing
- [ ] Binary (`image`) content support alongside `text`

## 🤝 Contributing

Pull requests are welcome. Because the project ships with no dependency tree, the contribution bar is low: keep it JDK-21-clean, add a unit test for any new behaviour, and run `javac` + the JUnit standalone launcher before opening a PR (see `.github/workflows/ci.yml` for the exact commands).

## 📄 License

Released under the [MIT License](LICENSE).

---

# 用 Java 手撸一个 MCP 服务器：从原理到落地（技术科普长文底稿）

> 下面的内容可以直接作为一篇发在掘金 / 知乎 / 小红书 / B 站的科普文底稿。它既讲了"为什么要有 MCP"，也讲了 Atlas 是怎么把协议落地成一堆可读的 Java 代码的，读者能照着把项目跑起来。

## 一、解决了什么问题

2025 年之后，"让大模型调用外部能力"几乎成了所有 AI 应用的标配。最早大家各写各的：A 产品用一套 JSON，B 产品用一套函数描述，模型侧每接一个工具都要重新适配。于是 **MCP（Model Context Protocol，模型上下文协议）** 出现了——它想把"模型 ↔ 工具/数据/提示词"这件事标准化，就像当年 USB 把外设接口统一了一样。

但现实里有个尴尬：很多 MCP SDK 为了"省事"，底层塞了一整套网络框架和 JSON 库，光依赖就要几十 MB。对只想暴露一两个工具的学生或后端同学来说，学习成本和引入风险都不小。**Atlas 想证明的另一件事是：协议本身其实很轻。** 它本质就是"在 stdio 上按行收发 JSON-RPC 2.0 消息"，剩下的都是结构化的胶水代码。

## 二、核心原理

MCP 的 stdio 模式可以浓缩成三句话：

1. **客户端把服务器当子进程拉起来**，往它的 stdin 写请求，从 stdout 读响应，每行一条 JSON。
2. 消息分两类：**请求（带 id，必须有回应）** 和 **通知（不带 id，发了就走，不发回应）**。
3. 握手的 `initialize` 之后，模型就能 `tools/list` 看有哪些工具、`tools/call` 真正调用、`resources/read` 读数据、`prompts/get` 拿提示词模板。

Atlas 里 `McpServer.dispatch()` 就是一个大 `switch`：按 `method` 路由，工具调用会回到 `Registry` 里查名字、执行、把结果包成 `content` 数组返回；出错就标 `isError: true` 而不是整个进程崩掉。

## 三、几个关键实现点（可直接抄）

- **自己写 JSON 解析器**：不是炫技，而是为了零依赖。一个递归下降的 `parse()` + 一个 `stringify()`，覆盖对象/数组/字符串/数字/布尔/null 就够了。
- **传输层抽象成接口**：`Transport` 只管 `readMessage()/writeMessage()`。生产用 `StdioTransport`，测试用 `InMemoryTransport` 喂脚本——这样 28 个单元测试一个子进程都不用起。
- **工具用函数式接口封装**：`ToolHandler.apply(Map)` 返回 `List<Content>`，可以抛 `ToolException`。这样"算错除法"之类的是业务错误，归到 `isError`，不影响服务器稳定性。
- **能力协商（capabilities）**：服务器只声明自己真正实现了的能力，避免客户端去请求根本不存在的功能。

## 四、和同类方案的对比

| 维度 | 重型 SDK（Netty+Jackson 那类） | Atlas |
| --- | --- | --- |
| 依赖 | 几十 MB，一堆传递依赖 | 零依赖，纯 JDK 21 |
| 可读性 | 黑盒，难审计 | ~15 个文件，下午能读完 |
| 上手成本 | 要懂构建工具和框架 | `javac` 一条命令 |
| 安全边界 | 依赖多 = 攻击面大 | 代码你全看得到 |
| 适合场景 | 企业级复杂网关 | 学习 / 面试展示 / 轻量接入 |

## 五、怎么扩展成一个像样的项目

1. 把示例里的 `calculator` 换成你自己的业务工具（查数据库、调内部 API、读配置文件）。
2. 加一个 `StreamTransport` 就能让服务器跑在 TCP/WebSocket 上，变成远程 MCP。
3. 写个注解 `@Tool`，编译期或运行期扫一遍自动注册，体验就接近 Spring 那套了。
4. 接上真实 MCP 客户端（比如支持 MCP 的 IDE 插件），把"模型调用你的 Java 方法"跑通，面试时能现场演示。

## 六、如何把本文扩展成一篇发布稿

- **标题党方向**：《面试官问我 MCP 是什么，我当场用 200 行 Java 手写了一个》《别再被 SDK 绑架了：零依赖实现模型上下文协议》。
- **开头钩子**：用一个"大模型想查天气却调不了你的接口"的真实痛点切入。
- **配图**：画一张"客户端-传输层-分发器-注册表-核心类型"的分层图（本仓库 README 已有一版 ASCII 图，可截图）。
- **结尾**：放仓库链接 + "跑通后你能收获什么（面试谈资 / 理解协议本质）"，引导收藏。
- **平台差异**：小红书偏短句+表情包，知乎偏原理深度，B 站适合配一段录屏跑 Demo。

---

<p align="center">
  Built with JDK 21 and not much else.
</p>
