import com.genesis.sandbox.grpc.AgentAction;
import com.genesis.sandbox.grpc.SandboxGatewayGrpc;
import com.genesis.sandbox.grpc.WorldState;
import io.grpc.stub.StreamObserver;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * gRPC 网络网关实现类
 * 负责维持与 Python AI 端的双向流式通信
 */
public class SandboxGatewayImpl extends SandboxGatewayGrpc.SandboxGatewayImplBase {

    // 核心缓冲区：使用无锁的高并发队列，暂存所有来自 Python 端的指令
    // 这个队列是连接 gRPC 线程和 GameTickEngine 线程的桥梁
    private final ConcurrentLinkedQueue<AgentAction> actionQueue = new ConcurrentLinkedQueue<>();

    // 维持所有活跃的 Python 客户端的输出流，用于后续 Java 广播状态
    private final ConcurrentLinkedQueue<StreamObserver<WorldState>> clientResponseObservers = new ConcurrentLinkedQueue<>();

    /**
     * 实现我们在 proto 中定义的双向流式 RPC 方法
     */
    @Override
    public StreamObserver<AgentAction> syncStream(StreamObserver<WorldState> responseObserver) {
        
        // 1. 将前端或 Python 客户端的响应流记录下来，方便以后广播
        clientResponseObservers.add(responseObserver);

        // 2. 返回一个 StreamObserver，告诉 gRPC 当收到 Python 的数据时该怎么回调
        return new StreamObserver<AgentAction>() {
            
            @Override
            public void onNext(AgentAction action) {
                // 当 Python 端通过流发来一条指令时，触发此方法
                System.out.printf("[网络层] 收到 Agent [%s] 的指令: %s\n", 
                        action.getAgentId(), action.getActionType());
                
                // 生产：将指令安全地推入队列，网络线程的工作到此结束，极其高效
                actionQueue.offer(action);
            }

            @Override
            public void onError(Throwable t) {
                // 当 Python 端连接断开或发生异常时触发
                System.err.println("[网络层] Python 客户端连接异常断开: " + t.getMessage());
                clientResponseObservers.remove(responseObserver);
            }

            @Override
            public void onCompleted() {
                // 当 Python 端主动结束流时触发
                System.out.println("[网络层] Python 客户端主动结束通信");
                clientResponseObservers.remove(responseObserver);
                responseObserver.onCompleted();
            }
        };
    }

    // 提供给 GameTickEngine 调用的消费接口
    public ConcurrentLinkedQueue<AgentAction> getActionQueue() {
        return actionQueue;
    }

    public ConcurrentLinkedQueue<StreamObserver<WorldState>> getClientResponseObservers() {
        return clientResponseObservers;
    }
}

