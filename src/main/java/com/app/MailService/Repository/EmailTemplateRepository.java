package com.app.MailService.Repository;

import com.app.MailService.Entity.EmailTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmailTemplateRepository extends JpaRepository<EmailTemplate, Integer> {

    EmailTemplate findByName(String templateName);
}
