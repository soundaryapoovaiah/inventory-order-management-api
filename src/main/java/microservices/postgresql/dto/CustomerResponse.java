package microservices.postgresql.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class CustomerResponse {

    private Long customerId;
    private String name;
    private String email;
    private String phone;
    private String address;
    private LocalDateTime createdAt;
}