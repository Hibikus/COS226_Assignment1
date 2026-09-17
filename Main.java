public class Main
{
    public static void main(String[] args) throws InterruptedException
    {
        String lockChoice   = args.length > 0 ? args[0].toLowerCase() : "all";
        int numberOfThreads = args.length > 1 ? Integer.parseInt(args[1]) : 4;
        int iterations      = args.length > 2 ? Integer.parseInt(args[2]) : 200;

        String[] locks = lockChoice.equals("all")
                ? new String[] { "ttas", "clh", "mcs" }
                : new String[] { lockChoice };

        for (String name : locks)
        {
            // Fresh auction and fresh lock for every run so runs don't affect each other.
            Auction auction = new Auction(AuctionUtils.generateItemName());
            Lock lock = createLock(name);
            Runner runner = new Runner(numberOfThreads, iterations, auction, lock);
            runner.run();
        }
    }

    static Lock createLock(String name)
    {
        switch (name)
        {
            case "ttas": return new TTASLock();
            case "clh":  return new CLHLock();
            case "mcs":  return new MCSLock();
            default: throw new IllegalArgumentException("Unknown lock: " + name + " (use ttas, clh, mcs or all)");
        }
    }
}