package com.fabric.service;

import com.fabric.database.dto.econt.EcontOfficesDTO;

import java.util.List;

public interface EcontOfficesService {
    List<EcontOfficesDTO> getOffices(String name);
}
