package com.app.MailService.Repository;

import com.app.MailService.Entity.QueueMessage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QueueMessageRepository extends JpaRepository<QueueMessage, Long> {

}
