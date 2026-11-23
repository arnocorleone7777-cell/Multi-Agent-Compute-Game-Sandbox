import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

public class ComputeNode {
    private final String nodeId;
    private AtomicInteger currentCompute; 
    private volatile String ownerAgentId; 
    private final int generationRate; 
    
    private final ReentrantLock nodeLock = new ReentrantLock();
    private Map<ComputeNode, Integer> connectedNodes;

    public ComputeNode(String nodeId, int initialCompute, int generationRate) {
        this.nodeId = nodeId;
        this.currentCompute = new AtomicInteger(initialCompute);
        this.generationRate = generationRate;
        this.ownerAgentId = "UNOWNED"; 
        this.connectedNodes = new ConcurrentHashMap<>();
    }

    public void generateTickResource() {
        if (!"UNOWNED".equals(ownerAgentId)) {
            currentCompute.addAndGet(generationRate);
        }
    }

    public boolean tryLockNode() {
        return nodeLock.tryLock();
    }

    public void unlockNode() {
        nodeLock.unlock();
    }

    public String getNodeId() { return nodeId; }
    public int getCurrentCompute() { return currentCompute.get(); }
    public void setCurrentCompute(int compute) { this.currentCompute.set(compute); }
    public String getOwnerAgentId() { return ownerAgentId; }
    public void setOwnerAgentId(String ownerAgentId) { this.ownerAgentId = ownerAgentId; }
    
    public void addConnection(ComputeNode node, int cost) {
        this.connectedNodes.put(node, cost);
    }
}