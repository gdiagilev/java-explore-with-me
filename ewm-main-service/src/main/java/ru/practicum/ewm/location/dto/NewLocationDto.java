package ru.practicum.ewm.location.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
public class NewLocationDto {

    @NotBlank
    @Size(min = 1, max = 255)
    private String name;

    @NotNull
    private Float lat;

    @NotNull
    private Float lon;

    @NotNull
    @Positive
    private Float radius;
}