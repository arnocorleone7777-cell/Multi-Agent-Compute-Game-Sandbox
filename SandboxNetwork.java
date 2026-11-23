import java.util.concurrent.ConcurrentHashMap;
import java.util.Collection;

public class SandboxNetwork {
    // 放弃普通的 HashMap，使用 ConcurrentHashMap 支持高频读写
    private final ConcurrentHashMap<String, ComputeNode> allNodes;

    public SandboxNetwork() {
        this.allNodes = new ConcurrentHashMap<>();
    }

    // 向世界中注入新节点
    public void addNode(ComputeNode node) {
        allNodes.put(node.getNodeId(), node);
    }

    // 建立拓扑网络的双向连线
    public void addEdge(String nodeIdA, String nodeIdB, int routingCost) {
        ComputeNode nodeA = allNodes.get(nodeIdA);
        ComputeNode nodeB = allNodes.get(nodeIdB);
        
        if (nodeA != null && nodeB != null) {
            nodeA.addConnection(nodeB, routingCost);
            nodeB.addConnection(nodeA, routingCost);
        }
    }

    // 获取所有节点（用于 Tick 遍历）
    public Collection<ComputeNode> getAllNodes() {
        return allNodes.values();
    }
    
    public ComputeNode getNode(String nodeId) {
        return allNodes.get(nodeId);
    }
}
