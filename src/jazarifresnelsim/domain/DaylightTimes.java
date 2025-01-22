
package jazarifresnelsim.domain;

import java.time.LocalDateTime;

public class DaylightTimes {
    private final LocalDateTime sunrise;
    private final LocalDateTime sunset;

    public DaylightTimes(LocalDateTime sunrise, LocalDateTime sunset) {
        this.sunrise = sunrise;
        this.sunset = sunset;
    }

    public LocalDateTime getSunrise() { return sunrise; }
    public LocalDateTime getSunset() { return sunset; }
}