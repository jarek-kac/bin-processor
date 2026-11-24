package fraud.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record BinRangeDetail(
        String binNum,
        String customerName,
        String countryCode,
        String fundingSource,
        String brandProductCode,
        String brandProductName
) {}
