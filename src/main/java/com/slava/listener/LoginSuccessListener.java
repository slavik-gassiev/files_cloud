package com.slava.listener;

import com.slava.service.FileService;
import org.springframework.context.ApplicationListener;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;

@Component
public class LoginSuccessListener implements ApplicationListener<AuthenticationSuccessEvent> {

    private final FileService fileService;

    public LoginSuccessListener(FileService fileService) {
        this.fileService = fileService;
    }

    @Override
    public void onApplicationEvent(AuthenticationSuccessEvent event) {
        String username = event.getAuthentication().getName();
        fileService.ensureBucketExists(username);
    }
}
