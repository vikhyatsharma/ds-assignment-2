// As Lamport clock are going to be used everywhere we will define them in seperate class

public class LamportClock {
    
    int latestTime; // Main variable which will store the time

    public LamportClock(int timestamp) { // Constructor
        this.latestTime = timestamp;
    }

    public int tick(int requestTime) { // Updating the value
        this.latestTime = Integer.max(latestTime, requestTime);
        this.latestTime++;
        return latestTime;
    }

    public void increment_clock() { // Increment the lamport clock by one
        this.latestTime++;
    }

    public int get_LamportClock() { // To get the result of lamport clock
        return this.latestTime;
    }
}