import grpc
import time
import sandbox_pb2
import sandbox_pb2_grpc

def generate_dummy_actions():
    """
    Python 端的生成器 (Generator)
    gRPC 双向流要求传入一个可迭代对象，yield 关键字非常适合这种持续发送指令的场景。
    """
    agent_id = "Agent_Alpha"
    print(f"[{agent_id}] 哑巴指令生成器已启动...")
    
    while True:
        time.sleep(3) # 每 3 秒瞎挥一拳
        
        # 构造发给 Java 的行动指令
        action = sandbox_pb2.AgentAction(
            agent_id=agent_id,
            action_type=sandbox_pb2.HACK,      # 发起攻击
            target_node_id="Node_Core",        # 攻击我们初始化的核心节点
            invest_compute=150                 # 投入 150 兵力
        )
        
        print(f"\n[Python 发送 ->] 尝试 Hack Node_Core，投入算力: 150")
        yield action

def run_verification():
    # 1. 建立与 Java 世界引擎的通道
    print("正在连接到 Java 创世纪引擎 (localhost:50051)...")
    channel = grpc.insecure_channel('localhost:50051')
    stub = sandbox_pb2_grpc.SandboxGatewayStub(channel)
    print("连接成功！等待接收世界状态...")

    try:
        # 2. 调用双向流接口 (SyncStream)
        # 传入我们的 generate_dummy_actions() 生成器作为入参流
        # 返回的 responses 是一个迭代器，里面装满了 Java 发来的 WorldState
        responses = stub.SyncStream(generate_dummy_actions())

        # 3. 持续监听 Java 的状态广播
        for state in responses:
            print(f"\n[<- Java 推送] 收到 Tick #{state.current_tick}")
            for node in state.nodes:
                print(f"  - 节点: {node.node_id:10} | 算力: {node.current_compute:4} | 归属: {node.owner_agent_id}")
                
    except grpc.RpcError as e:
        print(f"\n通信中断或发生错误: {e}")
    except KeyboardInterrupt:
        print("\n客户端手动退出")

if __name__ == '__main__':
    run_verification()