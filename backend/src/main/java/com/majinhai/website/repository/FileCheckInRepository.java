package com.majinhai.website.repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.majinhai.website.config.StorageProperties;
import com.majinhai.website.exception.BusinessException;
import com.majinhai.website.model.entity.CheckIn;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Repository;

@Repository
public class FileCheckInRepository implements CheckInRepository {

    private static final TypeReference<List<CheckIn>> CHECK_IN_LIST_TYPE = new TypeReference<>() {
    };

    private final ObjectMapper objectMapper;
    private final Path storageFile;
    private final AtomicLong idGenerator = new AtomicLong(1);
    private final List<CheckIn> storage = new ArrayList<>();

    public FileCheckInRepository(StorageProperties storageProperties, ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.storageFile = Path.of(storageProperties.getBaseDir(), storageProperties.getCheckInsFile());
        loadExistingData();
    }

    @Override
    public synchronized List<CheckIn> findAll() {
        return storage.stream()
                .sorted(Comparator.comparing(CheckIn::getDate).reversed())
                .toList();
    }

    @Override
    public synchronized CheckIn save(CheckIn checkIn) {
        checkIn.setId(idGenerator.getAndIncrement());
        storage.add(checkIn);
        persist();
        return checkIn;
    }

    private void loadExistingData() {
        try {
            Files.createDirectories(storageFile.getParent());

            if (Files.notExists(storageFile)) {
                persist();
                return;
            }

            if (Files.size(storageFile) == 0L) {
                persist();
                return;
            }

            List<CheckIn> savedCheckIns = objectMapper.readValue(storageFile.toFile(), CHECK_IN_LIST_TYPE);
            storage.clear();
            storage.addAll(savedCheckIns);

            long maxId = storage.stream()
                    .map(CheckIn::getId)
                    .filter(id -> id != null)
                    .mapToLong(Long::longValue)
                    .max()
                    .orElse(0L);
            idGenerator.set(maxId + 1);
        } catch (IOException exception) {
            throw new BusinessException("CHECKIN_STORAGE_INIT_FAILED", "初始化打卡存储失败");
        }
    }

    private void persist() {
        try {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(storageFile.toFile(), storage);
        } catch (IOException exception) {
            throw new BusinessException("CHECKIN_STORAGE_WRITE_FAILED", "写入打卡存储失败");
        }
    }
}
