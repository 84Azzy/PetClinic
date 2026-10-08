package com.zzy.petclinic.storage;

public record ValidatedImage(byte[] bytes, String contentType, String extension) {}
