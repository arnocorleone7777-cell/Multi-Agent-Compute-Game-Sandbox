import java.util.concurrent.ThreadLocalRandom;

public class CombatResolutionService {

    // 基础战损率：无论输赢，战争都会带来消耗（防止滚雪球效应）
    private static final double WINNER_ATTRITION_RATE = 0.10; // 赢家折损 10%
    private static final double LOSER_PENALTY_RATE = 0.50;    // 输家损失 50% 投入的兵力

    /**
     * 解析黑客攻击指令 (Hack Action)
     * @param attacker 发起攻击的 Agent
     * @param target 被攻击的算力节点
     * @param investCompute 攻击者决定投入多少算力进行这次攻击
     * @return true 如果攻击者获胜，否则 false
     */
    public boolean resolveHack(Agent attacker, ComputeNode target, int investCompute) {
        // 1. 基础校验：不能超过自身拥有的算力
        if (investCompute > attacker.getCompute()) {
            System.err.println("攻击失败：算力不足！");
            return false;
        }

        // 尝试获取目标节点的锁（极度重要：防止多个 Agent 同时攻击同一个节点）
        if (!target.tryLockNode()) {
            System.out.println("节点 " + target.getNodeId() + " 正在交火中，攻击被弹回！");
            return false; // 如果节点正在被别人打，本次攻击直接失效（或者可以选择排队等待）
        }

        try {
            int defenderCompute = target.getCurrentCompute();

            // 2. 核心数学模型：计算攻击胜率 (例如: 投入 300，防守 500，胜率 = 300/800 = 37.5%)
            double winProbability = (double) investCompute / (investCompute + defenderCompute);

            // 3. 工业级并发随机数：掷骰子 (生成 0.0000 到 0.9999 的随机数)
            double roll = ThreadLocalRandom.current().nextDouble();

            // 4. 命运时刻：判定胜负
            boolean attackerWins = roll < winProbability;

            // 5. 结算与战损计算
            if (attackerWins) {
                System.out.printf("[战报] %s 成功攻占节点 %s! (胜率: %.1f%%, Roll点: %.2f)\n", 
                        attacker.getAgentId(), target.getNodeId(), winProbability * 100, roll);
                
                // 赢家接管节点，但节点的剩余算力要扣除战损
                int remainingNodeCompute = (int) (defenderCompute * (1 - WINNER_ATTRITION_RATE));
                target.setOwnerAgentId(attacker.getAgentId());
                target.setCurrentCompute(remainingNodeCompute);
                
                // 攻击者收回投入的算力（这里可以设定夺取节点有额外奖励，暂略）
                
            } else {
                System.out.printf("[战报] %s 攻击节点 %s 失败! 被反向追踪. (胜率: %.1f%%, Roll点: %.2f)\n", 
                        attacker.getAgentId(), target.getNodeId(), winProbability * 100, roll);
                
                // 输家遭到惩罚，扣除投入算力的 50%
                int penalty = (int) (investCompute * LOSER_PENALTY_RATE);
                attacker.deductCompute(penalty);
                
                // 防守节点也因为抵抗消耗了一点算力
                target.setCurrentCompute((int) (defenderCompute * 0.95)); 
            }

            return attackerWins;

        } finally {
            // 6. 绝对防御：无论发生什么异常，必须释放锁，否则节点将永远瘫痪
            target.unlockNode();
        }
    }
}

