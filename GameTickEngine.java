import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import com.genesis.sandbox.grpc.WorldState;
import com.genesis.sandbox.grpc.NodeState;
import com.genesis.sandbox.grpc.AgentAction;
import io.grpc.stub.StreamObserver;

public class GameTickEngine {
    private final ScheduledExecutorService ticker = Executors.newSingleThreadScheduledExecutor();
    private final SandboxNetwork network;
    private final SandboxGatewayImpl gateway; 
    private final CombatResolutionService combatService = new CombatResolutionService();
    private int currentTick = 0;

    public GameTickEngine(SandboxNetwork network, SandboxGatewayImpl gateway) {
        this.network = network;
        this.gateway = gateway;
    }

    public void startWorld() {
        System.out.println(">>> 创世纪沙盘引擎启动中...");
        ticker.scheduleAtFixedRate(this::processTick, 0, 1000, TimeUnit.MILLISECONDS);
    }

    private void processTick() {
        try {
            currentTick++;
            System.out.println("\n[世界脉冲] Tick: " + currentTick);
            generateResources();
            resolveActions();
            broadcastState();
        } catch (Exception e) {
            System.err.println("Tick 发生异常: " + e.getMessage());
        }
    }

    private void generateResources() {
        for (ComputeNode node : network.getAllNodes()) {
            node.generateTickResource();
        }
        System.out.println("阶段1: 资源刷新完成");
    }

    private void resolveActions() {
        System.out.println("阶段2: 正在处理 Agent 决策...");
        var actionQueue = gateway.getActionQueue();
        
        while (!actionQueue.isEmpty()) {
            AgentAction action = actionQueue.poll(); 
            if (action == null) break;

            String agentId = action.getAgentId();
            String targetNodeId = action.getTargetNodeId();
            int investCompute = action.getInvestCompute();
            
            switch (action.getActionType()) {
                case HACK:
                    Agent attacker = new Agent(agentId, 1000); 
                    ComputeNode targetNode = network.getNode(targetNodeId);
                    if (targetNode != null) {
                        combatService.resolveHack(attacker, targetNode, investCompute);
                    }
                    break;
                case MINE:
                case MOVE:
                default:
                    System.out.println("收到其他行动指令");
                    break;
            }
        }
        System.out.println("阶段2: 当前 Tick 所有指令结算完毕.");
    }

    private void broadcastState() {
        var observers = gateway.getClientResponseObservers();
        if (observers.isEmpty()) {
            return; 
        }

        WorldState.Builder stateBuilder = WorldState.newBuilder()
                .setCurrentTick(currentTick);

        for (ComputeNode node : network.getAllNodes()) {
            stateBuilder.addNodes(NodeState.newBuilder()
                    .setNodeId(node.getNodeId())
                    .setCurrentCompute(node.getCurrentCompute())
                    .setOwnerAgentId(node.getOwnerAgentId())
                    .build());
        }

        WorldState finalState = stateBuilder.build();

        for (StreamObserver<WorldState> observer : observers) {
            try {
                observer.onNext(finalState); 
            } catch (Exception e) {
                System.err.println("向某个客户端推送状态失败，移除该连接");
                observers.remove(observer);
            }
        }
        System.out.println("阶段3: 全局数据已广播");
    }
}