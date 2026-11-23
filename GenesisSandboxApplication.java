import com.genesis.sandbox.grpc.SandboxGatewayGrpc;
import io.grpc.Server;
import io.grpc.ServerBuilder;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * 创世纪沙盘 - 服务端主启动类
 */
public class GenesisSandboxApplication {

    private Server grpcServer;
    private GameTickEngine tickEngine;
    private SandboxNetwork network;
    private SandboxGatewayImpl gateway;

    /**
     * 1. 组装并初始化整个世界的物理法则和拓扑
     */
    private void initializeWorld() {
        System.out.println(">>> 正在初始化沙盘网络拓扑...");
        network = new SandboxNetwork();

        // 构造一个具有测试意义的小型连通图
        // 参数：NodeID, 初始算力, 每Tick产出
        ComputeNode nodeAlpha = new ComputeNode("Node_Alpha", 500, 10);
        ComputeNode nodeBeta = new ComputeNode("Node_Beta", 200, 5);
        ComputeNode nodeCore = new ComputeNode("Node_Core", 1000, 50); // 高算力核心节点

        network.addNode(nodeAlpha);
        network.addNode(nodeBeta);
        network.addNode(nodeCore);

        // 建立连接路由及其代价 (Edge / Cost)
        network.addEdge("Node_Alpha", "Node_Core", 1);
        network.addEdge("Node_Beta", "Node_Core", 2);

        // 实例化网关与引擎
        gateway = new SandboxGatewayImpl();
        tickEngine = new GameTickEngine(network, gateway);
    }

    /**
     * 2. 启动 gRPC 网络服务器与时钟引擎
     */
    private void start() throws IOException {
        int port = 50051; // gRPC 默认/常用测试端口
        
        // 启动 gRPC Server
        grpcServer = ServerBuilder.forPort(port)
                .addService(gateway) // 注册我们刚才写的网关服务
                .build()
                .start();
        
        System.out.println(">>> gRPC Server 启动成功，监听端口: " + port);

        // 启动世界时钟
        tickEngine.startWorld();

        // 3. 注册 JVM 关闭钩子 (Graceful Shutdown)
        // 确保当用户在控制台按 Ctrl+C 或者终止进程时，服务器能安全释放资源，而不是直接拔电源
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.err.println("*** 收到系统关闭信号，正在执行优雅停机 ***");
            try {
                GenesisSandboxApplication.this.stop();
            } catch (InterruptedException e) {
                e.printStackTrace(System.err);
            }
            System.err.println("*** 服务器已安全关闭 ***");
        }));
    }

    /**
     * 优雅停机逻辑
     */
    private void stop() throws InterruptedException {
        if (grpcServer != null) {
            // 给现有的网络请求最多 30 秒的处理时间，拒绝新请求
            grpcServer.shutdown().awaitTermination(30, TimeUnit.SECONDS);
        }
    }

    /**
     * 阻塞主线程，保持服务器持续运行
     */
    private void blockUntilShutdown() throws InterruptedException {
        if (grpcServer != null) {
            // gRPC Server 启动后是守护进程，如果不阻塞主线程，程序会立刻退出
            grpcServer.awaitTermination();
        }
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        GenesisSandboxApplication app = new GenesisSandboxApplication();
        app.initializeWorld();
        app.start();
        app.blockUntilShutdown();
    }
}