import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;

/* CompliantNode refers to a node that follows the rules (not malicious)*/
public class CompliantNode implements Node {

    private boolean[] followees;
    private Set<Transaction> pendingTransactions;
    private ArrayList<Integer[]> candidates;

    public CompliantNode(double p_graph, double p_malicious, double p_txDistribution, int numRounds) {
         candidates = new ArrayList<Integer[]>();
    }

    public void setFollowees(boolean[] followees) {
        this.followees = new boolean[followees.length];

        for (int i = 0; i < followees.length; i++) {
            this.followees[i] = followees[i];
        }

        return;
    }

    public void setPendingTransaction(Set<Transaction> pendingTransactions) {
        this.pendingTransactions = new HashSet<Transaction>();

        for (Transaction tx : pendingTransactions) {
            this.pendingTransactions.add(tx);
        }
    }

    public Set<Transaction> sendToFollowers() {
        Set<Transaction> res = new HashSet<Transaction>();
      
        for (Transaction tx : pendingTransactions) {
            res.add(tx);
        }

        return res;
    }

    public void receiveFromFollowees(Set<Candidate> candidates) {
        for (Candidate candidate : candidates) {
            pendingTransactions.add(candidate.tx);
        }

        return;
    }
}
