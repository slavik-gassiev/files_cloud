package com.slava.config;

import com.slava.dto.UserDto;
import com.slava.entity.User;
import org.modelmapper.ModelMapper;
import org.modelmapper.PropertyMap;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashSet;

@Configuration
public class ModelMapperConfig {

    @Bean
    public ModelMapper modelMapper() {
        ModelMapper modelMapper = new ModelMapper();
        modelMapper.addMappings(new PropertyMap<UserDto, User>() {
            @Override
            protected void configure() {
                map().setRoles(new HashSet<>());
            }
        });
        return modelMapper;
    }
}

