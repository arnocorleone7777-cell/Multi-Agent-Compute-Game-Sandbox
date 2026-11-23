import java.util.concurrent.atomic.AtomicInteger;

public class Agent {
    private final String agentId;
    private final AtomicInteger currentCompute;

    public Agent(String agentId, int initialCompute) {
        this.agentId = agentId;
        this.currentCompute = new AtomicInteger(initialCompute);
    }

    public String getAgentId() { return agentId; }
    public int getCompute() { return currentCompute.get(); }
    public void deductCompute(int amount) { currentCompute.addAndGet(-amount); }
    public void addCompute(int amount) { currentCompute.addAndGet(amount); }
}