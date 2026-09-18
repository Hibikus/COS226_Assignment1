import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;
/*Optional Helper Runner Class*/
public class Runner 
{

    public final int numberOfThreads;
    public final int iterations;
    public final Auction auction;
    public final Lock lock;

    private final AtomicLong totalWaitingTime = new AtomicLong(0);

    // added variables to store results
    private long totalBids = 0;
    private final long[] bidsWon; //i only written by bidder i
    private final long[] leadTakeovers;
    private final long[] incrementSum;

    public Runner(int numberOfThreads,int iterations,Auction auction,Lock lock) 
    {
        this.numberOfThreads = numberOfThreads;
        this.iterations = iterations;
        this.auction = auction;
        this.lock = lock;

        this.bidsWon = new long[numberOfThreads];
        this.leadTakeovers = new long[numberOfThreads];
        this.incrementSum = new long[numberOfThreads];
    }

    public void run() throws InterruptedException 
    {
        Thread[] threads = new Thread[numberOfThreads];

        for(int i = 0; i < numberOfThreads; i++) 
        {
            final int bidderId = i;

            threads[i] = new Thread(() -> {
                bidder(bidderId);
            });
        }

        long startTime = System.nanoTime();

        for(Thread thread : threads) 
        {
            thread.start();
        }

        for(Thread thread : threads) 
        {
            thread.join();
        }

        long endTime = System.nanoTime();

        reportResults(endTime - startTime);
    }

    /*Defines the behaviour of an individual bidder. Note you have to decide how to incorporate your lock.*/
    public void bidder(int bidderId) 
    {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        long won = 0, takeovers = 0, incSum = 0;

        for (int i = 0; i < iterations; i++)
        {
            int increment = random.nextInt(1, 101);   // private work: outside the lock

            long requestTime = System.nanoTime();
            lock.lock();
            try
            {
                totalWaitingTime.addAndGet(System.nanoTime() - requestTime);

                double current = auction.getHighestBid();        // 1. read
                int previousLeader = auction.getHighestBidder();
                double newBid = current + increment;              // 2. decide
                auction.placeBid(bidderId, newBid);               // 3. write

                if (auction.getHighestBidder() == bidderId && auction.getHighestBid() == newBid)
                {
                    totalBids++;
                    won++;
                    incSum += increment;
                    if (previousLeader != bidderId) takeovers++;
                }
            }
            finally
            {
                lock.unlock();
            }
        }

        bidsWon[bidderId] = won;
        leadTakeovers[bidderId] = takeovers;
        incrementSum[bidderId] = incSum;
    }

    /*Optional Helper: Records and reports the results of the experiment.*/
    public void reportResults(long executionTime) 
    {
        long expectedBids = (long) numberOfThreads * iterations;
        long expectedFinal = 0;
        for (long s : incrementSum) expectedFinal += s;

        System.out.println("==================================================");
        System.out.println("Lock: " + lock.getClass().getSimpleName());
        System.out.println("Item: " + auction.getItemName());
        System.out.println("Threads: " + numberOfThreads + " Iterations: " + iterations);
        System.out.printf ("Execution time: %.3f ms%n", executionTime / 1_000_000.0);
        System.out.println("Total bids:     " + totalBids + " (expected " + expectedBids + ")"
                + (totalBids == expectedBids ? " OK" : " MISMATCH"));
        System.out.printf ("Final high bid: %.0f (expected %d)%s%n", auction.getHighestBid(), expectedFinal,
                auction.getHighestBid() == expectedFinal ? " OK" : " MISMATCH");
        System.out.printf ("Avg wait:       %.3f us%n",
                totalBids == 0 ? 0 : totalWaitingTime.get() / (double) totalBids / 1000.0);
        System.out.printf("%-8s %-8s %-10s%n", "Bidder", "Won", "Takeovers");
        for (int i = 0; i < numberOfThreads; i++)
        {
            System.out.printf("%-8d %-8d %-10d%n", i, bidsWon[i], leadTakeovers[i]);
        }
    }
}
