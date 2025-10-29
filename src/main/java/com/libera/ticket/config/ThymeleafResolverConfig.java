package com.libera.ticket.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.templateresolver.ITemplateResolver;

import java.util.Set;

@Configuration
public class ThymeleafResolverConfig {
    // 메일 템플릿
    @Bean
    public ITemplateResolver mailResolver() {
        ClassLoaderTemplateResolver r = new ClassLoaderTemplateResolver();
        r.setPrefix("templates/");   // classpath:/templates/mail/
        r.setSuffix(".html");
        r.setTemplateMode(TemplateMode.HTML);
        r.setCharacterEncoding("UTF-8");
        r.setCheckExistence(true);
        r.setResolvablePatterns(Set.of("mail/*"));
        r.setOrder(10); // 기본 리졸버보다 먼저 시도
        return r;
    }

    // 문자 템플릿
    @Bean
    public ITemplateResolver smsResolver() {
        ClassLoaderTemplateResolver r = new ClassLoaderTemplateResolver();
        r.setPrefix("templates/");    // classpath:/templates/sms/
        r.setSuffix(".txt");
        r.setTemplateMode(TemplateMode.TEXT);
        r.setCharacterEncoding("UTF-8");
        r.setCheckExistence(true);
        r.setResolvablePatterns(Set.of("sms/*"));
        r.setOrder(11);
        return r;
    }
}
