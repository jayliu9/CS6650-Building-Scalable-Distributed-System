package model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@AllArgsConstructor
@Builder
public class RequestRecord {

  private Long startTime;
  private RequestType requestType;
  private Long latency;
  private Integer responseCode;
}
