package pcp.com.bttemperature.database;

public class ConditionObject {
    private final double humidity;
    private final long light;
    private final double tempC;
    private final long timestamp;

    public ConditionObject(long passedTimestamp, double passedTempC, double passedHumidity, long passedLight) {
        this.tempC = passedTempC;
        this.humidity = passedHumidity;
        this.timestamp = passedTimestamp;
        this.light = passedLight;
    }

    public long getTimestamp() {
        return this.timestamp;
    }

    public double getTempC() {
        return this.tempC;
    }

    public double getTempF() {
        return convertCtoF(this.tempC);
    }

    public double getHumidity() {
        return this.humidity;
    }

    public long getLight() {
        return this.light;
    }

    private static double convertCtoF(double temperature) {
        return (1.8d * temperature) + 32.0d;
    }
}
