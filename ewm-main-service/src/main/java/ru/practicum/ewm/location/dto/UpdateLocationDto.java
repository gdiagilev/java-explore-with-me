package ru.practicum.ewm.location.dto;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateLocationDto {

    @Size(min = 1, max = 255)
    private String name;

    private Float lat;

    private Float lon;

    @Positive
    private Float radius;
}