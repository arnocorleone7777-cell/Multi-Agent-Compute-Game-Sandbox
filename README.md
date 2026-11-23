# Multi-Agent Compute Game Sandbox | 多智能体算力博弈演进沙盒引擎

> 基于 **Java (Spring Boot / ConcurrentLinkedQueue)** 高并发时钟引擎、**gRPC & Protobuf** 跨语言二进制双向流通信，以及 **Python (Ollama LLM + 确定性护栏)** 构建的多智能体协同演进沙盒。

## 1. 系统架构与核心设计 (Architecture)

```mermaid
graph LR
    A[Java 核心世界引擎<br/>GameTickEngine 1Hz] <-->|gRPC HTTP/2 双向流<br/>Protobuf 二进制契约| B[Python 异构网关<br/>sandbox_pb2_grpc]
    B --> C[慢系统: 本地 Ollama Qwen2-7B<br/>低频宏观战略制定]
    B --> D[快系统: 确定性护栏与图推演<br/>1Hz 毫秒级动作生成]
    C -.->|JSON 战略权重| D
    D -->|100% 合法指令校验| B

    跨语言高并发物理底座 (Java)： 针对纯 Python 仿真环境在状态推演中的 GIL 性能瓶颈，采用 Java 构建 GameTickEngine 核心世界引擎。网络层引入无锁并发队列 ConcurrentLinkedQueue 接收多智能体并发请求，结合 CAS 原子操作完成单 Tick $O(1)$ 级概率结算（单次结算耗时 < 0.01 ms）。低延迟二进制通信契约 (gRPC & Protobuf)： 通过 sandbox.proto 严格定义全局拓扑节点、算力资源与动作空间契约，依托 HTTP/2 多路复用实现 Localhost 环境下 0.5 ms – 2.0 ms 级低延迟状态同步。快慢双系统解耦与确定性护栏 (Harness Engineering)： 针对大语言模型在 1Hz 高频物理时钟下的推理延迟与数值幻觉问题，设计“本地 Qwen2-7B 宏观意图制定 + Python 端确定性边界校验与图推演兜底”的分层决策架构，保障 100% 合法指令交付率。

    2. 性能与承载边界 (Performance & Scalability)性能指标实测 / 架构推演基准核心技术支撑gRPC 双向流 I/O 延迟0.5 ms – 2.0 ms (Localhost 回环)HTTP/2 多路复用 + Protobuf 二进制序列化单 Tick 核心结算耗时< 0.01 ms / 指令ConcurrentLinkedQueue 无锁队列 + CAS 原子锁单节点理论并发上限10,000 – 20,000 Agents / 秒1000ms 时钟周期内无阻塞流水线吞吐3. 快速启动 (Quick Start)启动 Java 沙盒核心引擎Bashmvn clean compile exec:java
启动 Python 智能体客户端Bashpip install grpcio grpcio-tools ollama
python dummy_client.py        # 通信链路压测客户端
python mcts_llm_agent.py      # LLM 宏观策略 + 确定性护栏智能体
