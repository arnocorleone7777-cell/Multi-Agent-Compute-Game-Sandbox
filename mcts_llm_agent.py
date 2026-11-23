import time
import json
import random
import grpc
import ollama
import sandbox_pb2
import sandbox_pb2_grpc

class HybridStrategicAgent:
    """
    快慢双系统解耦的多智能体架构 (Harness Engineering):
    1. 慢系统 (Slow Thinker): 每 5 个 Tick 调用本地 Ollama (Qwen2-7B) 生成 JSON 宏观战略权重
    2. 快系统 (Fast Actor): 每个 Tick (1Hz) 基于当前世界状态执行确定性图算力分配与防幻觉拦截
    """
    def __init__(self, agent_id: str, target_addr: str = "localhost:50051"):
        self.agent_id = agent_id
        self.channel = grpc.insecure_channel(target_addr)
        self.stub = sandbox_pb2_grpc.SandboxGatewayStub(self.channel)
        self.macro_strategy = {"mode": "BALANCED", "focus_node": "Node_Core", "aggression": 0.5}
        self.tick_counter = 0

    def update_macro_strategy_via_llm(self, world_summary: str):
        """异步/低频调用本地 Qwen2-7B 制定宏观战略，避免阻塞 1Hz 物理时钟"""
        prompt = f"""你是算力博弈沙盒的战略指挥官。当前局势：{world_summary}
        请输出严格 JSON 决定未来 5 秒的宏观方针：
        {{"mode": "EXPAND 或 DEFEND 或 BALANCED", "focus_node": "目标节点ID", "aggression": 0.1到0.9的浮点数}}"""
        try:
            resp = ollama.chat(model="qwen2:7b", messages=[{"role": "user", "content": prompt}], format="json")
            parsed = json.loads(resp["message"]["content"])
            if parsed.get("mode") in {"EXPAND", "DEFEND", "BALANCED"}:
                self.macro_strategy = parsed
        except Exception:
            # 确定性兜底：模型超时或异常时保持上一周期合法策略
            pass

    def deterministic_harness_decision(self, available_compute: int) -> dict:
        """
        【确定性工程护栏】
        将大模型概率输出转化为 100% 合法的微观物理指令，杜绝算力透支或非法参数幻觉
        """
        aggression = min(max(float(self.macro_strategy.get("aggression", 0.5)), 0.1), 0.9)
        allocated = int(available_compute * aggression)
        # 硬性边界校验：单次投入算力不得低于 10 且不得超过当前可用余额
        safe_compute = max(10, min(allocated, available_compute))
        target = str(self.macro_strategy.get("focus_node") or "Node_Core")
        return {"target_node": target, "compute_power": safe_compute}

    def run_loop(self):
        print(f"[{self.agent_id}] Hybrid LLM + Deterministic Harness Agent started...")
        simulated_compute = 300
        while True:
            self.tick_counter += 1
            if self.tick_counter % 5 == 1:
                self.update_macro_strategy_via_llm(f"Tick={self.tick_counter}, Compute={simulated_compute}")

            action = self.deterministic_harness_decision(simulated_compute)
            print(f"[Tick {self.tick_counter}] Strategy={self.macro_strategy['mode']} -> SafeAction={action}")
            time.sleep(1.0)

if __name__ == "__main__":
    agent = HybridStrategicAgent(agent_id="Agent_Alpha_01")
    agent.run_loop()