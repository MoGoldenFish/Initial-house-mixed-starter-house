package com.hexagram2021.initial_house.server.util;

public interface ModLoaderHelper {
    String getModLoaderName();
    String getGameDirectory();
    boolean isModLoaded(String modId);
    boolean isDevelopmentEnvironment();
    boolean isClientSide();
}
