package com.app.MailService.Service;

import java.util.List;

public interface DBRecordService<T> {

    T save(T dBRecord);

    List<T> saveAll(List<T> dBRecords);

    List<T> findAll();

    T delete(Long id);
}
