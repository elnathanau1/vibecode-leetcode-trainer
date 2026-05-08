package com.leetcodetrainer.dto;

import lombok.Data;

import java.util.List;

@Data
public class ImportResultDTO {
    private int imported;
    private int skipped;
    private int duplicate;
    private List<String> notFound;
    private List<String> errors;
}
