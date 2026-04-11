package com.powerloom.dto;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class ReconDisplayRow {

    private String type;
    private String gstin;
    private String name;
    private String invoice;
    private String date;

    private double taxable;
    private double igst;
    private double cgst;
    private double sgst;
    private double cess;

    // ===== GETTERS & SETTERS =====

}