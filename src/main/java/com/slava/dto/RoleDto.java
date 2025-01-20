package com.slava.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
public class RoleDto implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private String name;
}
