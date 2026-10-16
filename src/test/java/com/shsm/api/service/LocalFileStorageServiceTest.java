package com.shsm.api.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LocalFileStorageServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void storesProfilePicturesAndDocumentsLocally() throws IOException {
        Path profiles = tempDir.resolve("profile_pictures");
        Path docs = tempDir.resolve("docs");
        LocalFileStorageService storage = new LocalFileStorageService(profiles.toString(), docs.toString());
        byte[] profileBytes = {1, 2, 3};
        byte[] documentBytes = {4, 5, 6};

        assertEquals("/profile_pictures/CURP.jpg", storage.storeProfilePicture(
                new ByteArrayInputStream(profileBytes), "CURP.jpg"));
        assertEquals("/docs/42/INE.pdf", storage.storeDocument(
                new ByteArrayInputStream(documentBytes), "42/INE.pdf"));
        assertArrayEquals(profileBytes, Files.readAllBytes(profiles.resolve("CURP.jpg")));
        assertArrayEquals(documentBytes, Files.readAllBytes(docs.resolve("42/INE.pdf")));

        storage.deleteProfilePicture("CURP.jpg");
        storage.deleteDocument("/docs/42/INE.pdf");

        assertFalse(Files.exists(profiles.resolve("CURP.jpg")));
        assertFalse(Files.exists(docs.resolve("42/INE.pdf")));
    }

    @Test
    void replacesExistingFilesAndRejectsPathsOutsideStorage() throws IOException {
        Path profiles = tempDir.resolve("profile_pictures");
        LocalFileStorageService storage = new LocalFileStorageService(profiles.toString(), tempDir.resolve("docs").toString());

        storage.storeProfilePicture(new ByteArrayInputStream(new byte[]{1}), "CURP.jpg");
        storage.storeProfilePicture(new ByteArrayInputStream(new byte[]{9}), "CURP.jpg");

        assertArrayEquals(new byte[]{9}, Files.readAllBytes(profiles.resolve("CURP.jpg")));
        assertThrows(IllegalArgumentException.class,
                () -> storage.storeDocument(new ByteArrayInputStream(new byte[]{1}), "../../outside.pdf"));
        assertTrue(Files.exists(profiles.resolve("CURP.jpg")));
    }
}