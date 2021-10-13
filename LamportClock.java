public class LamportClock {
    
    int latestTime;

    public LamportClock(int timestamp) {
        this.latestTime = timestamp;
    }

    public int tick(int requestTime) {
        this.latestTime = Integer.max(latestTime, requestTime);
        this.latestTime++;
        return latestTime;
    }

    public void increment_clock() {
        this.latestTime++;
    }

    public int get_LamportClock() {
        return this.latestTime;
    }
}