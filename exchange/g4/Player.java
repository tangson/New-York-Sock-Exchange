package exchange.g4;

import exchange.g4.edmonds.SockArrangementFinder;
import exchange.sim.Offer;
import exchange.sim.Request;
import exchange.sim.Sock;
import exchange.sim.Transaction;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Player extends exchange.sim.Player {
    /*
     * Inherited from exchange.sim.Player:
     * Random random   -       Random number generator, if you need it
     * 
     * Remark: you have to manually adjust the order of socks, to minimize the total embarrassment
     * the score is calculated based on your returned list of getSocks(). Simulator will pair up socks 0-1, 2-3, 4-5, etc.
     */
    private int id;
    private int[] id_offer;
    private double maxDistance, minDistance;
    private Sock[] socks;
    private double[][] centers;
    private int[] clusters;
    private boolean isTransaction;
    private int current;
    private double[] maxDist;

    @Override
    public void init(int id, int n, int p, int t, List<Sock> socks) {
        this.id = id;
        this.socks = (Sock[]) socks.toArray(new Sock[2 * n]);
        this.isTransaction = true;
        this.current = 0;
        this.maxDist = new double[8];
        this.id_offer = new int[8];
    }

    @Override
    public Offer makeOffer(List<Request> lastRequests, List<Transaction> lastTransactions) {
        /*
         * lastRequests.get(i)  -       Player i's request last round
         * lastTransactions     -       All completed transactions last round.
         */
        if (isTransaction == true) {
            current = 0;
            centers = new double[4][3];
            clusters = new int[socks.length];
            K_means(10);
            current = 1;
            isTransaction = false;
            chooseOffer();
            return new Offer(socks[id_offer[0]], socks[id_offer[1]]);
        }
        else {
            if (current == 0) {
                current = 1;
                return new Offer(socks[id_offer[0]], socks[id_offer[1]]);
            }
            else if (current == 1) {
                current = 2;
                return new Offer(socks[id_offer[2]], socks[id_offer[3]]);
            }
            else if (current == 2) {
                current = 3;
                return new Offer(socks[id_offer[4]], socks[id_offer[5]]);
            }
            else {
                current = 0;
                return new Offer(socks[id_offer[6]], socks[id_offer[7]]);
            }
        }
    }

    @Override
    public Request requestExchange(List<Offer> offers) {
        /*
         * offers.get(i)	        - Player i's offer
         * For each offer:
         * offer.getSock(rank = 1, 2)	- get rank's offer
         * offer.getFirst()		- equivalent to offer.getSock(1)
         * offer.getSecond()		- equivalent to offer.getSock(2)
         *
         * Remark: For Request object, rank ranges between 1 and 2
         */

        List<Integer> availableOffers = new ArrayList<>();
        for (int i = 0; i < offers.size(); i++) {
            if (i == id) continue;

            // Encoding the offer information into integer: id * 2 + rank - 1
            if (offers.get(i).getFirst() != null)
                availableOffers.add(i * 2);
            if (offers.get(i).getSecond() != null)
                availableOffers.add(i * 2 + 1);
        }

        if (availableOffers.size() == 0)
            return new Request(-1, -1, -1, -1);

        int[] expect;
        expect = new int[6];
        if (current == 0) {
            int j = 0;
            for (int i = 0; i < 6; i++) {
                expect[i] = chooseRequest(availableOffers, offers, id_offer[j], j);
                j++;
            }
        }
        else if (current == 1) {
            int j = 2;
            for (int i = 0; i < 6; i++) {
                expect[i] = chooseRequest(availableOffers, offers, id_offer[j], j);
                j++;
            }
        }
        else if (current == 2) {
            int j = 0;
            for (int i = 0; i < 6; i++) {
                if (j == 2) {
                    j = j + 2;
                }
                expect[i] = chooseRequest(availableOffers, offers, id_offer[j], j);
                j++;
            }
        }
        else {
            int j = 0;
            for (int i = 0; i < 6; i++) {
                if (j == 4) {
                    j = j + 2;
                }
                expect[i] = chooseRequest(availableOffers, offers, id_offer[j], j);
                j++;
            }
        }
        for (int i = 0; i < 4; i++) {
            for (int j = i + 1; j < 5; j++) {
                if (expect[i] < expect[j]) {
                    int temp = expect[i];
                    expect[i] = expect[j];
                    expect[j] = temp;
                }
            }
        }

        if (expect[0] == -1) {
            if (expect[1] == -1) {
                return new Request(-1, -1, -1, -1);
            } else {
                return new Request(expect[1] / 2, expect[1] % 2 + 1, -1, -1);
            }
        } else {
            if (expect[1] == -1) {
                return new Request(expect[0] / 2, expect[0] % 2 + 1, -1, -1);
            } else {
                return new Request(expect[0] / 2, expect[0] % 2 + 1, expect[1] / 2, expect[1] % 2 + 1);
            }
        }
    }

    @Override
    public void completeTransaction(Transaction transaction) {
        /*
         * transaction.getFirstID()        -       first player ID of the transaction
         * transaction.getSecondID()       -       Similar as above
         * transaction.getFirstRank()      -       Rank of the socks for first player
         * transaction.getSecondRank()     -       Similar as above
         * transaction.getFirstSock()      -       Sock offered by the first player
         * transaction.getSecondSock()     -       Similar as above
         *
         * Remark: rank ranges between 1 and 2
         */
        int rank;
        Sock newSock;
        if (transaction.getFirstID() == id) {
            rank = transaction.getFirstRank();
            newSock = transaction.getSecondSock();
        }
        else {
            rank = transaction.getSecondRank();
            newSock = transaction.getFirstSock();
        }
        if (current == 0) {
            if (rank == 1) socks[id_offer[6]] = newSock;
            else socks[id_offer[7]] = newSock;
        }
        else if (current == 1) {
            if (rank == 1) socks[id_offer[0]] = newSock;
            else socks[id_offer[1]] = newSock;
        }
        else if (current == 2) {
            if (rank == 1) socks[id_offer[2]] = newSock;
            else socks[id_offer[3]] = newSock;
        }
        else {
            if (rank == 1) socks[id_offer[4]] = newSock;
            else socks[id_offer[5]] = newSock;
        }
        isTransaction = true;
    }

    @Override
    public List<Sock> getSocks() {
        ArrayList<Sock> s = new ArrayList(Arrays.asList(this.socks));
        ArrayList<Sock> ans = null;
        if (socks.length > 200) {
            ans = SockHelper.getSocks(s);
        }
        else {
            ans = SockArrangementFinder.getSocks(s);
        }

        int minPrice = 0;
        for (int i = 0; i < ans.size() - 1; i += 2) {
            Sock s1 = ans.get(i);
            Sock s2 = ans.get(i + 1);
            Double dist = s1.distance(s2);
            minPrice += dist.intValue();
        }
        return ans;
    }

    public void K_means(int rounds) {
        centers[0][0] = 64; centers[0][1] = 64; centers[0][2] = 64;
        centers[1][0] = 64; centers[1][1] = 192; centers[1][2] = 192;
        centers[2][0] = 192; centers[2][1] = 192; centers[2][2] = 64;
        centers[3][0] = 192; centers[3][1] = 64; centers[3][2] = 192;
        for (int k = 0; k < rounds; k++) {
            for (int i = 0; i < socks.length; i++) {
                double min = 1e9;
                for (int j = 0; j < 4; j++) {
                    double dist = Math.sqrt(Math.pow(socks[i].R - centers[j][0], 2) +
                            Math.pow(socks[i].G - centers[j][1], 2) + Math.pow(socks[i].B - centers[j][2], 2));
                    if (dist < min) {
                        min = dist;
                        clusters[i] = j;
                    }
                }
            }
            double[][] new_centers;
            int[] count;
            new_centers = new double[4][3];
            count = new int[4];
            for (int i = 0; i < 4; i++) {
                for (int j = 0; j < 3; j++) {
                    new_centers[i][j] = 0;
                }
                count[i] = 0;
            }
            for (int i = 0; i < socks.length; i++) {
                int t = clusters[i];
                count[t]++;
                new_centers[t][0] += socks[i].R;
                new_centers[t][1] += socks[i].G;
                new_centers[t][2] += socks[i].B;
            }
            for (int i = 0; i < 4; i++) {
                for (int j = 0; j < 3; j++) {
                    centers[i][j] = new_centers[i][j] / count[i];
                }
            }
        }
    }

    public void chooseOffer() {
        double[] maxFirst;
        double[] maxSecond;
        double[] maxThird;
        int[] markSecond;
        int[] markThird;
        maxFirst = new double[4];
        maxSecond = new double[4];
        maxThird = new double[4];
        markSecond = new int[4];
        markThird = new int[4];
        for (int i = 0; i < socks.length; i++) {
            int j = clusters[i];
            double dist = Math.sqrt(Math.pow(socks[i].R - centers[j][0], 2) +
                    Math.pow(socks[i].G - centers[j][1], 2) + Math.pow(socks[i].B - centers[j][2], 2));
            if (dist > maxFirst[j]) {
                maxFirst[j] = dist;
            }
        }
        for (int i = 0; i < socks.length; i++) {
            int j = clusters[i];
            double dist = Math.sqrt(Math.pow(socks[i].R - centers[j][0], 2) +
                    Math.pow(socks[i].G - centers[j][1], 2) + Math.pow(socks[i].B - centers[j][2], 2));
            if ((dist > maxSecond[j]) && (dist < maxFirst[j])) {
                maxSecond[j] = dist;
                markSecond[j] = i;
            }
        }
        for (int i = 0; i < socks.length; i++) {
            int j = clusters[i];
            double dist = Math.sqrt(Math.pow(socks[i].R - centers[j][0], 2) +
                    Math.pow(socks[i].G - centers[j][1], 2) + Math.pow(socks[i].B - centers[j][2], 2));
            if ((dist > maxThird[j]) && (dist < maxSecond[j])) {
                maxThird[j] = dist;
                markThird[j] = i;
            }
        }
        id_offer[0] = markSecond[0]; id_offer[1] = markSecond[1];
        id_offer[2] = markSecond[2]; id_offer[3] = markSecond[3];
        id_offer[4] = markThird[0]; id_offer[5] = markThird[1];
        id_offer[6] = markThird[2]; id_offer[7] = markThird[3];
        maxDist[0] = maxSecond[0]; maxDist[1] = maxSecond[1];
        maxDist[2] = maxSecond[2]; maxDist[3] = maxSecond[3];
        maxDist[4] = maxThird[0]; maxDist[5] = maxThird[1];
        maxDist[6] = maxThird[2]; maxDist[7] = maxThird[3];
    }

    public int chooseRequest(List<Integer> availableOffers, List<Offer> offers, int identity, int num) {
        int n = availableOffers.size();
        double[] dist;
        double min = 1e9;
        int mark = 0;
        for (int i = 0; i < n; i++) {
            int k = availableOffers.get(i);
            int k1 = k / 2;
            int k2 = k % 2 + 1;
            Sock sock0;
            if (k2 == 1) {
                sock0 = offers.get(k1).getFirst();
            } else {
                sock0 = offers.get(k1).getSecond();
            }
            double temp = sock0.distance(socks[identity]);
            if (temp < min) {
                min = temp;
                mark = k;
            }
        }
        if (min < maxDist[num]) {
            return mark;
        } else return -1;
    }
}
