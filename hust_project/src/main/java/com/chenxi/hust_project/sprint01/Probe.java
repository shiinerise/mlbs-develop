package com.chenxi.hust_project.sprint01;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;

@Data
@AllArgsConstructor
public class Probe implements Serializable {

    private String key;
    private String[] time_stamp_array;
}
