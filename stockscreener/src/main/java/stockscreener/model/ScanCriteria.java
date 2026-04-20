package stockscreener.model;

public class ScanCriteria {

    private Double minPrice;
    private Double maxPrice;

    private Double minAvgVolume;
    private Double minOptionsVolume;

    public ScanCriteria() {
        // Default institutional filters
        this.minPrice = 1.0;                 // $1 minimum
        this.maxPrice = 2000.0;              // $2000 maximum
        this.minAvgVolume = 2_000_000.0;     // > 2M avg volume
        this.minOptionsVolume = 10_000.0;    // > 10k options volume
    }

    public Double getMinPrice() {
        return minPrice;
    }

    public void setMinPrice(Double minPrice) {
        this.minPrice = minPrice;
    }

    public Double getMaxPrice() {
        return maxPrice;
    }

    public void setMaxPrice(Double maxPrice) {
        this.maxPrice = maxPrice;
    }

    public Double getMinAvgVolume() {
        return minAvgVolume;
    }

    public void setMinAvgVolume(Double minAvgVolume) {
        this.minAvgVolume = minAvgVolume;
    }

    public Double getMinOptionsVolume() {
        return minOptionsVolume;
    }

    public void setMinOptionsVolume(Double minOptionsVolume) {
        this.minOptionsVolume = minOptionsVolume;
    }
}
